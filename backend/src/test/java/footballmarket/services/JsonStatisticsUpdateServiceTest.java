package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import footballmarket.config.WhoScoredProperties;
import footballmarket.integrations.WhoScoredIntegration;
import footballmarket.integrations.WhoScoredRequestPacer;
import footballmarket.integrations.WhoScoredStatisticsIntegration;
import footballmarket.integrations.records.WhoScoredStatisticsResponse;
import footballmarket.integrations.records.WhoScoredStatisticsTable;
import footballmarket.models.Player;
import footballmarket.models.PlayerStatistics;
import footballmarket.models.enums.ExtractionRunStatus;
import footballmarket.models.enums.MetricSource;
import footballmarket.repositories.PlayerStatisticsRepository;
import footballmarket.services.exceptions.PlayerStatisticsConfirmationException;
import footballmarket.services.internal.ExtractionRunManager;
import footballmarket.services.internal.PlayerStatisticsCoverageQuery;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.Instant;
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
 * Actualización JSON independiente con servicio, persistencia e integración reales dobladas.
 *
 * <p>Cubrir que la fila conserva una sola métrica por columna, que lo no publicado queda nulo sin
 * inventar ceros y que una campaña nueva no se confunde con repetir un jugador ya completado el
 * mismo día.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, IdentityCatalogueFixture.class})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class JsonStatisticsUpdateServiceTest {
  private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

  @Autowired private JsonStatisticsUpdateService updates;
  @Autowired private ExtractionRunManager runs;
  @Autowired private PlayerStatisticsCoverageQuery coverage;
  @Autowired private PlayerStatisticsRepository statistics;
  @Autowired private PlayerIdentitySynchronizationService identities;
  @Autowired private PlayerCatalogService catalogue;
  @Autowired private IdentityCatalogueFixture fixture;
  @Autowired private WhoScoredRequestPacer pacer;
  @MockitoBean private WhoScoredStatisticsIntegration integration;
  @MockitoBean private WhoScoredIntegration identityIntegration;
  @MockitoSpyBean private WhoScoredProperties properties;

  private List<WhoScoredProperties.Competition> bindings;
  private Long playerId;

  @BeforeEach
  void arrangeConfirmedPlayer() {
    List<Long> leagueIds = fixture.prepare();
    List<Integer> tournaments = List.of(2, 3, 4, 5, 22);
    this.bindings = new ArrayList<>();
    for (int index = 0; index < 5; index++) {
      this.bindings.add(
          new WhoScoredProperties.Competition(leagueIds.get(index), tournaments.get(index)));
    }
    org.mockito.Mockito.doReturn(this.bindings).when(this.properties).competitions();
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
    this.playerId = catalogue.getActivePlayers(0, 10).getContent().getFirst().getId();
  }

  /** Competencia del proveedor equivalente a la liga del jugador bajo prueba. */
  private String playerTournament() {
    Player player = catalogue.getActivePlayers(0, 10).getContent().getFirst();
    Long leagueId = player.getTeam().getLeague().getId();
    for (WhoScoredProperties.Competition competition : this.bindings) {
      if (competition.leagueId().equals(leagueId)) {
        return Integer.toString(competition.tournamentId());
      }
    }
    throw new IllegalStateException("La liga del jugador no está enlazada con una competencia");
  }

  private void enqueueResponse(String externalPlayerId, String rating) {
    enqueueResponseForTournament(externalPlayerId, playerTournament(), rating);
  }

  /** Respuesta del proveedor publicada en una competencia que no es la liga del jugador. */
  private void enqueueUnmatchedResponse(String externalPlayerId) {
    enqueueResponseForTournament(externalPlayerId, "777", "6.9033333333333333");
  }

  private void enqueueResponseForTournament(
      String externalPlayerId, String tournament, String rating) {
    WhoScoredStatisticsTable table =
        new WhoScoredStatisticsTable(
            "summary/all",
            tournament,
            "200",
            "2026",
            "2026/27",
            null,
            List.of(
                new WhoScoredStatisticsTable.Cell("apps", "6"),
                new WhoScoredStatisticsTable.Cell("rating", rating)),
            NOW);
    when(integration.fetch(any(), any()))
        .thenReturn(new WhoScoredStatisticsResponse(externalPlayerId, List.of(table), NOW));
  }

  private Long openRun(LocalDate date) {
    return runs.open(date, MetricSource.JSON, 86_400_000L).getId();
  }

  private WhoScoredRequestPacer.StatisticsWorker worker() {
    return pacer.statisticsWorker(1);
  }

  @Nested
  @DisplayName("Actualización JSON de un jugador elegible")
  class Update {
    @Test
    @DisplayName("Confirma el jugador en una fila y deja el resto pendiente")
    void confirmaJugadorJson() {
      // Arrange
      LocalDate date = LocalDate.now(java.time.ZoneOffset.UTC);
      Long runId = openRun(date);
      enqueueResponse("1001", "6.9033333333333333");

      // Act
      var outcome = updates.update(runId, playerId, worker());

      // Assert
      var result = coverage.coverage(date, MetricSource.JSON);
      assertThat(result.completedPlayers()).isEqualTo(1);
      assertThat(outcome.itemId()).isNotNull();
      assertThat(statistics.findMetricsByCampaign(date, MetricSource.JSON))
          .singleElement()
          .satisfies(
              row -> {
                assertThat(row.getContextStatus()).isEqualTo("RESOLVED");
                assertThat(row.metricValue("whoscored_rating_average")).isNotNull();
                assertThat(row.getPresentMetrics()).isPositive();
              });
    }

    @Test
    @DisplayName("Una métrica no publicada queda nula y no se persiste como cero")
    void metricaNoPublicadaQuedaNula() {
      // Arrange
      LocalDate date = LocalDate.now(java.time.ZoneOffset.UTC);
      Long runId = openRun(date);
      enqueueResponse("1001", "6.9033333333333333");

      // Act
      updates.update(runId, playerId, worker());

      // Assert
      PlayerStatistics row = statistics.findMetricsByCampaign(date, MetricSource.JSON).getFirst();
      assertThat(row.metricValue("yellow_cards_total")).isNull();
      assertThat(row.metricValue("appearances_total")).isNotNull();
      assertThat(row.getPresentMetrics()).isEqualTo(2);
      assertThat(row.getAbsentMetrics()).isEqualTo(55);
    }

    @Test
    @DisplayName("Repetir un jugador ya completado el mismo día se rechaza como campaña repetida")
    void noRepiteConfirmadoDelMismoDia() {
      // Arrange
      LocalDate date = LocalDate.now(java.time.ZoneOffset.UTC);
      Long runId = openRun(date);
      enqueueResponse("1001", "6.9033333333333333");
      updates.update(runId, playerId, worker());
      runs.close(runId, ExtractionRunStatus.COMPLETED, null, 0);
      Long secondRun = openRun(date);

      // Act / Assert
      assertThatThrownBy(() -> updates.update(secondRun, playerId, worker()))
          .isInstanceOfSatisfying(
              PlayerStatisticsConfirmationException.class,
              failure -> assertThat(failure.isAlreadyCompleted()).isTrue());
    }

    @Test
    @DisplayName("Un jugador sin contexto resuelto se reconfirma el mismo día sin duplicar su fila")
    void reconfirmaSinContextoResuelto() {
      // Arrange
      LocalDate date = LocalDate.now(java.time.ZoneOffset.UTC);
      Long runId = openRun(date);
      enqueueUnmatchedResponse("1001");
      updates.update(runId, playerId, worker());
      runs.close(runId, ExtractionRunStatus.PARTIAL, null, 0);
      Long secondRun = openRun(date);
      enqueueResponse("1001", "6.9033333333333333");

      // Act
      var outcome = updates.update(secondRun, playerId, worker());

      // Assert
      assertThat(outcome.itemId()).isNotNull();
      assertThat(statistics.findMetricsByCampaign(date, MetricSource.JSON))
          .singleElement()
          .satisfies(
              row -> {
                assertThat(row.getContextStatus()).isEqualTo(PlayerStatistics.RESOLVED);
                assertThat(row.metricValue("whoscored_rating_average")).isNotNull();
              });
      assertThat(coverage.coverage(date, MetricSource.JSON).completedPlayers()).isEqualTo(1);
    }

    @Test
    @DisplayName("Una campaña de otra fecha vuelve a permitir la consulta del mismo jugador")
    void nuevaCampanaPermiteRepeticion() {
      // Arrange
      LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
      Long runId = openRun(today);
      enqueueResponse("1001", "6.9033333333333333");
      updates.update(runId, playerId, worker());
      runs.close(runId, ExtractionRunStatus.COMPLETED, null, 0);
      Long otherRun = openRun(today.minusDays(1));

      // Act
      var outcome = updates.update(otherRun, playerId, worker());

      // Assert
      assertThat(outcome.itemId()).isNotNull();
      assertThat(coverage.coverage(today.minusDays(1), MetricSource.JSON).completedPlayers())
          .isEqualTo(1);
      assertThat(statistics.count()).isEqualTo(2);
    }
  }
}
