package footballmarket.services.internal;

import footballmarket.models.PlayerStatistics;
import footballmarket.models.enums.ExtractionRunStatus;
import footballmarket.models.enums.MetricSource;
import footballmarket.models.enums.PlayerExtractionResult;
import footballmarket.repositories.PlayerStatisticsExtractionRunRepository;
import footballmarket.repositories.PlayerStatisticsRepository;
import footballmarket.repositories.PlayerStatisticsRunItemRepository;
import footballmarket.services.exceptions.ValuationSnapshotUnavailableException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Selecciona la última campaña JSON confirmada.
 *
 * <p>Una campaña está confirmada cuando su run está terminado, ningún item quedó en estado de fallo
 * estructural y todo item confirmado tiene su fila JSON de esa fecha. Los items sin datos del
 * proveedor no invalidan la campaña y los fallos de un jugador concreto no la invalidan tampoco:
 * esos jugadores quedan sin snapshot y se excluyen durante el recorrido poblacional.
 */
@Component
public class ValuationSnapshotSelector {
  private static final Set<ExtractionRunStatus> TERMINAL_RUN_STATUSES =
      Set.of(ExtractionRunStatus.COMPLETED, ExtractionRunStatus.PARTIAL);

  private static final Set<PlayerExtractionResult> STRUCTURAL_FAILURE_RESULTS =
      Set.of(
          PlayerExtractionResult.INCOMPLETE,
          PlayerExtractionResult.INTERRUPTED,
          PlayerExtractionResult.BLOCKED,
          PlayerExtractionResult.STRUCTURE_CHANGED);

  private final PlayerStatisticsRepository statistics;
  private final PlayerStatisticsRunItemRepository items;
  private final PlayerStatisticsExtractionRunRepository runs;

  public ValuationSnapshotSelector(
      PlayerStatisticsRepository statistics,
      PlayerStatisticsRunItemRepository items,
      PlayerStatisticsExtractionRunRepository runs) {
    this.statistics = statistics;
    this.items = items;
    this.runs = runs;
  }

  public Snapshot select() {
    for (LocalDate date : this.runs.findJsonCampaignDatesDescending(TERMINAL_RUN_STATUSES)) {
      if (!this.isConfirmed(date)) continue;
      List<PlayerStatistics> rows = this.statistics.findValuationCampaign(date, MetricSource.JSON);
      List<PlayerStatistics> scraping =
          this.statistics.findValuationCampaign(date, MetricSource.SCRAPING);
      return new Snapshot(date, rows, scraping, this.items.findEligiblePlayerIds());
    }
    throw new ValuationSnapshotUnavailableException();
  }

  private boolean isConfirmed(LocalDate date) {
    long confirmed =
        this.items.countDistinctPlayersByResult(
            date, MetricSource.JSON, PlayerExtractionResult.COMPLETED);
    if (confirmed == 0) return false;
    long structuralFailures =
        this.items.countDistinctPlayersByResults(
            date, MetricSource.JSON, STRUCTURAL_FAILURE_RESULTS);
    if (structuralFailures > 0) return false;
    long rowsWithConfirmedItem =
        this.statistics.countValuationRowsWithCompletedItem(date, MetricSource.JSON);
    return rowsWithConfirmedItem == confirmed;
  }

  public record Snapshot(
      LocalDate extractionDate,
      List<PlayerStatistics> jsonRows,
      List<PlayerStatistics> scrapingRows,
      List<Long> eligiblePlayerIds) {}
}
