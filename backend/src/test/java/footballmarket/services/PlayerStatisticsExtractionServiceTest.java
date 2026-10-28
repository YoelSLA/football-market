package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import footballmarket.config.WhoScoredProperties;
import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.statistics.application.PlayerStatisticsExtractionService;
import footballmarket.features.statistics.application.exceptions.PlayerStatisticsSynchronizationException;
import footballmarket.features.statistics.application.internal.ExtractionRunManager;
import footballmarket.features.statistics.application.internal.PlayerStatisticsCoverageQuery;
import footballmarket.features.statistics.domain.enums.ExtractionRunStatus;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.integrations.WhoScoredIntegration;
import footballmarket.integrations.WhoScoredStatisticsScrapingIntegration;
import footballmarket.integrations.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.records.WhoScoredScrapedContextRow;
import footballmarket.integrations.records.WhoScoredScrapedMetricCell;
import footballmarket.integrations.records.WhoScoredScrapedPlayer;
import footballmarket.integrations.records.WhoScoredStructuredDataBlock;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.LocalDate;
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
 * Extracción de un jugador con la infraestructure productiva de ejecuciones de US02.
 *
 * <p>La ejecución se abre con el gestor de runs del caso de uso; el test no prepara datos
 * funcionales ni afirma sobre persistencia accesible solo por repositorio.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, IdentityCatalogueFixture.class})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class PlayerStatisticsExtractionServiceTest {
  private static final LocalDate DATE = LocalDate.of(2026, 10, 15);
  @Autowired private PlayerStatisticsExtractionService extraction;
  @Autowired private ExtractionRunManager runs;
  @Autowired private PlayerStatisticsCoverageQuery coverage;
  @Autowired private PlayerIdentitySynchronizationService identities;
  @Autowired private IdentityCatalogueFixture fixture;

  @Autowired
  private footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository
      references;

  @Autowired private footballmarket.features.catalog.infraestructure.PlayerRepository players;
  @Autowired private PlayerCatalogService catalogue;
  @MockitoBean private WhoScoredStatisticsScrapingIntegration integration;
  @MockitoBean private WhoScoredIntegration identityIntegration;
  @MockitoSpyBean private WhoScoredProperties properties;
  private Long playerId;

  @BeforeEach
  void arrangeConfirmedPlayer() {
    List<Long> leagueIds = fixture.prepare();
    List<Integer> tournaments = List.of(2, 3, 4, 5, 22);
    List<WhoScoredProperties.Competition> bindings = new ArrayList<>();
    for (int index = 0; index < 5; index++) {
      bindings.add(
          new WhoScoredProperties.Competition(leagueIds.get(index), tournaments.get(index)));
    }
    org.mockito.Mockito.doReturn(bindings).when(properties).competitions();
    when(identityIntegration.currentPage(
            org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.eq(1)))
        .thenAnswer(
            invocation -> {
              int tournament = invocation.getArgument(0);
              int index = tournaments.indexOf(tournament) + 1;
              return new footballmarket.integrations.records.WhoScoredCatalogPage(
                  List.of(
                      new footballmarket.integrations.records.WhoScoredPlayerIdentity(
                          Integer.toString(1000 + index),
                          "Player " + index,
                          null,
                          "Team " + index,
                          tournament,
                          null,
                          null,
                          null)),
                  List.of(),
                  new footballmarket.integrations.records.WhoScoredPaging(1, 1, 1, 1, 1, 1),
                  "page-" + tournament);
            });
    identities.synchronizeCurrent();
    playerId = catalogue.getActivePlayers(0, 10).getContent().getFirst().getId();
    when(integration.openSession())
        .thenReturn(mock(WhoScoredStatisticsScrapingIntegration.Session.class));
    // getArgument con tipo: String.valueOf(genérico) infería char[] y rompía en tiempo de
    // ejecución.
    when(integration.scrape(any(), any(), any()))
        .thenAnswer(invocation -> scraped(invocation.getArgument(1, String.class)));
  }

  /**
   * Fila del proveedor para el jugador que se le indica.
   *
   * <p>Se construye desde el identificador externo que recibe el recorrido, no desde un jugador
   * prefijado: devolver siempre el mismo identificador y la misma competición confirma como válido
   * un contexto que no corresponde al jugador recorrido.
   */
  private WhoScoredScrapedPlayer scraped(String providerPlayerId) {
    WhoScoredScrapedMetricCell apps = new WhoScoredScrapedMetricCell("apps", "6");
    WhoScoredScrapedMetricCell rating =
        new WhoScoredScrapedMetricCell("rating", "6.9033333333333333");
    WhoScoredScrapedMetricCell absent = new WhoScoredScrapedMetricCell("goal", "-");
    WhoScoredScrapedContextRow row =
        new WhoScoredScrapedContextRow(
            "initial",
            tournamentOf(providerPlayerId),
            "200",
            "11",
            "2025/2026",
            "2024/2025",
            List.of(apps, rating, absent),
            java.time.Instant.parse("2026-10-15T12:00:00Z"));
    return new WhoScoredScrapedPlayer(
        providerPlayerId, List.of(row), new WhoScoredStructuredDataBlock(List.of()), 2, List.of());
  }

  private Long openRun() {
    return runs.open(DATE, MetricSource.SCRAPING, null, 1_800_000L).getId();
  }

  @Nested
  @DisplayName("Extracción individual sobre una ejecución abierta")
  class Extraction {
    @Test
    @DisplayName("Confirma el jugador y sus contextos sin perder el rating publicado")
    void confirmaJugadorYContextos() {
      // Arrange
      Long runId = openRun();

      // Act
      try (PlayerStatisticsExtractionService.Batch batch = extraction.openBatch()) {
        batch.extract(runId, playerId, null);
      }

      // Assert / Verify
      var result = coverage.coverage(DATE, MetricSource.SCRAPING);
      assertThat(result.completedPlayers()).isEqualTo(1);
      assertThat(result.pendingPlayers()).isEqualTo(4);
      assertThat(result.systemicallyAbsentMetrics()).isNotNull();
    }

    @Test
    @DisplayName("Un fallo del proveedor deja el jugador fallido sin confirmar datos parciales")
    void falloNoConfirmaDatosParciales() {
      // Arrange
      Long runId = openRun();
      // doThrow: con when() el doble se invoca con null y se ejecuta la respuesta del arrange.
      org.mockito.Mockito.doThrow(
              new WhoScoredStatisticsException("PROVIDER_STRUCTURE_CHANGED", 200))
          .when(integration)
          .scrape(any(), any(), any());

      // Act
      try (PlayerStatisticsExtractionService.Batch batch = extraction.openBatch()) {
        assertThatThrownBy(() -> batch.extract(runId, playerId, null))
            .isInstanceOf(WhoScoredStatisticsException.class);
      }

      // Assert / Verify
      var result = coverage.coverage(DATE, MetricSource.SCRAPING);
      assertThat(result.completedPlayers()).isZero();
      assertThat(result.failedPlayers()).isEqualTo(1);
      assertThat(result.pendingPlayers()).isEqualTo(5);
    }

    @Test
    @DisplayName("Un jugador sin referencia confirmada no se extrae")
    void rechazaJugadorSinReferencia() {
      // Arrange
      Long runId = openRun();

      // Act / Assert
      try (PlayerStatisticsExtractionService.Batch batch = extraction.openBatch()) {
        assertThatThrownBy(() -> batch.extract(runId, 999_999L, null))
            .isInstanceOf(PlayerStatisticsSynchronizationException.class);
      }
    }

    @Test
    @DisplayName("Una ejecución cerrada no admite nuevas confirmaciones")
    void rejectsClosedRun() {
      // Arrange
      Long runId = openRun();
      runs.close(runId, ExtractionRunStatus.COMPLETED, null, 0);

      // Act / Assert
      try (PlayerStatisticsExtractionService.Batch batch = extraction.openBatch()) {
        assertThatThrownBy(() -> batch.extract(runId, playerId, null))
            .isInstanceOf(PlayerStatisticsSynchronizationException.class);
      }
    }
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
}
