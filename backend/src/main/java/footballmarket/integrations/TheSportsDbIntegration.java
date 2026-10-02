package footballmarket.integrations;

import footballmarket.integrations.exceptions.InvalidTheSportsDbResponseException;
import footballmarket.integrations.exceptions.TheSportsDbRateLimitException;
import footballmarket.integrations.exceptions.TheSportsDbUnavailableException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Adaptador v1 para búsqueda y consulta por ID; nunca publica la ruta que contiene la clave. */
@Component
public class TheSportsDbIntegration {
  private static final Logger LOG = LoggerFactory.getLogger(TheSportsDbIntegration.class);

  /** Datos adaptados del proveedor, separados de los DTO HTTP de FootballMarket y del Model. */
  public record PlayerData(
      String externalId,
      String name,
      String alternateName,
      String team,
      String sport,
      LocalDate dateOfBirth,
      String nationality,
      String cutout,
      String thumbnail) {}

  private final RestClient client;
  private final TheSportsDbRequestPacer pacer;
  private final Clock clock;

  @Autowired
  public TheSportsDbIntegration(
      @Qualifier("theSportsDbRestClient") RestClient theSportsDbRestClient,
      TheSportsDbRequestPacer pacer) {
    this(theSportsDbRestClient, pacer, Clock.systemUTC());
  }

  public TheSportsDbIntegration(RestClient client, TheSportsDbRequestPacer pacer, Clock clock) {
    this.client = client;
    this.pacer = pacer;
    this.clock = clock;
  }

  /** Una sola búsqueda reutiliza identidad e imágenes; no hace un lookup adicional. */
  public List<PlayerData> search(String name) {
    return this.request("searchplayers.php", "p", name, "player");
  }

  /** Consulta solo la referencia persistida, sin volver a resolver por nombre. */
  public List<PlayerData> lookup(String externalId) {
    return this.request("lookupplayer.php", "id", externalId, "players");
  }

  private List<PlayerData> request(String endpoint, String parameter, String value, String root) {
    this.pacer.beforeRequest();
    try {
      Map<?, ?> body =
          this.client
              .get()
              .uri(builder -> builder.path("/" + endpoint).queryParam(parameter, value).build())
              .retrieve()
              .onStatus(
                  status -> status.value() == 429,
                  (request, response) -> {
                    Instant retryAt = this.retryAt(response.getHeaders().getFirst("Retry-After"));
                    this.pacer.delayUntil(retryAt);
                    throw new TheSportsDbRateLimitException(retryAt);
                  })
              .onStatus(
                  HttpStatusCode::isError,
                  (request, response) -> {
                    if (response.getStatusCode().is5xxServerError()) {
                      throw new TheSportsDbUnavailableException();
                    }
                    throw new InvalidTheSportsDbResponseException();
                  })
              .body(Map.class);
      if (body == null || !body.containsKey(root)) {
        throw new InvalidTheSportsDbResponseException();
      }
      Object payload = body.get(root);
      if (payload == null) {
        return List.of();
      }
      if (!(payload instanceof List<?> players)) {
        throw new InvalidTheSportsDbResponseException();
      }
      List<PlayerData> result = new ArrayList<>();
      for (Object player : players) {
        if (!(player instanceof Map<?, ?> fields)) {
          throw new InvalidTheSportsDbResponseException();
        }
        result.add(this.adapt(fields));
      }
      return List.copyOf(result);
    } catch (RestClientException exception) {
      // La excepción del cliente puede incluir la URL con clave; nunca propagarla ni registrarla.
      throw new TheSportsDbUnavailableException();
    }
  }

  private PlayerData adapt(Map<?, ?> fields) {
    String id = this.text(fields, "idPlayer");
    if (id == null || id.isBlank()) {
      throw new InvalidTheSportsDbResponseException();
    }
    String born = this.text(fields, "dateBorn");
    LocalDate date = null;
    if (born != null && !born.isBlank()) {
      try {
        date = LocalDate.parse(born);
      } catch (DateTimeParseException exception) {
        throw new InvalidTheSportsDbResponseException();
      }
    }
    return new PlayerData(
        id,
        this.text(fields, "strPlayer"),
        this.text(fields, "strPlayerAlternate"),
        this.text(fields, "strTeam"),
        this.text(fields, "strSport"),
        date,
        this.text(fields, "strNationality"),
        this.validImageUrl(this.text(fields, "strCutout"), "strCutout"),
        this.validImageUrl(this.text(fields, "strThumb"), "strThumb"));
  }

  private String text(Map<?, ?> fields, String key) {
    Object value = fields.get(key);
    if (value == null) {
      return null;
    }
    if (!(value instanceof String text)) {
      throw new InvalidTheSportsDbResponseException();
    }
    return text;
  }

  private String validImageUrl(String value, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      URI uri = URI.create(value);
      String host = uri.getHost();
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || host == null
          || uri.getUserInfo() != null
          || uri.getFragment() != null) {
        LOG.warn("Imagen descartada: esquema o autoridad inválidos; campo={}", field);
        return null;
      }
      String domain = host.toLowerCase(Locale.ROOT);
      if (!domain.equals("thesportsdb.com") && !domain.endsWith(".thesportsdb.com")) {
        LOG.warn("Imagen descartada: host no autorizado; campo={}", field);
        return null;
      }
      return value;
    } catch (IllegalArgumentException exception) {
      LOG.warn("Imagen descartada: URL malformada; campo={}", field);
      return null;
    }
  }

  private Instant retryAt(String header) {
    Instant now = this.clock.instant();
    if (header != null) {
      try {
        long seconds = Long.parseLong(header.strip());
        if (seconds >= 0) {
          return now.plusSeconds(seconds);
        }
      } catch (NumberFormatException | java.time.DateTimeException exception) {
        try {
          Instant date =
              ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
          if (date.isAfter(now)) {
            return date;
          }
        } catch (DateTimeParseException ignored) {
          // Ausencia de una fecha HTTP válida: plazo conservador.
        }
      }
    }
    return now.plusSeconds(60);
  }
}
