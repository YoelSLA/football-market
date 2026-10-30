package footballmarket.integrations.whoscored;

import footballmarket.features.statistics.infraestructure.config.PlayerStatisticsProperties;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsRateLimitException;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsStructureChangedException;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsUnavailableException;
import footballmarket.integrations.whoscored.records.WhoScoredJsonQuery;
import footballmarket.integrations.whoscored.records.WhoScoredStatisticsResponse;
import footballmarket.integrations.whoscored.records.WhoScoredStatisticsTable;
import footballmarket.integrations.whoscored.records.WhoScoredStatisticsTable.Cell;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Recorrido JSON de un jugador por las quince combinaciones conocidas.
 *
 * <p>Reutiliza el cliente seguro y el pacer existentes: no hay cliente ni pacer nuevos. El ritmo se
 * aplica por worker entre inicios de consulta, {@code Retry-After} se respeta para las consultas
 * posteriores y solo un timeout o fallo de transporte admite un reintento. El MIME declarado se
 * ignora porque el proveedor publica JSON con {@code text/html}; decide el cuerpo.
 */
@Component
public class WhoScoredStatisticsIntegration {
  /**
   * Intentos por consulta antes de dar por perdido al jugador.
   *
   * <p>Tres intentos cubren la degradación observada del proveedor sin insistir cuando la
   * indisponibilidad es global: el intervalo del worker actúa como espera entre intentos y el
   * presupuesto de la consulta sigue acotando el tiempo total.
   */
  private static final int MAX_ATTEMPTS = 3;

  private static final Set<String> ROW_METADATA =
      Set.of(
          "playerId",
          "playerName",
          "name",
          "teamId",
          "teamName",
          "tournamentId",
          "tournamentName",
          "competitionName",
          "seasonId",
          "seasonName",
          "position",
          "isCaptain");

  /**
   * Métricas del catálogo que el proveedor publica en la fila pero no declara en {@code
   * statColumns}. Se leen de forma explícita porque el resto de claves de la fila son identidad o
   * contexto, no métricas, y no pueden entrar en el catálogo cerrado.
   */
  private static final Set<String> ROW_ONLY_METRICS = Set.of("rating");

  private final RestClient client;
  private final WhoScoredRequestPacer pacer;
  private final WhoScoredProperties providerProperties;
  private final PlayerStatisticsProperties properties;
  private final ObjectMapper mapper;
  private final Clock clock;

  public WhoScoredStatisticsIntegration(
      @Qualifier("whoScoredRestClient") RestClient client,
      WhoScoredRequestPacer pacer,
      WhoScoredProperties providerProperties,
      PlayerStatisticsProperties properties,
      ObjectMapper mapper) {
    this.client = client;
    this.pacer = pacer;
    this.providerProperties = providerProperties;
    this.properties = properties;
    this.mapper = mapper;
    this.clock = Clock.systemUTC();
  }

  /** Ritmo por worker del canal JSON, creado una vez por hilo del lote. */
  public WhoScoredRequestPacer.StatisticsWorker newWorker() {
    return this.pacer.statisticsWorker(this.properties.requestIntervalMs());
  }

  /** Las quince consultas del jugador, en orden estable y con el ritmo del worker. */
  public WhoScoredStatisticsResponse fetch(
      String playerId, WhoScoredRequestPacer.StatisticsWorker worker) {
    if (worker == null) {
      throw new WhoScoredStatisticsException("PROVIDER_DATA_INCONSISTENT", null);
    }
    List<WhoScoredStatisticsTable> tables = new ArrayList<>();
    for (WhoScoredJsonQuery.Combination combination : WhoScoredJsonQuery.COMBINATIONS) {
      tables.addAll(
          this.query(WhoScoredJsonQuery.of(playerId, combination), worker, combination.section()));
    }
    return new WhoScoredStatisticsResponse(playerId, tables, this.clock.instant());
  }

  private List<WhoScoredStatisticsTable> query(
      WhoScoredJsonQuery query, WhoScoredRequestPacer.StatisticsWorker worker, String section) {
    Instant start = this.clock.instant();
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        return this.pacer.executeStatistics(worker, null, () -> this.request(query, section));
      } catch (WhoScoredStatisticsUnavailableException unavailable) {
        if (!retryable(unavailable) || attempt == MAX_ATTEMPTS || this.budgetExhausted(start)) {
          throw unavailable;
        }
      } catch (RestClientException transport) {
        if (attempt == MAX_ATTEMPTS || this.budgetExhausted(start)) {
          throw new WhoScoredStatisticsUnavailableException(
              isTimeout(transport) ? "PROVIDER_TIMEOUT" : "PROVIDER_TRANSPORT_ERROR", null);
        }
      } catch (WhoScoredStatisticsException known) {
        throw known;
      }
    }
    throw new WhoScoredStatisticsUnavailableException("PROVIDER_TRANSPORT_ERROR", null);
  }

  /**
   * Un 5xx del proveedor es transitorio y no invalida la consulta.
   *
   * <p>El proveedor se degrada en ventanas de minutos durante las que responde 502 con frecuencia y
   * después se recupera sin cambios. Sin reintento, una sola de esas respuestas pierde el jugador,
   * porque cada jugador necesita quince consultas. El límite de peticiones y el bloqueo no llegan
   * aquí: se propagan como excepción conocida y la fachada los trata de forma distinta.
   */
  private static boolean retryable(WhoScoredStatisticsUnavailableException failure) {
    Integer status = failure.getProviderStatus();
    return status == null || (status >= 500 && status < 600);
  }

  /** El presupuesto por consulta impide insistir más allá del timeout configurado del canal. */
  private boolean budgetExhausted(Instant start) {
    return java.time.Duration.between(start, this.clock.instant()).toMillis()
        >= this.properties.queryTimeoutMs();
  }

  private List<WhoScoredStatisticsTable> request(WhoScoredJsonQuery query, String section) {
    try {
      return this.client
          .get()
          .uri(
              builder ->
                  builder
                      .path("/statisticsfeed/1/getplayerstatistics")
                      .queryParam("category", query.category())
                      .queryParam("subcategory", query.subcategory())
                      .queryParam("statsAccumulationType", 0)
                      .queryParam("isCurrent", true)
                      .queryParam("playerId", query.playerId())
                      .queryParam("includeZeroValues", true)
                      .queryParam("field", "Overall")
                      .queryParam("sortBy", "Rating")
                      .queryParam("page", 1)
                      .build())
          .exchange(
              (request, response) -> {
                int status = response.getStatusCode().value();
                if (status == 429) {
                  this.pacer.defer(response.getHeaders().getFirst("Retry-After"));
                  throw new WhoScoredStatisticsRateLimitException(status);
                }
                if (status == 403 || status == 404) {
                  throw new WhoScoredStatisticsException(
                      status == 403 ? "PROVIDER_BLOCKED" : "PLAYER_NOT_FOUND", status);
                }
                if (status < 200 || status >= 300) {
                  throw new WhoScoredStatisticsUnavailableException(
                      "PROVIDER_TRANSPORT_ERROR", status);
                }
                byte[] bytes =
                    response.getBody().readNBytes(this.providerProperties.maxResponseBytes());
                return this.parse(bytes, query, section);
              });
    } catch (WhoScoredStatisticsException known) {
      throw known;
    } catch (RestClientException transport) {
      throw new WhoScoredStatisticsUnavailableException(
          isTimeout(transport) ? "PROVIDER_TIMEOUT" : "PROVIDER_TRANSPORT_ERROR", null);
    } catch (RuntimeException failure) {
      if (Thread.currentThread().isInterrupted()) {
        throw new WhoScoredStatisticsException("EXTRACTION_INTERRUPTED", null);
      }
      throw failure;
    }
  }

  private List<WhoScoredStatisticsTable> parse(
      byte[] body, WhoScoredJsonQuery query, String section) {
    JsonNode root;
    try {
      root =
          this.mapper
              .reader()
              .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
              .readTree(body);
    } catch (RuntimeException malformed) {
      throw new WhoScoredStatisticsStructureChangedException();
    }
    WhoScoredJsonResponseValidator.validate(root);
    Set<String> declared = new LinkedHashSet<>();
    for (JsonNode column : root.path("statColumns")) {
      declared.add(WhoScoredJsonResponseValidator.declaredColumn(column));
    }
    Instant observedAt = this.clock.instant();
    List<WhoScoredStatisticsTable> tables = new ArrayList<>();
    for (JsonNode row : root.path("playerTableStats")) {
      String rowPlayerId = text(row, "playerId");
      if (rowPlayerId != null && !rowPlayerId.equals(query.playerId())) {
        throw new WhoScoredStatisticsStructureChangedException();
      }
      String competitionId = text(row, "tournamentId");
      String seasonKey = text(row, "seasonId");
      if (competitionId == null || seasonKey == null) {
        throw new WhoScoredStatisticsStructureChangedException();
      }
      List<Cell> cells = new ArrayList<>();
      for (String field : declared) {
        if (!ROW_METADATA.contains(field)) {
          cells.add(new Cell(field, token(row.path(field))));
        }
      }
      for (String field : ROW_ONLY_METRICS) {
        if (row.has(field) && !declared.contains(field)) {
          cells.add(new Cell(field, token(row.path(field))));
        }
      }
      tables.add(
          new WhoScoredStatisticsTable(
              section,
              competitionId,
              text(row, "teamId"),
              seasonKey,
              text(row, "seasonName"),
              null,
              cells,
              observedAt));
    }
    if (tables.isEmpty()) {
      // Una combinación sin filas es una ausencia de datos, no una forma inválida: el recorrido
      // continúa y el ensamblador marca esas métricas como AUSENTES.
      return List.of();
    }
    return tables;
  }

  /** Conserva el token decimal original; un objeto con {@code value} se developa sin perderlo. */
  private static String token(JsonNode value) {
    if (value == null || value.isNull()) {
      return "-";
    }
    if (value.isObject()) {
      JsonNode nested = value.path("value");
      return nested.isMissingNode() ? "-" : token(nested);
    }
    if (value.isTextual()) {
      return value.stringValue();
    }
    return value.toString();
  }

  private static String text(JsonNode row, String field) {
    JsonNode value = row.path(field);
    if (value.isTextual()) {
      String raw = value.stringValue().strip();
      return raw.isEmpty() ? null : raw;
    }
    if (value.isIntegralNumber() && value.canConvertToLong()) {
      return Long.toString(value.longValue());
    }
    return null;
  }

  private static boolean isTimeout(Throwable failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof java.net.http.HttpTimeoutException
          || cause instanceof java.net.SocketTimeoutException) {
        return true;
      }
      if (cause.getCause() == cause) {
        return false;
      }
    }
    return false;
  }
}
