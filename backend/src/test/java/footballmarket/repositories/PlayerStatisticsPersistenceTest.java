package footballmarket.repositories;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

import footballmarket.config.WhoScoredProperties;
import footballmarket.models.*;
import footballmarket.models.enums.*;
import footballmarket.services.exceptions.PlayerStatisticsConfirmationException;
import footballmarket.services.internal.ExtractionIncidentRecorder;
import footballmarket.services.internal.PlayerMetricsContext;
import footballmarket.services.internal.PlayerMetricsContext.MetricResult;
import footballmarket.services.internal.PlayerStatisticsWriter;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Fila ancha por jugador, campaña y canal.
 *
 * <p>La confirmación persiste una sola fila con el contexto de la liga local del jugador, y la
 * fuente forma parte de la clave única junto con la fecha de campaña.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class PlayerStatisticsPersistenceTest {
  @Autowired private LeagueRepository leagues;
  @Autowired private TeamRepository teams;
  @Autowired private PlayerRepository players;
  @Autowired private PlayerStatisticsExtractionRunRepository runs;
  @Autowired private PlayerStatisticsRunItemRepository items;
  @Autowired private PlayerStatisticsRepository statistics;
  @Autowired private PlayerStatisticsWriter writer;
  @Autowired private ExtractionIncidentRecorder incidentRecorder;
  @Autowired private ExtractionIncidentRepository incidents;
  @Autowired private PlatformTransactionManager manager;
  @Autowired private EntityManager entityManager;
  @MockitoSpyBean private WhoScoredProperties properties;

  private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
  private static final LocalDate DATE = LocalDate.of(2026, 10, 15);
  private static final String TOURNAMENT = "2";

  private record Fixture(Long runId, Long playerId) {}

  private Fixture prepare(MetricSource source, LocalDate date, Long existingPlayerId) {
    Fixture fixture =
        new TransactionTemplate(manager)
            .execute(
                status -> {
                  Player player;
                  if (existingPlayerId == null) {
                    League league = leagues.save(new League("Statistics League"));
                    Team team = teams.save(new Team("Statistics Team", league, true));
                    player = new Player("Statistics Player", team, "Forward");
                    player.addExternalReference(ExternalProvider.WHOSCORED, "123");
                    players.saveAndFlush(player);
                  } else {
                    player = players.findById(existingPlayerId).orElseThrow();
                  }
                  PlayerStatisticsExtractionRun run =
                      runs.saveAndFlush(new PlayerStatisticsExtractionRun(date, source, NOW, null));
                  return new Fixture(run.getId(), player.getId());
                });
    this.bindTournament(fixture.playerId());
    return fixture;
  }

  /** Enlaza la liga creada con la competencia equivalente del proveedor. */
  private void bindTournament(Long playerId) {
    // El equipo y su liga se leen dentro de la transaccion: son proxies perezosos y no se
    // inicializan fuera de sesion.
    Long leagueId =
        new TransactionTemplate(manager)
            .execute(
                status -> players.findById(playerId).orElseThrow().getTeam().getLeague().getId());
    List<WhoScoredProperties.Competition> bindings = new ArrayList<>();
    for (int index = 0; index < 5; index++) {
      bindings.add(
          index == 0
              ? new WhoScoredProperties.Competition(leagueId, Integer.parseInt(TOURNAMENT))
              : new WhoScoredProperties.Competition(leagueId + 100 + index, 100 + index));
    }
    doReturn(bindings).when(this.properties).competitions();
  }

  private void finishRun(Long runId) {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status ->
                runs.findById(runId)
                    .orElseThrow()
                    .finish(
                        ExtractionRunStatus.COMPLETED,
                        NOW.plusSeconds(2),
                        1,
                        0,
                        1,
                        1,
                        0,
                        0,
                        0,
                        2,
                        0,
                        0,
                        0,
                        null));
  }

  private List<PlayerMetricsContext> values(String rating) {
    Map<String, MetricResult> metrics = new LinkedHashMap<>();
    metrics.put("whoscored_rating_average", new MetricResult(new BigDecimal(rating), 16));
    metrics.put("goals_per_match", new MetricResult(new BigDecimal("0.333333"), 6));
    return List.of(new PlayerMetricsContext(TOURNAMENT, "3", "2026", "2026/27", metrics));
  }

  /** Contexto de una liga distinta a la del jugador: no debe conservarse. */
  private List<PlayerMetricsContext> foreignContexts() {
    Map<String, MetricResult> metrics = new LinkedHashMap<>();
    metrics.put("goals_total", new MetricResult(new BigDecimal("9"), 0));
    return List.of(new PlayerMetricsContext("9999", "3", "2026", "2026/27", metrics));
  }

  @Nested
  @DisplayName("Fila ancha por campaña y canal")
  class Persistence {
    @Test
    @DisplayName("Audita códigos admitidos y rechaza contenido externo en código o sección")
    void recordsOnlySanitizedIncidents() {
      // Arrange
      Fixture fixture = prepare(MetricSource.SCRAPING, DATE, null);
      Long itemId =
          new TransactionTemplate(manager)
              .execute(
                  status -> {
                    PlayerStatisticsExtractionRun run =
                        runs.findById(fixture.runId()).orElseThrow();
                    Player player = players.findById(fixture.playerId()).orElseThrow();
                    PlayerStatisticsRunItem item =
                        new PlayerStatisticsRunItem(run, player, "123", NOW);
                    item.finish(
                        PlayerExtractionResult.PROVIDER_ERROR,
                        NOW.plusSeconds(1),
                        0,
                        0,
                        0,
                        "PROVIDER_TIMEOUT");
                    return items.saveAndFlush(item).getId();
                  });
      // Act
      incidentRecorder.record(itemId, "saves", "PROVIDER_TIMEOUT", 504, 2, NOW);
      // Assert
      List<ExtractionIncident> recorded =
          new TransactionTemplate(manager)
              .execute(
                  status ->
                      entityManager
                          .createQuery(
                              "select i from ExtractionIncident i where i.runItem.id = :itemId"
                                  + " order by i.occurredAt, i.id",
                              ExtractionIncident.class)
                          .setParameter("itemId", itemId)
                          .getResultList());
      assertThat(recorded)
          .singleElement()
          .satisfies(
              incident -> {
                assertThat(incident.getCode()).isEqualTo("PROVIDER_TIMEOUT");
                assertThat(incident.getSection()).isEqualTo("saves");
                assertThat(incident.getProviderStatus()).isEqualTo(504);
                assertThat(incident.getAttempt()).isEqualTo(2);
              });
      assertThatThrownBy(
              () ->
                  incidentRecorder.record(
                      itemId, "<html>external</html>", "PROVIDER_TIMEOUT", 504, 2, NOW))
          .isInstanceOf(
              footballmarket.services.exceptions.PlayerStatisticsSynchronizationException.class);
      assertThatThrownBy(
              () ->
                  incidentRecorder.record(
                      itemId, "saves", "Authorization: synthetic-secret", 504, 2, NOW))
          .isInstanceOf(
              footballmarket.services.exceptions.PlayerStatisticsSynchronizationException.class);
      // Verify
      assertThat(incidents.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Confirma una sola fila con el contexto de la liga local del jugador")
    void confirmsSingleRowForPlayerLeague() {
      // Arrange
      Fixture fixture = prepare(MetricSource.JSON, DATE, null);
      // Act
      writer.confirm(
          fixture.runId(),
          fixture.playerId(),
          "123",
          NOW,
          NOW.plusSeconds(1),
          values("6.9033333333333333"));
      // Assert
      assertThat(statistics.findMetricsByCampaign(DATE, MetricSource.JSON))
          .singleElement()
          .satisfies(
              row -> {
                assertThat(row.getContextStatus()).isEqualTo("RESOLVED");
                assertThat(row.getProviderCompetitionId()).isEqualTo(TOURNAMENT);
                assertThat(row.getSeasonKey()).isEqualTo("2026");
                assertThat(row.metricValue("whoscored_rating_average"))
                    .isEqualTo(new BigDecimal("6.9033333333333333"));
                assertThat(row.metricValue("goals_total")).isNull();
                assertThat(row.getPresentMetrics()).isEqualTo(2);
              });
    }

    @Test
    @DisplayName("Un contexto ajeno a la liga del jugador no se persiste ni se suma")
    void ignoresForeignLeagueContext() {
      // Arrange
      Fixture fixture = prepare(MetricSource.JSON, DATE, null);
      // Act
      writer.confirm(
          fixture.runId(), fixture.playerId(), "123", NOW, NOW.plusSeconds(1), foreignContexts());
      // Assert
      assertThat(statistics.findMetricsByCampaign(DATE, MetricSource.JSON))
          .singleElement()
          .satisfies(
              row -> {
                assertThat(row.getContextStatus()).isEqualTo("NO_LEAGUE_CONTEXT");
                assertThat(row.metricValue("goals_total")).isNull();
              });
    }

    @Test
    @DisplayName("Una fila sin contexto resuelto se reconfirma en la misma fila sin duplicarla")
    void reconfirmsUnresolvedRowInPlace() {
      // Arrange
      Fixture first = prepare(MetricSource.JSON, DATE, null);
      writer.confirm(
          first.runId(), first.playerId(), "123", NOW, NOW.plusSeconds(1), foreignContexts());
      finishRun(first.runId());
      Long rowId = statistics.findMetricsByCampaign(DATE, MetricSource.JSON).getFirst().getId();
      Fixture second = prepare(MetricSource.JSON, DATE, first.playerId());

      // Act
      writer.confirm(
          second.runId(),
          second.playerId(),
          "123",
          NOW,
          NOW.plusSeconds(1),
          values("7.1234567890123456"));

      // Assert
      assertThat(statistics.findMetricsByCampaign(DATE, MetricSource.JSON))
          .singleElement()
          .satisfies(
              row -> {
                assertThat(row.getId()).isEqualTo(rowId);
                assertThat(row.getContextStatus()).isEqualTo("RESOLVED");
                assertThat(row.metricValue("whoscored_rating_average")).isNotNull();
                assertThat(row.metricValue("goals_total")).isNull();
              });
    }

    @Test
    @DisplayName("Un jugador que sigue sin liga conserva una sola fila y su ausencia explícita")
    void keepsSingleRowWhenStillUnresolved() {
      // Arrange
      Fixture first = prepare(MetricSource.JSON, DATE, null);
      writer.confirm(
          first.runId(), first.playerId(), "123", NOW, NOW.plusSeconds(1), foreignContexts());
      finishRun(first.runId());
      Long rowId = statistics.findMetricsByCampaign(DATE, MetricSource.JSON).getFirst().getId();
      Fixture second = prepare(MetricSource.JSON, DATE, first.playerId());

      // Act
      writer.confirm(
          second.runId(), second.playerId(), "123", NOW, NOW.plusSeconds(1), foreignContexts());

      // Assert
      assertThat(statistics.findMetricsByCampaign(DATE, MetricSource.JSON))
          .singleElement()
          .satisfies(
              row -> {
                assertThat(row.getId()).isEqualTo(rowId);
                assertThat(row.getContextStatus()).isEqualTo("NO_LEAGUE_CONTEXT");
                assertThat(row.metricValue("goals_total")).isNull();
              });
    }

    @Test
    @DisplayName("Un fallo de validación posterior a crear el item revierte toda la confirmación")
    void rollsBackInvalidMetric() {
      // Arrange
      Fixture fixture = prepare(MetricSource.SCRAPING, DATE, null);
      PlayerMetricsContext invalid =
          new PlayerMetricsContext(
              TOURNAMENT,
              "3",
              "2026",
              "2026/27",
              Map.of("not_in_catalog", new MetricResult(BigDecimal.ONE, 0)));
      // Act / Assert
      assertThatThrownBy(
              () ->
                  writer.confirm(
                      fixture.runId(),
                      fixture.playerId(),
                      "123",
                      NOW,
                      NOW.plusSeconds(1),
                      List.of(invalid)))
          .isInstanceOfSatisfying(
              PlayerStatisticsConfirmationException.class,
              error -> assertThat(error.isAlreadyCompleted()).isFalse());
      // Verify
      assertThat(items.count()).isZero();
      assertThat(statistics.count()).isZero();
    }

    @Test
    @DisplayName(
        "Pendientes excluye solo confirmados de la misma campaña y filtra la población sin referencia")
    void selectsPendingCampaign() {
      // Arrange
      Fixture fixture = prepare(MetricSource.SCRAPING, DATE, null);
      Long otherId =
          new TransactionTemplate(manager)
              .execute(
                  status -> {
                    Player original = players.findById(fixture.playerId()).orElseThrow();
                    Player eligible = new Player("Other", original.getTeam(), "Forward");
                    eligible.addExternalReference(ExternalProvider.WHOSCORED, "456");
                    players.saveAndFlush(eligible);
                    Player excluded = new Player("Excluded", original.getTeam(), "Forward");
                    players.saveAndFlush(excluded);
                    return eligible.getId();
                  });
      PlayerStatisticsPersistenceTest.this.bindTournament(otherId);
      // Act
      List<Long> before =
          items.findPendingPlayerIds(
              DATE, MetricSource.SCRAPING, org.springframework.data.domain.Pageable.unpaged());
      writer.confirm(
          fixture.runId(),
          fixture.playerId(),
          "123",
          NOW,
          NOW.plusSeconds(1),
          values("6.9033333333333333"));
      // Assert
      assertThat(before).containsExactly(fixture.playerId(), otherId);
      assertThat(
              items.findPendingPlayerIds(
                  DATE, MetricSource.SCRAPING, org.springframework.data.domain.Pageable.unpaged()))
          .containsExactly(otherId);
      assertThat(
              items.findPendingPlayerIds(
                  DATE, MetricSource.JSON, org.springframework.data.domain.Pageable.unpaged()))
          .containsExactly(fixture.playerId(), otherId);
      assertThat(
              items.findPendingPlayerIds(
                  DATE.plusDays(1),
                  MetricSource.SCRAPING,
                  org.springframework.data.domain.Pageable.unpaged()))
          .containsExactly(fixture.playerId(), otherId);
    }

    @Test
    @DisplayName("Cada canal conserva su propia fila y no pisa al otro")
    void keepsOneRowPerSource() {
      // Arrange
      Fixture scraping = prepare(MetricSource.SCRAPING, DATE, null);
      // Act
      writer.confirm(
          scraping.runId(),
          scraping.playerId(),
          "123",
          NOW,
          NOW.plusSeconds(1),
          values("6.9033333333333333"));
      finishRun(scraping.runId());
      Fixture json = prepare(MetricSource.JSON, DATE, scraping.playerId());
      writer.confirm(
          json.runId(),
          json.playerId(),
          "123",
          NOW,
          NOW.plusSeconds(1),
          values("7.1234567890123456"));
      // Assert
      assertThat(statistics.findMetricsByCampaign(DATE, MetricSource.SCRAPING))
          .singleElement()
          .satisfies(
              row ->
                  assertThat(row.metricValue("whoscored_rating_average"))
                      .isEqualTo(new BigDecimal("6.9033333333333333")));
      assertThat(statistics.findMetricsByCampaign(DATE, MetricSource.JSON))
          .singleElement()
          .satisfies(
              row ->
                  assertThat(row.metricValue("whoscored_rating_average"))
                      .isEqualTo(new BigDecimal("7.1234567890123456")));
      assertThat(statistics.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Recupera exactamente el rating de dieciséis decimales y la derivada de seis")
    void preservesDecimalRoundTrip() {
      // Arrange
      Fixture fixture = prepare(MetricSource.SCRAPING, DATE, null);
      // Act
      writer.confirm(
          fixture.runId(),
          fixture.playerId(),
          "123",
          NOW,
          NOW.plusSeconds(1),
          values("6.9033333333333333"));
      // Assert
      PlayerStatistics saved =
          statistics.findMetricsByCampaign(DATE, MetricSource.SCRAPING).getFirst();
      assertThat(saved.metricValue("whoscored_rating_average"))
          .isEqualTo(new BigDecimal("6.9033333333333333"));
      assertThat(saved.metricValue("goals_per_match")).isEqualTo(new BigDecimal("0.333333"));
    }

    @Test
    @DisplayName("Una confirmación duplicada revierte item y fila y se traduce de forma controlada")
    void rollsBackDuplicateCampaign() {
      // Arrange
      Fixture first = prepare(MetricSource.SCRAPING, DATE, null);
      writer.confirm(
          first.runId(),
          first.playerId(),
          "123",
          NOW,
          NOW.plusSeconds(1),
          values("6.9033333333333333"));
      finishRun(first.runId());
      Fixture duplicate = prepare(MetricSource.SCRAPING, DATE, first.playerId());
      long itemCount = items.count();
      long rowCount = statistics.count();
      // Act / Assert
      assertThatThrownBy(
              () ->
                  writer.confirm(
                      duplicate.runId(),
                      duplicate.playerId(),
                      "123",
                      NOW,
                      NOW.plusSeconds(1),
                      values("7.1234567890123456")))
          .isInstanceOfSatisfying(
              PlayerStatisticsConfirmationException.class,
              error -> assertThat(error.isAlreadyCompleted()).isTrue());
      // Verify
      assertThat(items.count()).isEqualTo(itemCount);
      assertThat(statistics.count()).isEqualTo(rowCount);
    }
  }
}
