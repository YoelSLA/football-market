package footballmarket.support;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.statistics.domain.enums.ExtractionRunStatus;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.features.statistics.domain.models.MetricCatalog;
import footballmarket.features.statistics.domain.models.PlayerStatistics;
import footballmarket.features.statistics.domain.models.PlayerStatisticsExtractionRun;
import footballmarket.features.statistics.domain.models.PlayerStatisticsRunItem;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsExtractionRunRepository;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsRepository;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsRunItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

/** Fixture compartido que prepara una campaña completa exclusivamente durante Arrange. */
public class ValuationTestData {
  private final PlayerRepository players;
  private final PlayerStatisticsExtractionRunRepository runs;
  private final PlayerStatisticsRunItemRepository items;
  private final PlayerStatisticsRepository statistics;

  public ValuationTestData(
      PlayerRepository players,
      PlayerStatisticsExtractionRunRepository runs,
      PlayerStatisticsRunItemRepository items,
      PlayerStatisticsRepository statistics) {
    this.players = players;
    this.runs = runs;
    this.items = items;
    this.statistics = statistics;
  }

  @Transactional
  public void prepareConfirmedJsonCampaign(LocalDate date) {
    List<Player> population = this.players.findAll();
    PlayerStatisticsExtractionRun run =
        this.runs.save(
            new PlayerStatisticsExtractionRun(date, MetricSource.JSON, Instant.EPOCH, null));
    for (Player player : population) {
      String externalId = externalId(player);
      player.addExternalReference(ExternalProvider.WHOSCORED, externalId);
      this.players.save(player);
      PlayerStatisticsRunItem item =
          new PlayerStatisticsRunItem(run, player, externalId, Instant.EPOCH);
      item.finish(PlayerExtractionResult.COMPLETED, Instant.EPOCH.plusSeconds(1), 1, 57, 0, null);
      this.items.save(item);
      PlayerStatistics row =
          new PlayerStatistics(
              player,
              date,
              MetricSource.JSON,
              PlayerStatistics.RESOLVED,
              item,
              Instant.EPOCH.plusSeconds(1));
      row.resolveContext("2", "team", "2026", "2026/27");
      Map<String, BigDecimal> metrics = new LinkedHashMap<>();
      for (footballmarket.features.statistics.domain.records.MetricDefinition definition :
          MetricCatalog.definitions()) {
        if (definition.sources().contains(MetricSource.JSON))
          metrics.put(definition.key(), BigDecimal.ONE);
      }
      row.applyMetrics(metrics, metrics.size(), 60 - metrics.size());
      this.statistics.save(row);
    }
    run.finish(
        ExtractionRunStatus.COMPLETED,
        Instant.EPOCH.plusSeconds(2),
        population.size(),
        0,
        population.size(),
        population.size(),
        0,
        0,
        0,
        57L * population.size(),
        0,
        0,
        population.size(),
        null);
  }

  /**
   * Campaña terminada con un item sin datos del proveedor, un jugador elegible sin item ni fila y
   * un run parcial. La campaña es confirmable: la ausencia de datos no la invalida.
   */
  @Transactional
  public void preparePartialJsonCampaign(LocalDate date) {
    List<Player> population = this.eligiblePopulation();
    PlayerStatisticsExtractionRun run =
        this.runs.save(
            new PlayerStatisticsExtractionRun(date, MetricSource.JSON, Instant.EPOCH, null));
    Player withoutData = population.get(0);
    Player withoutSnapshot = population.get(population.size() - 1);
    for (Player player : population.subList(1, population.size() - 1)) {
      PlayerStatisticsRunItem item = this.completedItem(run, player);
      this.items.save(item);
      this.statistics.save(this.completedRow(player, date, item));
    }
    PlayerStatisticsRunItem noDataItem =
        new PlayerStatisticsRunItem(run, withoutData, externalId(withoutData), Instant.EPOCH);
    noDataItem.finish(PlayerExtractionResult.NO_DATA, Instant.EPOCH.plusSeconds(1), 0, 0, 60, null);
    this.items.save(noDataItem);
    PlayerStatistics noDataRow =
        new PlayerStatistics(
            withoutData,
            date,
            MetricSource.JSON,
            PlayerStatistics.NO_LEAGUE_CONTEXT,
            noDataItem,
            Instant.EPOCH.plusSeconds(1));
    noDataRow.applyMetrics(Map.of(), 0, 60);
    this.statistics.save(noDataRow);
    long completed = population.size() - 2;
    run.finish(
        ExtractionRunStatus.PARTIAL,
        Instant.EPOCH.plusSeconds(2),
        population.size(),
        0,
        completed + 1,
        completed,
        1,
        0,
        1,
        57L * completed,
        0,
        0,
        completed + 1,
        null);
  }

  /** Campaña terminada con un item en fallo estructural: no puede confirmar el snapshot. */
  @Transactional
  public void prepareStructurallyFailedJsonCampaign(LocalDate date) {
    List<Player> population = this.eligiblePopulation();
    PlayerStatisticsExtractionRun run =
        this.runs.save(
            new PlayerStatisticsExtractionRun(date, MetricSource.JSON, Instant.EPOCH, null));
    Player blocked = population.get(0);
    for (Player player : population.subList(1, population.size())) {
      PlayerStatisticsRunItem item = this.completedItem(run, player);
      this.items.save(item);
      this.statistics.save(this.completedRow(player, date, item));
    }
    PlayerStatisticsRunItem blockedItem =
        new PlayerStatisticsRunItem(run, blocked, externalId(blocked), Instant.EPOCH);
    blockedItem.finish(
        PlayerExtractionResult.BLOCKED, Instant.EPOCH.plusSeconds(1), 0, 0, 60, "BLOCKED");
    this.items.save(blockedItem);
    long completed = population.size() - 1;
    run.finish(
        ExtractionRunStatus.PARTIAL,
        Instant.EPOCH.plusSeconds(2),
        population.size(),
        0,
        population.size(),
        completed,
        0,
        1,
        0,
        57L * completed,
        0,
        0,
        population.size(),
        null);
  }

  private List<Player> eligiblePopulation() {
    List<Player> population = this.players.findAll();
    for (Player player : population) {
      player.addExternalReference(ExternalProvider.WHOSCORED, externalId(player));
      this.players.save(player);
    }
    return population;
  }

  /** Identificador WhoScored numérico y único por jugador, como exige el dominio. */
  private static String externalId(Player player) {
    return Long.toString(900_000L + player.getId());
  }

  private PlayerStatisticsRunItem completedItem(PlayerStatisticsExtractionRun run, Player player) {
    PlayerStatisticsRunItem item =
        new PlayerStatisticsRunItem(run, player, externalId(player), Instant.EPOCH);
    item.finish(PlayerExtractionResult.COMPLETED, Instant.EPOCH.plusSeconds(1), 1, 57, 0, null);
    return item;
  }

  private PlayerStatistics completedRow(
      Player player, LocalDate date, PlayerStatisticsRunItem item) {
    PlayerStatistics row =
        new PlayerStatistics(
            player,
            date,
            MetricSource.JSON,
            PlayerStatistics.RESOLVED,
            item,
            Instant.EPOCH.plusSeconds(1));
    row.resolveContext("2", "team", "2026", "2026/27");
    Map<String, BigDecimal> metrics = new LinkedHashMap<>();
    for (footballmarket.features.statistics.domain.records.MetricDefinition definition :
        MetricCatalog.definitions()) {
      if (definition.sources().contains(MetricSource.JSON))
        metrics.put(definition.key(), BigDecimal.ONE);
    }
    row.applyMetrics(metrics, metrics.size(), 60 - metrics.size());
    return row;
  }
}
