package footballmarket.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import footballmarket.config.WhoScoredClientConfig;
import footballmarket.config.WhoScoredProperties;
import footballmarket.integrations.exceptions.WhoScoredIntegrationException;
import footballmarket.integrations.records.WhoScoredCatalogPage;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class WhoScoredIntegrationTest {
  private HttpServer server;
  private WhoScoredIntegration integration;
  private final AtomicInteger status = new AtomicInteger(200);
  private final AtomicInteger requests = new AtomicInteger();
  private final AtomicReference<String> body = new AtomicReference<>();
  private final AtomicReference<String> query = new AtomicReference<>();
  private final AtomicReference<String> authorization = new AtomicReference<>();
  private final AtomicReference<String> userAgent = new AtomicReference<>();
  private final java.util.concurrent.atomic.AtomicBoolean delayResponse =
      new java.util.concurrent.atomic.AtomicBoolean();
  private final java.util.concurrent.CountDownLatch releaseResponse =
      new java.util.concurrent.CountDownLatch(1);

  @BeforeEach
  void startControlledProvider() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/statisticsfeed/1/getplayerstatistics",
        exchange -> {
          requests.incrementAndGet();
          query.set(exchange.getRequestURI().getRawQuery());
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
          if (delayResponse.get()) {
            try {
              releaseResponse.await();
            } catch (InterruptedException interrupted) {
              Thread.currentThread().interrupt();
            }
          }
          byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
          exchange.getResponseHeaders().set("Location", "/must-not-follow");
          exchange.sendResponseHeaders(status.get(), bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    List<WhoScoredProperties.Competition> competitions =
        List.of(
            new WhoScoredProperties.Competition(101L, 2),
            new WhoScoredProperties.Competition(102L, 3),
            new WhoScoredProperties.Competition(103L, 4),
            new WhoScoredProperties.Competition(104L, 5),
            new WhoScoredProperties.Competition(105L, 22));
    WhoScoredProperties properties =
        new WhoScoredProperties(
            "https://provider.example",
            "identity-test",
            3,
            1,
            1000,
            1000,
            10000,
            5242880,
            competitions);
    RestClient client =
        new WhoScoredClientConfig()
            .whoScoredRestClient(properties)
            .mutate()
            .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
            .build();
    AtomicLong time = new AtomicLong();
    WhoScoredRequestPacer pacer =
        new WhoScoredRequestPacer(1, time::get, time::addAndGet, Clock.systemUTC());
    integration = new WhoScoredIntegration(client, pacer, properties, JsonMapper.builder().build());
  }

  @AfterEach
  void stopControlledProvider() {
    releaseResponse.countDown();
    server.stop(0);
  }

  @Nested
  @DisplayName("Contrato de catálogo y fallos sanitizados")
  class Catalogue {
    @Test
    @DisplayName("El timeout de lectura del cliente productivo se traduce sin reintentos")
    void translatesReadTimeout() {
      // Arrange
      body.set("{}");
      delayResponse.set(true);
      try {
        // Act / Assert
        assertThatThrownBy(() -> integration.currentPage(4, 1))
            .isInstanceOf(WhoScoredIntegrationException.class);
        // Verify
        assertThat(requests.get()).isEqualTo(1);
      } finally {
        releaseResponse.countDown();
      }
    }

    @Test
    @DisplayName("El cuerpo por encima del límite se rechaza antes de parsear")
    void boundsResponseBody() {
      // Arrange
      body.set("x".repeat(5242881));
      // Act / Assert
      assertThatThrownBy(() -> integration.currentPage(4, 1))
          .isInstanceOf(WhoScoredIntegrationException.class)
          .satisfies(
              error ->
                  assertThat(((WhoScoredIntegrationException) error).getCode())
                      .isEqualTo("PROVIDER_LIMIT_EXCEEDED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "\"abc\"", "null", "\"\"", "\"9223372036854775808\""})
    @DisplayName("Un ID inválido descarta solo su fila y mantiene el paging original")
    void discardsInvalidIdentityOnly(String invalidId) throws Exception {
      // Arrange
      body.set(
          fixture("catalog-current.json")
              .replace("\"playerId\": 101", "\"playerId\": " + invalidId));
      // Act
      WhoScoredCatalogPage page = integration.currentPage(4, 1);
      // Assert
      assertThat(page.identities()).hasSize(2);
      assertThat(page.discardedRows())
          .singleElement()
          .satisfies(
              row -> {
                assertThat(row.rowPosition()).isEqualTo(1);
                assertThat(row.reasonCode()).isEqualTo("INVALID_EXTERNAL_PLAYER_ID");
              });
      assertThat(page.paging().lastRecordIndex()).isEqualTo(3);
    }

    @Test
    @DisplayName("La última página corta conserva el fin por metadatos sin otra solicitud")
    void acceptsFinalShortPage() throws Exception {
      // Arrange
      body.set(fixture("catalog-current-final.json"));
      // Act
      WhoScoredCatalogPage page = integration.currentPage(4, 2);
      // Assert
      assertThat(page.identities()).hasSize(1);
      assertThat(page.paging().currentPage()).isEqualTo(page.paging().totalPages());
      // Verify
      assertThat(requests.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"<html>blocked</html>", "{}", "{\"playerTableStats\":[]}", "[]"})
    @DisplayName("HTML y estructuras incompletas no se interpretan como catálogo vacío")
    void rejectsInvalidStructures(String invalidBody) {
      // Arrange
      body.set(invalidBody);
      // Act / Assert
      assertThatThrownBy(() -> integration.currentPage(4, 1))
          .isInstanceOf(WhoScoredIntegrationException.class);
      // Verify
      assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("La competición contradictoria invalida la página completa")
    void rejectsContradictoryCompetition() throws Exception {
      // Arrange
      body.set(
          fixture("catalog-current.json").replace("\"tournamentId\": 4", "\"tournamentId\": 5"));
      // Act / Assert
      assertThatThrownBy(() -> integration.currentPage(4, 1))
          .isInstanceOf(WhoScoredIntegrationException.class);
    }

    @Test
    @DisplayName("Rechaza una página por encima del límite sin red")
    void rejectsPageLimitBeforeNetwork() {
      // Act / Assert
      assertThatThrownBy(() -> integration.currentPage(4, 10001))
          .isInstanceOf(WhoScoredIntegrationException.class);
      // Verify
      assertThat(requests.get()).isZero();
    }

    @Test
    @DisplayName("Acepta JSON text html y envía catálogo actual sin JWT")
    void acceptsJsonWithIncorrectMime() throws Exception {
      // Arrange
      body.set(fixture("catalog-current.json"));
      // Act
      WhoScoredCatalogPage page = integration.currentPage(4, 1);
      // Assert
      assertThat(page.identities()).hasSize(3);
      assertThat(page.identities().get(1).externalId()).isEqualTo("102");
      assertThat(page.identities().get(1).teamId()).isNull();
      assertThat(page.paging().totalPages()).isEqualTo(2);
      // Verify
      assertThat(query.get())
          .contains(
              "playerId=",
              "isCurrent=true",
              "tournamentOptions=4",
              "numberOfPlayersToPick=3",
              "page=1");
      assertThat(query.get()).doesNotContain("tournamentId=", "stageId=");
      assertThat(authorization.get()).isNull();
      assertThat(userAgent.get()).isEqualTo("identity-test");
      assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Distingue una primera página vacía válida de una página fuera de rango")
    void validatesEmptyPaging() throws Exception {
      // Arrange
      body.set(fixture("catalog-empty.json"));
      // Act
      WhoScoredCatalogPage empty = integration.currentPage(4, 1);
      // Assert
      assertThat(empty.identities()).isEmpty();
      // Arrange
      body.set(fixture("catalog-invalid.json"));
      // Act / Assert
      assertThatThrownBy(() -> integration.currentPage(4, 159))
          .isInstanceOf(WhoScoredIntegrationException.class)
          .hasMessageNotContaining("playerTableStats");
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 403, 429, 500})
    @DisplayName("No sigue redirecciones ni reintenta errores HTTP")
    void rejectsHttpErrorsWithoutRetry(int responseStatus) {
      // Arrange
      status.set(responseStatus);
      body.set("<html>sensitive-provider-body</html>");
      // Act / Assert
      assertThatThrownBy(() -> integration.currentPage(4, 2))
          .isInstanceOf(WhoScoredIntegrationException.class)
          .hasMessageNotContaining("sensitive-provider-body")
          .asInstanceOf(
              org.assertj.core.api.InstanceOfAssertFactories.type(
                  WhoScoredIntegrationException.class))
          .satisfies(
              failure -> {
                assertThat(failure.getCompetitionId()).isEqualTo(4);
                assertThat(failure.getPage()).isEqualTo(2);
                assertThat(failure.getHttpStatus()).isEqualTo(responseStatus);
              });
      // Verify
      assertThat(requests.get()).isEqualTo(1);
    }
  }

  private String fixture(String name) throws Exception {
    try (java.io.InputStream resource =
        getClass().getResourceAsStream("/whoscored/identity/" + name)) {
      return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Nested
  @DisplayName("Contrato histórico y contextos de identidad")
  class Historical {
    @Test
    @DisplayName("Solicita isCurrent false y conserva variantes y temporadas sin exigir la vigente")
    void preservesHistoricalContexts() throws Exception {
      // Arrange
      body.set(fixture("catalog-historical.json"));
      // Act
      WhoScoredCatalogPage page = integration.historicalPage(4, 1);
      // Assert
      assertThat(page.identities())
          .extracting(row -> row.externalId())
          .containsExactly("101", "101", "105");
      assertThat(page.identities())
          .extracting(row -> row.teamName())
          .containsExactly("Real Madrid", "Sevilla", "Valencia");
      assertThat(page.identities().get(1).seasonName()).isNull();
      assertThat(page.identities().get(0).seasonId()).isEqualTo("1");
      // Verify
      assertThat(query.get()).contains("isCurrent=false", "playerId=", "tournamentOptions=4");
      assertThat(authorization.get()).isNull();
    }

    @Test
    @DisplayName(
        "Un contexto nuevo del mismo ID cambia la huella pero repetirlo no aporta progreso")
    void fingerprintsHistoricalProgress() throws Exception {
      // Arrange
      String original = fixture("catalog-historical.json");
      body.set(original);
      WhoScoredCatalogPage first = integration.historicalPage(4, 1);
      body.set(original.replace("Sevilla", "Betis").replace("\"seasonId\": 2", "\"seasonId\": 3"));
      // Act
      WhoScoredCatalogPage changed = integration.historicalPage(4, 1);
      WhoScoredCatalogPage repeated = integration.historicalPage(4, 1);
      // Assert
      assertThat(changed.fingerprint()).isNotEqualTo(first.fingerprint());
      assertThat(repeated.fingerprint()).isEqualTo(changed.fingerprint());
      assertThat(changed.identities())
          .extracting(row -> row.externalId())
          .containsExactly("101", "101", "105");
    }
  }
}
