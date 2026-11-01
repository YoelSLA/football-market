package footballmarket.features.statistics.application.shared.internal;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.statistics.application.exceptions.PlayerStatisticsSynchronizationException;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.features.statistics.domain.records.PlayerStatisticsCoverage;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsRunItemRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/** Calcula población y pendientes de una campaña para el resumen interno de sincronización. */
@Component
public class PlayerStatisticsCoverageQuery {
  private final PlayerRepository players;
  private final PlayerExternalReferenceRepository references;
  private final PlayerStatisticsRunItemRepository items;

  public PlayerStatisticsCoverageQuery(
      PlayerRepository players,
      PlayerExternalReferenceRepository references,
      PlayerStatisticsRunItemRepository items) {
    this.players = players;
    this.references = references;
    this.items = items;
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
    long pendingPlayers = Math.max(0, eligible - completed);
    return new PlayerStatisticsCoverage(
        eligible, catalog - eligible, pendingPlayers, pendingPlayers == 0);
  }
}
