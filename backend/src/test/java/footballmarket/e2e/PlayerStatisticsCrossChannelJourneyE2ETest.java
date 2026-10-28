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
import footballmarket.integrations.whoscored.WhoScoredRequestPacer;
import footballmarket.integrations.whoscored.WhoScoredStatisticsIntegration;
import footballmarket.integrations.whoscored.WhoScoredStatisticsScrapingIntegration;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.whoscored.records.WhoScoredJsonQuery;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedContextRow;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedMetricCell;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedPlayer;
import footballmarket.integrations.whoscored.records.WhoScoredStatisticsResponse;
import footballmarket.integrations.whoscored.records.WhoScoredStatisticsTable;
import footballmarket.integrations.whoscored.records.WhoScoredStructuredDataBlock;
import footballmarket.support.IdentityControlledProvider;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestClient;

/**
 * Journey HTTP de ambos canales sobre la misma población.
 *
 * <p>El JSON se invoca sin parámetros ni cuerpo, recorre todos los pendientes del día UTC y no
 * repite confirmados; el scraping mantiene su propio corte y su propio avance. Reloj controlado,
 * HTTP real y proveedores doblados: nunca se accede a WhoScored.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("e2e")
class PlayerStatisticsCrossChannelJourneyE2ETest {
  private static final IdentityControlledProvider PROVIDER = startProvider();
  private static final CampaignClock CLOCK =
      new CampaignClock(Instant.parse("2026-10-15T09:00:00Z"));

  @LocalServerPort private int port;
  @Autowired private WhoScoredRequestPacer pacer;

  @Autowired
  private footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository
      references;

  @Autowired private footballmarket.features.catalog.infraestructure.PlayerRepository players;
  @MockitoSpyBean private WhoScoredProperties properties;
  @MockitoBean private WhoScoredStatisticsScrapingIntegration scrapingIntegration;
  @MockitoBean private WhoScoredStatisticsIntegration jsonIntegration;

  @TestBean(name = "whoScoredRestClient", methodName = "providerClient")
  private RestClient whoScored;

  @TestBean(name = "footballDataRestClient", methodName = "providerClient")
  private RestClient footballData;

  @TestBean(name = "theSportsDbRestClient", methodName = "providerClient")
  private RestClient sportsDb;

  @TestBean(name = "statisticsClock", methodName = "campaignClock")
  private Clock statisticsClock;

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

  static Clock campaignClock() {
    return CLOCK;
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
        new RegisterRequestDTO("statistics.cross", "statistics-cross@test.com", "password123");
    api.post().uri("/api/auth/register").body(register).retrieve().toBodilessEntity();
    return api.post()
        .uri("/api/auth/login")
        .body(new LoginRequestDTO(register.email(), register.password()))
        .retrieve()
        .body(LoginResponseDTO.class)
        .token();
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

  private PlayerStatisticsSyncResponseDTO postScraping(
      RestClient api, String token, int maxPlayers) {
    return api.post()
        .uri("/api/players/statistics/whoscored/sync/scraping")
        .headers(headers -> headers.setBearerAuth(token))
        .body(
            new PlayerStatisticsSyncRequestDTO(
                LocalDate.now(CLOCK.withZone(ZoneOffset.UTC)), maxPlayers))
        .retrieve()
        .body(PlayerStatisticsSyncResponseDTO.class);
  }

  private PlayerStatisticsSyncResponseDTO postJson(RestClient api, String token) {
    return api.post()
        .uri("/api/players/statistics/whoscored/sync/json")
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .body(PlayerStatisticsSyncResponseDTO.class);
  }

  private PlayerStatisticsCoverageResponseDTO coverage(
      RestClient api, String token, String source) {
    return coverage(api, token, source, LocalDate.now(CLOCK.withZone(ZoneOffset.UTC)));
  }

  /**
   * Cobertura de una fecha concreta.
   *
   * <p>La fecha se declara de forma explícita porque el endpoint toma la fecha actual real cuando
   * no se le pasa, y el reloj de campaña del test no es el reloj del sistema.
   */
  private PlayerStatisticsCoverageResponseDTO coverage(
      RestClient api, String token, String source, LocalDate extractionDate) {
    return api.get()
        .uri(
            "/api/players/statistics/whoscored/coverage?source="
                + source
                + "&extractionDate="
                + extractionDate)
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .body(PlayerStatisticsCoverageResponseDTO.class);
  }

  @Nested
  @DisplayName("Entrada JSON sin parámetros sobre la población del día")
  class JsonJourney {
    @BeforeEach
    void arrangeProviders() {
      CLOCK.set(Instant.parse("2026-10-15T09:00:00Z"));
      when(scrapingIntegration.openSession())
          .thenReturn(mock(WhoScoredStatisticsScrapingIntegration.Session.class));
      when(scrapingIntegration.scrape(any(), any(), any()))
          .thenAnswer(invocation -> scraped(invocation.getArgument(1)));
      // El ritmo del canal JSON lo aporta el pacer real: la integración doblada no crea workers.
      when(jsonIntegration.newWorker()).thenAnswer(ignored -> pacer.statisticsWorker(1));
      when(jsonIntegration.fetch(any(), any()))
          .thenAnswer(
              invocation -> {
                WhoScoredStatisticsTable table =
                    new WhoScoredStatisticsTable(
                        "summary/all",
                        tournamentOf(invocation.getArgument(0)),
                        "200",
                        "2025/2026",
                        "2025/2026",
                        null,
                        List.of(
                            new WhoScoredStatisticsTable.Cell("apps", "6"),
                            new WhoScoredStatisticsTable.Cell("rating", "6.9033333333333333")),
                        CLOCK.instant());
                return new WhoScoredStatisticsResponse(
                    invocation.getArgument(0), List.of(table), CLOCK.instant());
              });
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
                  new WhoScoredScrapedMetricCell("rating", "6.9033333333333333")),
              CLOCK.instant());
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
    @DisplayName("Un POST JSON recorre todos los pendientes del día sin corte de población")
    void recorreTodosLosPendientes() {
      // Arrange
      RestClient api = api();
      String token = authenticatedUser(api);
      confirmIdentities(api, token);

      // Act
      PlayerStatisticsSyncResponseDTO summary = postJson(api, token);

      // Assert
      assertThat(summary.source()).isEqualTo("JSON");
      assertThat(summary.status()).isEqualTo("COMPLETED");
      assertThat(summary.players().eligible()).isEqualTo(5);
      assertThat(summary.players().selected()).isEqualTo(5);
      assertThat(summary.players().completed()).isEqualTo(5);
      assertThat(summary.players().pending()).isZero();
      assertThat(summary.providerCalls()).isEqualTo(5L * WhoScoredJsonQuery.COMBINATIONS.size());
    }

    @Test
    @DisplayName("La segunda llamada del mismo día omite confirmados y no consulta al proveedor")
    void omiteConfirmadosDelDia() {
      // Arrange
      RestClient api = api();
      String token = authenticatedUser(api);
      confirmIdentities(api, token);
      postJson(api, token);

      // Act
      PlayerStatisticsSyncResponseDTO summary = postJson(api, token);

      // Assert
      assertThat(summary.status()).isEqualTo("COMPLETED");
      assertThat(summary.players().selected()).isZero();
      assertThat(summary.providerCalls()).isZero();
      assertThat(coverage(api, token, "JSON").completedPlayers()).isEqualTo(5);
    }

    @Test
    @DisplayName("Una fecha UTC nueva abre otra campaña y vuelve a consultar los pendientes")
    void nuevaFechaUtcAbreOtraCampana() {
      // Arrange
      RestClient api = api();
      String token = authenticatedUser(api);
      confirmIdentities(api, token);
      postJson(api, token);
      CLOCK.set(Instant.parse("2026-10-16T09:00:00Z"));

      // Act
      PlayerStatisticsSyncResponseDTO summary = postJson(api, token);

      // Assert
      assertThat(summary.extractionDate()).isEqualTo(LocalDate.of(2026, 10, 16));
      assertThat(summary.players().completed()).isEqualTo(5);
    }

    @Test
    @DisplayName("Una petición que cruza medianoche conserva la fecha fijada al inicio")
    void conservaFechaAlCruzarMedianoche() {
      // Arrange
      RestClient api = api();
      String token = authenticatedUser(api);
      confirmIdentities(api, token);
      CLOCK.set(Instant.parse("2026-10-15T23:59:59Z"));

      // Act
      PlayerStatisticsSyncResponseDTO summary = postJson(api, token);
      CLOCK.set(Instant.parse("2026-10-16T00:30:00Z"));

      // Assert
      assertThat(summary.extractionDate()).isEqualTo(LocalDate.of(2026, 10, 15));
      // La campaña se inició el 15 y el reloj ya es el 16: la cobertura que se consulta es la del
      // 15.
      assertThat(coverage(api, token, "JSON", LocalDate.of(2026, 10, 15)).completedPlayers())
          .isEqualTo(5);
    }

    @Test
    @DisplayName("Los canales son independientes: el avance de scraping no reduce el alcance JSON")
    void canalesIndependientes() {
      // Arrange
      RestClient api = api();
      String token = authenticatedUser(api);
      confirmIdentities(api, token);
      postScraping(api, token, 1);

      // Act
      PlayerStatisticsSyncResponseDTO json = postJson(api, token);

      // Assert
      assertThat(json.players().completed()).isEqualTo(5);
      assertThat(coverage(api, token, "SCRAPING").completedPlayers()).isEqualTo(1);
      assertThat(coverage(api, token, "SCRAPING").pendingPlayers()).isEqualTo(4);
    }

    @Test
    @DisplayName("Una indisponibilidad deja los fallidos pendientes sin reselección infinita")
    void fallidosQuedanPendientes() {
      // Arrange
      RestClient api = api();
      String token = authenticatedUser(api);
      confirmIdentities(api, token);
      // doThrow evita invocar la respuesta previa del doble con argumentos nulos.
      org.mockito.Mockito.doThrow(new WhoScoredStatisticsException("PROVIDER_TIMEOUT", null))
          .when(jsonIntegration)
          .fetch(any(), any());

      // Act
      PlayerStatisticsSyncResponseDTO summary = postJson(api, token);

      // Assert
      assertThat(summary.players().failed()).isEqualTo(5);
      assertThat(summary.players().completed()).isZero();
      assertThat(summary.players().pending()).isEqualTo(5);
      assertThat(summary.status()).isNotEqualTo("COMPLETED");
    }
  }

  /** Reloj UTC controlado por la prueba para demostrar la vigencia de la campaña. */
  static final class CampaignClock extends Clock {
    private volatile Instant instant;

    CampaignClock(Instant instant) {
      this.instant = instant;
    }

    void set(Instant value) {
      this.instant = value;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return this.instant;
    }
  }
}
