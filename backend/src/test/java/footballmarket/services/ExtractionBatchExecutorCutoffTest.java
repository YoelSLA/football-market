package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.identity.application.PlayerIdentitySynchronizationService;
import footballmarket.features.statistics.application.PlayerStatisticsExtractionService;
import footballmarket.features.statistics.application.PlayerStatisticsSynchronizationService;
import footballmarket.features.statistics.application.internal.ExtractionBatchExecutor;
import footballmarket.features.statistics.application.internal.ExtractionRunManager;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncRequest;
import footballmarket.features.statistics.infraestructure.config.PlayerStatisticsProperties;
import footballmarket.integrations.whoscored.WhoScoredIntegration;
import footballmarket.integrations.whoscored.WhoScoredStatisticsScrapingIntegration;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedContextRow;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedMetricCell;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedPlayer;
import footballmarket.integrations.whoscored.records.WhoScoredStructuredDataBlock;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Corte del lote de scraping sobre la frontera real del Service y la persistencia real.
 *
 * <p>El ritmo se reconstruye de confirmaciones anteriores de igual fecha y canal; sin muestras se
 * aplica el valor técnico conservador. Ningún dato se prepara por repositorio.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, IdentityCatalogueFixture.class})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class ExtractionBatchExecutorCutoffTest {
  @Autowired private PlayerStatisticsSynchronizationService service;
  @Autowired private ExtractionBatchExecutor executor;
  @Autowired private ExtractionRunManager runs;
  @Autowired private PlayerStatisticsProperties properties;
  @Autowired private PlayerIdentitySynchronizationService identities;
  @Autowired private IdentityCatalogueFixture fixture;

  @Autowired
  private footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository
      references;

  @Autowired private footballmarket.features.catalog.infraestructure.PlayerRepository players;
  @MockitoBean private WhoScoredStatisticsScrapingIntegration integration;
  @MockitoBean private WhoScoredIntegration identityIntegration;
  @MockitoSpyBean private WhoScoredProperties providerProperties;
  @MockitoSpyBean private PlayerStatisticsProperties statistics;

  @BeforeEach
  void arrangeConfirmedPlayers() {
    List<Long> leagueIds = fixture.prepare();
    List<Integer> tournaments = List.of(2, 3, 4, 5, 22);
    List<WhoScoredProperties.Competition> bindings = new ArrayList<>();
    for (int index = 0; index < 5; index++) {
      bindings.add(
          new WhoScoredProperties.Competition(leagueIds.get(index), tournaments.get(index)));
    }
    org.mockito.Mockito.doReturn(bindings).when(providerProperties).competitions();
    when(identityIntegration.currentPage(
            org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.eq(1)))
        .thenAnswer(
            invocation -> {
              int tournament = invocation.getArgument(0);
              int index = tournaments.indexOf(tournament) + 1;
              return new footballmarket.integrations.whoscored.records.WhoScoredCatalogPage(
                  List.of(
                      new footballmarket.integrations.whoscored.records.WhoScoredPlayerIdentity(
                          Integer.toString(1000 + index),
                          "Player " + index,
                          null,
                          "Team " + index,
                          tournament,
                          null,
                          null,
                          null)),
                  List.of(),
                  new footballmarket.integrations.whoscored.records.WhoScoredPaging(
                      1, 1, 1, 1, 1, 1),
                  "page-" + tournament);
            });
    doReturn(1_800_000L).when(statistics).scrapingBatchTimeoutMs();
    identities.synchronizeCurrent();
    WhoScoredStatisticsScrapingIntegration.Session session =
        mock(WhoScoredStatisticsScrapingIntegration.Session.class);
    when(integration.openSession()).thenReturn(session);
    when(integration.scrape(any(), any(), any()))
        .thenAnswer(invocation -> scraped(invocation.getArgument(1)));
  }

  /**
   * Fila del proveedor para el jugador recorrido.
   *
   * <p>La competición es la del tournaments enlazado a la liga del jugador: devolver una fija solo
   * resolvería el contexto del jugador de esa liga y el resto se confirmaría sin datos, con lo que
   * ni el ritmo se reconstruye ni el pendiente avanza.
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
            Instant.parse("2026-10-15T12:00:00Z"));
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
        providerProperties.competitions().stream()
            .filter(competition -> competition.leagueId().equals(leagueId))
            .findFirst()
            .orElseThrow()
            .tournamentId());
  }

  private LocalDate today() {
    return LocalDate.now(ZoneOffset.UTC);
  }

  @Nested
  @DisplayName("Reconstrucción del ritmo de campaña")
  class Pace {
    @Test
    @DisplayName("Sin confirmaciones previas se aplica el valor técnico conservador de un minuto")
    void arranqueSinMuestras() {
      // Act
      long pace = executor.paceMillis(today(), MetricSource.SCRAPING);

      // Assert
      assertThat(pace).isEqualTo(60_000L);
    }

    @Test
    @DisplayName("Tras un lote el ritmo se reconstruye con las confirmaciones de esa fecha y canal")
    void ritmoReconstruidoEntreLotes() {
      // Arrange
      service.synchronizeScraping(new PlayerStatisticsSyncRequest(today(), 2));

      // Act
      long pace = executor.paceMillis(today(), MetricSource.SCRAPING);

      // Assert
      assertThat(pace).isPositive().isLessThan(60_000L);
    }

    @Test
    @DisplayName("Las confirmaciones de otro canal no alteran el ritmo de scraping")
    void otroCanalNoAfectaElRitmo() {
      // Arrange
      runs.open(today(), MetricSource.JSON, null, 60_000L);

      // Act
      long pace = executor.paceMillis(today(), MetricSource.SCRAPING);

      // Assert
      assertThat(pace).isEqualTo(60_000L);
    }
  }

  @Nested
  @DisplayName("Corte por presupuesto temporal y máximo global")
  class Cutoff {
    @Test
    @DisplayName("Un presupuesto ya agotado no selecciona ningún jugador ni consulta el proveedor")
    void sinPresupuestoNoSelecciona() {
      // Arrange
      Long runId = runs.open(today(), MetricSource.SCRAPING, null, 60_000L).getId();

      // Act
      ExtractionBatchExecutor.Outcome outcome =
          executor.execute(
              this.batchWithoutProvider(),
              runId,
              today(),
              MetricSource.SCRAPING,
              properties.scrapingWorkers(),
              null,
              Instant.now().minusMillis(1));

      // Assert
      assertThat(outcome.selected()).isZero();
      verifyNoInteractions(integration);
    }

    @Test
    @DisplayName("El máximo solicitado es global entre los cuatro workers configurados")
    void maxPlayersGlobalEntreWorkers() {
      // Act
      var summary = service.synchronizeScraping(new PlayerStatisticsSyncRequest(today(), 1));

      // Assert
      assertThat(properties.scrapingWorkers()).isEqualTo(4);
      assertThat(summary.counters().selected()).isEqualTo(1);
      assertThat(summary.counters().pending()).isEqualTo(4);
    }

    @Test
    @DisplayName("Un fallo del proveedor por jugador no interrumpe el resto del lote")
    void falloPuntualNoInterrumpe() {
      // Arrange
      // doThrow: con when() el doble se invoca con null y se ejecuta la respuesta del arrange.
      doThrow(new WhoScoredStatisticsException("PROVIDER_BLOCKED", 403))
          .when(integration)
          .scrape(any(), any(), any());

      // Act
      var summary = service.synchronizeScraping(new PlayerStatisticsSyncRequest(today(), 3));

      // Assert
      assertThat(summary.counters().selected()).isEqualTo(3);
      assertThat(summary.counters().failed()).isEqualTo(3);
      assertThat(summary.counters().pending()).isEqualTo(5);
    }

    @Test
    @DisplayName(
        "Una indisponibilidad prolongada cierra el lote con lo alcanzado y pendientes explícitos")
    void indisponibilidadProlongada() {
      // Arrange
      doThrow(new WhoScoredStatisticsException("PROVIDER_TIMEOUT", null))
          .when(integration)
          .scrape(any(), any(), any());

      // Act
      var summary = service.synchronizeScraping(new PlayerStatisticsSyncRequest(today(), null));

      // Assert
      assertThat(summary.status())
          .isEqualTo(footballmarket.features.statistics.domain.enums.ExtractionRunStatus.FAILED);
      assertThat(summary.counters().pending()).isEqualTo(5);
      assertThat(summary.coverageComplete()).isFalse();
    }

    private PlayerStatisticsExtractionService.Batch batchWithoutProvider() {
      return new PlayerStatisticsExtractionService.Batch() {
        @Override
        public Long extract(Long runId, Long playerId, Instant deadline) {
          throw new AssertionError("No debe recorrerse ningún jugador sin presupuesto");
        }

        @Override
        public void close() {}
      };
    }
  }
}
