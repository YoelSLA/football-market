package footballmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import footballmarket.features.auth.presentation.dtos.request.LoginRequestDTO;
import footballmarket.features.auth.presentation.dtos.request.RegisterRequestDTO;
import footballmarket.features.auth.presentation.dtos.response.LoginResponseDTO;
import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.presentation.dtos.response.PlayerResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayersPageResponseDTO;
import footballmarket.features.identity.presentation.dtos.response.PlayerIdentitySyncRunResponseDTO;
import footballmarket.features.statistics.presentation.dtos.request.PlayerStatisticsSyncRequestDTO;
import footballmarket.features.statistics.presentation.dtos.response.PlayerStatisticsCoverageResponseDTO;
import footballmarket.features.statistics.presentation.dtos.response.PlayerStatisticsSyncResponseDTO;
import footballmarket.integrations.whoscored.WhoScoredStatisticsScrapingIntegration;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedContextRow;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedMetricCell;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedPlayer;
import footballmarket.integrations.whoscored.records.WhoScoredStructuredDataBlock;
import footballmarket.support.IdentityControlledProvider;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestClient;

/** Journey HTTP completo de un lote de scraping y su consulta de cobertura. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("e2e")
class PlayerStatisticsSyncJourneyE2ETest {
  private static final IdentityControlledProvider PROVIDER = startProvider();
  @LocalServerPort private int port;
  @MockitoSpyBean private WhoScoredProperties properties;

  @org.springframework.beans.factory.annotation.Autowired
  private footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository
      references;

  @org.springframework.beans.factory.annotation.Autowired
  private footballmarket.features.catalog.infraestructure.PlayerRepository players;

  @MockitoBean private WhoScoredStatisticsScrapingIntegration integration;

  @TestBean(name = "whoScoredRestClient", methodName = "providerClient")
  private RestClient whoScored;

  @TestBean(name = "footballDataRestClient", methodName = "providerClient")
  private RestClient footballData;

  @TestBean(name = "theSportsDbRestClient", methodName = "providerClient")
  private RestClient sportsDb;

  private static IdentityControlledProvider startProvider() {
    try {
      return new IdentityControlledProvider();
    } catch (java.io.IOException failure) {
      throw new ExceptionInInitializerError(failure);
    }
  }

  static RestClient providerClient() {
    return PROVIDER.client();
  }

  @AfterAll
  static void stopProvider() {
    PROVIDER.close();
  }

  private RestClient api() {
    return RestClient.builder().baseUrl("http://localhost:" + port).build();
  }

  private String authenticatedUser(RestClient api) {
    RegisterRequestDTO register =
        new RegisterRequestDTO("statistics.journey", "statistics-journey@test.com", "password123");
    api.post().uri("/api/auth/register").body(register).retrieve().toBodilessEntity();
    LoginResponseDTO authenticated =
        api.post()
            .uri("/api/auth/login")
            .body(new LoginRequestDTO(register.email(), register.password()))
            .retrieve()
            .body(LoginResponseDTO.class);
    return authenticated.token();
  }

  private void confirmIdentities(RestClient api, String token) {
    api.post()
        .uri("/api/players/sync")
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .toBodilessEntity();
    PlayersPageResponseDTO catalogue =
        api.get()
            .uri("/api/players?size=100")
            .headers(headers -> headers.setBearerAuth(token))
            .retrieve()
            .body(PlayersPageResponseDTO.class);
    List<Integer> tournaments = List.of(2, 3, 4, 5, 22);
    List<WhoScoredProperties.Competition> bindings = new ArrayList<>();
    for (int index = 1; index <= 5; index++) {
      String leagueName = "League " + index;
      PlayerResponseDTO local =
          catalogue.content().stream()
              .filter(player -> leagueName.equals(player.leagueName()))
              .findFirst()
              .orElseThrow();
      bindings.add(
          new WhoScoredProperties.Competition(local.leagueId(), tournaments.get(index - 1)));
    }
    org.mockito.Mockito.doReturn(bindings).when(properties).competitions();
    PlayerIdentitySyncRunResponseDTO identities =
        api.post()
            .uri("/api/players/identities/whoscored/sync")
            .headers(headers -> headers.setBearerAuth(token))
            .retrieve()
            .body(PlayerIdentitySyncRunResponseDTO.class);
    assertThat(identities.id()).isPositive();
  }

  @Nested
  @DisplayName("Lote de scraping y cobertura desde HTTP")
  class ScrapingJourney {
    @BeforeEach
    void arrangeProvider() {
      when(integration.openSession())
          .thenReturn(mock(WhoScoredStatisticsScrapingIntegration.Session.class));
      when(integration.scrape(any(), any(), any()))
          .thenAnswer(invocation -> scraped(invocation.getArgument(1)));
    }

    /**
     * Fila del proveedor para el jugador recorrido.
     *
     * <p>La competición es la del tournaments enlazado a la liga del jugador: devolver una fija
     * solo resolvería el contexto de uno de cada cinco y el resto se confirmaría sin datos.
     */
    private WhoScoredScrapedPlayer scraped(String playerId) {
      WhoScoredScrapedContextRow row =
          new WhoScoredScrapedContextRow(
              "initial",
              tournamentOf(playerId),
              "200",
              "11",
              "2025/2026",
              "2024/2025",
              List.of(
                  new WhoScoredScrapedMetricCell("apps", "6"),
                  new WhoScoredScrapedMetricCell("rating", "6.9033333333333333"),
                  new WhoScoredScrapedMetricCell("goal", "-")),
              java.time.Instant.parse("2026-10-15T12:00:00Z"));
      return new WhoScoredScrapedPlayer(
          playerId, List.of(row), new WhoScoredStructuredDataBlock(List.of()), 2, List.of());
    }

    private String tournamentOf(String providerPlayerId) {
      Long playerId =
          references
              .findByProviderAndExternalId(ExternalProvider.WHOSCORED, providerPlayerId)
              .orElseThrow()
              .getPlayer()
              .getId();
      Long leagueId = players.findVisibleById(playerId).orElseThrow().getTeam().getLeague().getId();
      return String.valueOf(
          properties.competitions().stream()
              .filter(competition -> competition.leagueId().equals(leagueId))
              .findFirst()
              .orElseThrow()
              .tournamentId());
    }

    @Test
    @DisplayName("Un lote de un jugador deja cobertura consultable sin repetir la extracción")
    void loteDeUnJugadorYCoverage() {
      // Arrange
      RestClient api = api();
      String token = authenticatedUser(api);
      confirmIdentities(api, token);
      LocalDate today = LocalDate.now(ZoneOffset.UTC);

      // Act
      PlayerStatisticsSyncResponseDTO summary =
          api.post()
              .uri("/api/players/statistics/whoscored/sync/scraping")
              .headers(headers -> headers.setBearerAuth(token))
              .body(new PlayerStatisticsSyncRequestDTO(today, 1))
              .retrieve()
              .body(PlayerStatisticsSyncResponseDTO.class);
      PlayerStatisticsCoverageResponseDTO coverage =
          api.get()
              .uri("/api/players/statistics/whoscored/coverage?source=SCRAPING")
              .headers(headers -> headers.setBearerAuth(token))
              .retrieve()
              .body(PlayerStatisticsCoverageResponseDTO.class);

      // Assert
      assertThat(summary.source()).isEqualTo("SCRAPING");
      assertThat(summary.status()).isEqualTo("COMPLETED");
      assertThat(summary.players().selected()).isEqualTo(1);
      assertThat(summary.players().completed()).isEqualTo(1);
      assertThat(summary.coverageComplete()).isFalse();
      assertThat(coverage.source()).isEqualTo("SCRAPING");
      assertThat(coverage.extractionDate()).isEqualTo(today);
      assertThat(coverage.completedPlayers()).isEqualTo(1);
      assertThat(coverage.pendingPlayers()).isEqualTo(4);
      assertThat(coverage.lastRunId()).isEqualTo(summary.runId());
    }

    @Test
    @DisplayName("La entrada JSON rechaza una consulta no permitida sin ejecutar el canal")
    void jsonRechazaEntradaNoPermitida() {
      // Arrange
      RestClient api = api();
      String token = authenticatedUser(api);

      // Act
      HttpStatusCode status =
          api.post()
              .uri("/api/players/statistics/whoscored/sync/json?maxPlayers=1")
              .headers(headers -> headers.setBearerAuth(token))
              .exchange((request, response) -> response.getStatusCode());

      // Assert
      assertThat(status.value()).isEqualTo(400);
    }
  }
}
