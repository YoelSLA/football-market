package footballmarket.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import footballmarket.config.PlayerStatisticsProperties;
import footballmarket.config.WhoScoredProperties;
import footballmarket.integrations.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.records.WhoScoredJsonQuery;
import footballmarket.integrations.records.WhoScoredStatisticsResponse;
import footballmarket.support.WhoScoredStatisticsProviderStub;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Recorrido JSON contra el proveedor controlado de loopback.
 *
 * <p>Comprueba las quince combinaciones conocidas, la aceptación de JSON con MIME HTML y el rechazo
 * de cualquier forma o combinación especulativa. Nunca accede a WhoScored real.
 */
@org.junit.jupiter.api.Tag("lightweight")
class WhoScoredStatisticsIntegrationTest {
  private static final String PLAYER_ID = "1001";

  private WhoScoredStatisticsProviderStub provider;
  private List<Long> waits;
  private AtomicLong now;
  private WhoScoredRequestPacer pacer;
  private WhoScoredStatisticsIntegration integration;

  @BeforeEach
  void setUp() throws Exception {
    this.provider = new WhoScoredStatisticsProviderStub();
    this.waits = new ArrayList<>();
    this.now = new AtomicLong(0);
    this.pacer =
        new WhoScoredRequestPacer(
            1,
            this.now::get,
            milliseconds -> {
              this.waits.add(milliseconds);
              this.now.addAndGet(milliseconds);
            },
            Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC));
    this.integration =
        new WhoScoredStatisticsIntegration(
            RestClient.builder().baseUrl(this.provider.baseUrl()).build(),
            this.pacer,
            new WhoScoredProperties(
                "https://www.whoscored.test",
                "FootballMarket/1.0",
                25,
                2500,
                5000,
                5000,
                5,
                5_000_000,
                List.of(
                    new WhoScoredProperties.Competition(1L, 2),
                    new WhoScoredProperties.Competition(2L, 3),
                    new WhoScoredProperties.Competition(3L, 4),
                    new WhoScoredProperties.Competition(4L, 5),
                    new WhoScoredProperties.Competition(5L, 22))),
            new PlayerStatisticsProperties(3, 3, 1, 30_000, 30_000, false, true),
            JsonMapper.builder().build());
  }

  @AfterEach
  void tearDown() {
    this.provider.close();
  }

  private void enqueueValidRows() {
    enqueueValidRows(1);
  }

  /**
   * El recorrido completo son quince consultas: sin respuestas en cola el doble responde 500.
   *
   * <p>La carga reproduce la forma realmente publicada por el proveedor: {@code statColumns} es un
   * array de cadenas, {@code seasonId} es un entero y {@code rating} aparece en la fila sin
   * declararse en {@code statColumns}.
   */
  private void enqueueValidRows(int count) {
    for (int index = 0; index < count; index++) {
      this.provider.enqueue(200, "text/html", OBSERVED_PLAYER_STATISTICS, null);
    }
  }

  private static final String OBSERVED_PLAYER_STATISTICS =
      """
      {"playerTableStats":[{"height":176,"weight":64,"age":29,"isManOfTheMatch":true,
      "isActive":true,"playedPositions":"DC","playedPositionsShort":"DC",
      "teamRegionName":"England","regionCode":"EUR","tournamentShortName":"Premier League",
      "apps":"6","subOn":"0","manOfTheMatch":"0","goal":"1","assistTotal":"0",
      "shotsPerGame":"1.5","aerialWonPerGame":"2.1","name":"Test Player","firstName":"Test",
      "lastName":"Player","playerId":1001,"positionText":"Defender","teamId":3,
      "teamName":"Test FC","seasonId":2026,"seasonName":"2026/2027","isOpta":true,
      "tournamentId":2,"tournamentRegionId":100,"tournamentRegionCode":"EUR",
      "tournamentRegionName":"Europe","tournamentName":"Premier League",
      "rating":6.9033333333333333,"minsPlayed":"420","yellowCard":"1","redCard":"0",
      "passSuccess":"85.5","ranking":12}],
      "paging":{"page":1,"totalPages":1,"totalResults":1},
      "statColumns":["apps","subOn","goal","assistTotal","shotsPerGame","passSuccess",
      "aerialWonPerGame","manOfTheMatch","yellowCard","redCard","minsPlayed"]}
      """;

  private static String parameter(URI uri, String name) {
    for (String pair : uri.getQuery().split("&")) {
      String[] parts = pair.split("=", 2);
      if (parts[0].equals(name)) {
        return parts.length > 1
            ? java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8)
            : "";
      }
    }
    return null;
  }

  @Nested
  @DisplayName("Combinaciones conocidas del recorrido JSON")
  class Combinations {
    @Test
    @DisplayName("El recorrido consulta exactamente las quince combinaciones, sin duplicados")
    void recorreQuinceCombinaciones() {
      // Arrange
      for (int index = 0; index < WhoScoredJsonQuery.COMBINATIONS.size(); index++) {
        enqueueValidRows(1);
      }

      // Act
      WhoScoredStatisticsResponse response =
          integration.fetch(PLAYER_ID, pacer.statisticsWorker(1));

      // Assert
      assertThat(WhoScoredJsonQuery.COMBINATIONS).hasSize(15);
      assertThat(response.tables()).hasSize(15);
      assertThat(provider.requests()).hasSize(15);
      assertThat(provider.requests())
          .extracting(uri -> parameter(uri, "category") + "/" + parameter(uri, "subcategory"))
          .containsExactlyElementsOf(
              WhoScoredJsonQuery.COMBINATIONS.stream()
                  .map(combination -> combination.category() + "/" + combination.subcategory())
                  .toList());
      assertThat(provider.requests())
          .allSatisfy(
              uri -> {
                assertThat(uri.getPath()).isEqualTo("/statisticsfeed/1/getplayerstatistics");
                assertThat(parameter(uri, "playerId")).isEqualTo(PLAYER_ID);
                assertThat(parameter(uri, "statsAccumulationType")).isEqualTo("0");
                assertThat(parameter(uri, "isCurrent")).isEqualTo("true");
                assertThat(parameter(uri, "includeZeroValues")).isEqualTo("true");
                assertThat(parameter(uri, "field")).isEqualTo("Overall");
                assertThat(parameter(uri, "sortBy")).isEqualTo("Rating");
                assertThat(parameter(uri, "page")).isEqualTo("1");
              });
    }

    @Test
    @DisplayName("Un JSON publicado con MIME text/html se interpreta con su decimal exacto")
    void aceptaMimeHtml() {
      // Arrange
      enqueueValidRows(WhoScoredJsonQuery.COMBINATIONS.size());

      // Act
      WhoScoredStatisticsResponse response =
          integration.fetch(PLAYER_ID, pacer.statisticsWorker(1));

      // Assert
      assertThat(response.tables())
          .hasSize(WhoScoredJsonQuery.COMBINATIONS.size())
          .filteredOn(table -> table.section().equals("summary/all"))
          .singleElement()
          .satisfies(
              table -> {
                assertThat(table.competitionId()).isEqualTo("2");
                assertThat(table.teamId()).isEqualTo("3");
                assertThat(table.seasonKey()).isEqualTo("2026");
                assertThat(table.cells())
                    .filteredOn(cell -> cell.providerField().equals("rating"))
                    .singleElement()
                    .satisfies(
                        cell -> {
                          assertThat(cell.text()).isEqualTo("6.9033333333333333");
                          assertThat(new BigDecimal(cell.text()))
                              .isEqualTo(new BigDecimal("6.9033333333333333"));
                        });
              });
    }

    @Test
    @DisplayName("Una combinación especulativa se rechaza antes de contactar al proveedor")
    void rechazaCombinacionEspeculativa() {
      // Act / Assert
      assertThatThrownBy(() -> new WhoScoredJsonQuery(PLAYER_ID, "shots", "heatmaps"))
          .isInstanceOf(WhoScoredStatisticsException.class);
      assertThatThrownBy(() -> new WhoScoredJsonQuery(PLAYER_ID, "xg", "detail"))
          .isInstanceOf(WhoScoredStatisticsException.class);
      assertThatThrownBy(() -> new WhoScoredJsonQuery("0", "summary", "all"))
          .isInstanceOf(WhoScoredStatisticsException.class);
      assertThat(provider.requests()).isEmpty();
    }

    @Test
    @DisplayName("Las combinaciones retiradas por redundancia ya no son válidas")
    void rechazaCombinacionesRetiradas() {
      // Act / Assert
      assertThat(WhoScoredJsonQuery.COMBINATIONS)
          .extracting(combination -> combination.category() + "/" + combination.subcategory())
          .doesNotContain(
              "cards/type",
              "clearances/success",
              "interception/success",
              "offsides/type",
              "possession-loss/type");
      assertThatThrownBy(() -> new WhoScoredJsonQuery(PLAYER_ID, "cards", "type"))
          .isInstanceOf(WhoScoredStatisticsException.class);
      assertThatThrownBy(() -> new WhoScoredJsonQuery(PLAYER_ID, "clearances", "success"))
          .isInstanceOf(WhoScoredStatisticsException.class);
      assertThatThrownBy(() -> new WhoScoredJsonQuery(PLAYER_ID, "interception", "success"))
          .isInstanceOf(WhoScoredStatisticsException.class);
      assertThatThrownBy(() -> new WhoScoredJsonQuery(PLAYER_ID, "offsides", "type"))
          .isInstanceOf(WhoScoredStatisticsException.class);
      assertThatThrownBy(() -> new WhoScoredJsonQuery(PLAYER_ID, "possession-loss", "type"))
          .isInstanceOf(WhoScoredStatisticsException.class);
      assertThat(provider.requests()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Validación de la estructura publicada")
  class Structure {
    @Test
    @DisplayName(
        "La ausencia de playerTableStats o statColumns se clasifica como estructura cambiada")
    void clasificaEstructuraCambiada() {
      // Arrange
      provider.enqueue(200, "application/json", "{\"statColumns\":[{\"name\":\"rating\"}]}", null);

      // Act / Assert
      assertThatThrownBy(() -> integration.fetch(PLAYER_ID, pacer.statisticsWorker(1)))
          .isInstanceOfSatisfying(
              WhoScoredStatisticsException.class,
              failure -> assertThat(failure.getCode()).isEqualTo("PROVIDER_STRUCTURE_CHANGED"));
    }

    @Test
    @DisplayName("Un cuerpo que no es JSON tampoco se interpreta como ausencia de datos")
    void rechazaCuerpoNoJson() {
      // Arrange
      provider.enqueue(200, "text/html", "<html><body>blocked</body></html>", null);

      // Act / Assert
      assertThatThrownBy(() -> integration.fetch(PLAYER_ID, pacer.statisticsWorker(1)))
          .isInstanceOfSatisfying(
              WhoScoredStatisticsException.class,
              failure -> assertThat(failure.getCode()).isEqualTo("PROVIDER_STRUCTURE_CHANGED"));
    }

    @Test
    @DisplayName("statColumns publicado como array de cadenas es la forma válida observada")
    void aceptaStatColumnsDeCadenas() {
      // Arrange
      enqueueValidRows(WhoScoredJsonQuery.COMBINATIONS.size());

      // Act
      WhoScoredStatisticsResponse response =
          integration.fetch(PLAYER_ID, pacer.statisticsWorker(1));

      // Assert
      assertThat(response.tables()).isNotEmpty();
      assertThat(response.tables().getFirst().cells())
          .extracting(cell -> cell.providerField())
          .contains("apps", "goal", "assistTotal", "minsPlayed", "passSuccess");
    }

    @Test
    @DisplayName("La forma de objeto con name se sigue admitiendo como compatibilidad")
    void aceptaStatColumnsDeObjetos() {
      // Arrange
      for (int index = 0; index < WhoScoredJsonQuery.COMBINATIONS.size(); index++) {
        provider.enqueue(
            200,
            "text/html",
            """
            {"playerTableStats":[{"playerId":1001,"tournamentId":2,"teamId":3,"seasonId":"2026",
            "seasonName":"2026/27","apps":"6"}],
            "statColumns":[{"name":"apps"}]}
            """,
            null);
      }

      // Act
      WhoScoredStatisticsResponse response =
          integration.fetch(PLAYER_ID, pacer.statisticsWorker(1));

      // Assert
      assertThat(response.tables()).isNotEmpty();
    }

    @Test
    @DisplayName("Una métrica publicada solo en la fila se conserva aunque no se declare")
    void conservaMetricaSoloEnLaFila() {
      // Arrange
      enqueueValidRows(WhoScoredJsonQuery.COMBINATIONS.size());

      // Act
      WhoScoredStatisticsResponse response =
          integration.fetch(PLAYER_ID, pacer.statisticsWorker(1));

      // Assert
      assertThat(response.tables().getFirst().cells())
          .extracting(cell -> cell.providerField(), cell -> cell.text())
          .contains(tuple("rating", "6.9033333333333333"));
    }

    @Test
    @DisplayName("Las claves de identidad de la fila no se convierten en columnas de métrica")
    void descartaClavesDeIdentidad() {
      // Arrange
      enqueueValidRows(WhoScoredJsonQuery.COMBINATIONS.size());

      // Act
      WhoScoredStatisticsResponse response =
          integration.fetch(PLAYER_ID, pacer.statisticsWorker(1));

      // Assert
      assertThat(response.tables().getFirst().cells())
          .extracting(cell -> cell.providerField())
          .doesNotContain("height", "weight", "age", "regionCode", "ranking", "positionText");
    }
  }

  @Nested
  @DisplayName("Reintento limitado y ritmo del canal")
  class Resilience {
    @Test
    @DisplayName(
        "Solo un timeout de transporte admite reintento y agota el código PROVIDER_TIMEOUT")
    void reintentaSoloAnteTimeout() {
      // Arrange
      AtomicInteger attempts = new AtomicInteger();
      WhoScoredStatisticsIntegration failing =
          new WhoScoredStatisticsIntegration(
              RestClient.builder()
                  .baseUrl("http://127.0.0.1:1")
                  .requestFactory(
                      (uri, method) -> {
                        attempts.incrementAndGet();
                        throw new java.net.SocketTimeoutException("sin respuesta");
                      })
                  .build(),
              pacer,
              new WhoScoredProperties(
                  "https://www.whoscored.test",
                  "FootballMarket/1.0",
                  25,
                  2500,
                  5000,
                  5000,
                  5,
                  5_000_000,
                  List.of(
                      new WhoScoredProperties.Competition(1L, 2),
                      new WhoScoredProperties.Competition(2L, 3),
                      new WhoScoredProperties.Competition(3L, 4),
                      new WhoScoredProperties.Competition(4L, 5),
                      new WhoScoredProperties.Competition(5L, 22))),
              new PlayerStatisticsProperties(3, 3, 1, 30_000, 30_000, false, true),
              JsonMapper.builder().build());

      // Act / Assert
      assertThatThrownBy(() -> failing.fetch(PLAYER_ID, pacer.statisticsWorker(1)))
          .isInstanceOfSatisfying(
              WhoScoredStatisticsException.class,
              failure -> assertThat(failure.getCode()).isEqualTo("PROVIDER_TIMEOUT"));
      // Tres intentos: los dos primeros reintentan solo ante timeout y el tercero agota el código.
      assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Una respuesta 403 no se reintenta y clasifica PROVIDER_BLOCKED")
    void noReintentaBloqueo() {
      // Arrange
      provider.enqueue(403, "text/html", "{}", null);

      // Act / Assert
      assertThatThrownBy(() -> integration.fetch(PLAYER_ID, pacer.statisticsWorker(1)))
          .isInstanceOfSatisfying(
              WhoScoredStatisticsException.class,
              failure -> {
                assertThat(failure.getCode()).isEqualTo("PROVIDER_BLOCKED");
                assertThat(failure.getProviderStatus()).isEqualTo(403);
              });
      assertThat(provider.requests()).hasSize(1);
    }

    @Test
    @DisplayName("Retry-After se respeta y aplaza la consulta posterior sin repetir la fallida")
    void respetaRetryAfter() {
      // Arrange
      provider.enqueue(429, "application/json", "{}", "30");

      // Act
      assertThatThrownBy(() -> integration.fetch(PLAYER_ID, pacer.statisticsWorker(1)))
          .isInstanceOf(WhoScoredStatisticsException.class);

      // Assert
      assertThat(provider.requests()).hasSize(1);
      enqueueValidRows(WhoScoredJsonQuery.COMBINATIONS.size());
      WhoScoredStatisticsResponse response =
          integration.fetch(PLAYER_ID, pacer.statisticsWorker(1));
      // El único aplazamiento largo es el derivado de Retry-After; el resto son los 1 ms de ritmo
      // entre las quince combinaciones.
      assertThat(waits).contains(30_000L);
      assertThat(waits).filteredOn(wait -> wait >= 30_000L).containsExactly(30_000L);
      assertThat(response.tables()).hasSize(WhoScoredJsonQuery.COMBINATIONS.size());
    }
  }
}
