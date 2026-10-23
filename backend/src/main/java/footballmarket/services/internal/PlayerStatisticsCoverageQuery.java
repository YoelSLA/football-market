package footballmarket.services.internal;

import footballmarket.models.MetricCatalog;
import footballmarket.models.PlayerStatistics;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.enums.MetricSource;
import footballmarket.models.enums.PlayerExtractionResult;
import footballmarket.models.records.PlayerStatisticsCoverage;
import footballmarket.repositories.PlayerExternalReferenceRepository;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.PlayerStatisticsExtractionRunRepository;
import footballmarket.repositories.PlayerStatisticsRepository;
import footballmarket.repositories.PlayerStatisticsRunItemRepository;
import footballmarket.services.exceptions.PlayerStatisticsSynchronizationException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Cobertura consultable de una campaña, derivada únicamente de observaciones persistidas.
 *
 * <p>La ausencia sistémica son las claves del catálogo sin ninguna observación {@code PRESENT} en
 * la población elegible de esa fecha y canal; no se computa como cero ni altera el catálogo.
 */
@Component
public class PlayerStatisticsCoverageQuery {
  private final PlayerRepository players;
  private final PlayerExternalReferenceRepository references;
  private final PlayerStatisticsRunItemRepository items;
  private final PlayerStatisticsRepository statistics;
  private final PlayerStatisticsExtractionRunRepository runs;

  public PlayerStatisticsCoverageQuery(
      PlayerRepository players,
      PlayerExternalReferenceRepository references,
      PlayerStatisticsRunItemRepository items,
      PlayerStatisticsRepository statistics,
      PlayerStatisticsExtractionRunRepository runs) {
    this.players = players;
    this.references = references;
    this.items = items;
    this.statistics = statistics;
    this.runs = runs;
  }

  /** Calcula la cobertura vigente del catálogo para la fecha y el canal indicados. */
  public PlayerStatisticsCoverage coverage(LocalDate extractionDate, MetricSource source) {
    if (extractionDate == null || source == null) {
      throw new PlayerStatisticsSynchronizationException();
    }
    long catalog = this.players.count();
    long eligible = this.references.countEligiblePlayers(ExternalProvider.WHOSCORED);
    long completed =
        this.items.countDistinctPlayersByResult(
            extractionDate, source, PlayerExtractionResult.COMPLETED);
    long pending = this.items.countDistinctPlayersWithoutConfirmation(extractionDate, source);
    long failed = pending;
    long pendingPlayers = Math.max(0, eligible - completed);
    var last =
        this.runs
            .findByExtractionDateAndSourceOrderByStartedAtDescIdDesc(extractionDate, source)
            .stream()
            .findFirst()
            .orElse(null);
    return new PlayerStatisticsCoverage(
        source,
        extractionDate,
        catalog,
        eligible,
        catalog - eligible,
        completed,
        pendingPlayers,
        failed,
        pendingPlayers == 0,
        this.systemicallyAbsentMetrics(extractionDate, source),
        last == null ? null : last.getId(),
        last == null ? null : last.getFinishedAt());
  }

  /** Claves canónicas sin ninguna observación presente para el canal en la fecha consultada. */
  private List<String> systemicallyAbsentMetrics(LocalDate extractionDate, MetricSource source) {
    List<String> catalog =
        MetricCatalog.definitions().stream()
            .filter(definition -> definition.sources().contains(source))
            .map(footballmarket.models.MetricDefinition::key)
            .toList();
    java.util.Set<String> present = new java.util.HashSet<>();
    for (PlayerStatistics row : this.statistics.findMetricsByCampaign(extractionDate, source)) {
      for (String key : catalog) {
        if (row.metricValue(key) != null) {
          present.add(key);
        }
      }
    }
    return catalog.stream().filter(key -> !present.contains(key)).toList();
  }
}
