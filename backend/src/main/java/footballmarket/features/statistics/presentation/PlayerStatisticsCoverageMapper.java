package footballmarket.features.statistics.presentation;

import footballmarket.features.statistics.domain.records.PlayerStatisticsCoverage;
import footballmarket.features.statistics.presentation.dtos.response.PlayerStatisticsCoverageResponseDTO;

/** Transformación de la consulta de cobertura a DTO HTTP sin lógica de negocio. */
public final class PlayerStatisticsCoverageMapper {
  private PlayerStatisticsCoverageMapper() {}

  public static PlayerStatisticsCoverageResponseDTO toResponse(PlayerStatisticsCoverage coverage) {
    return new PlayerStatisticsCoverageResponseDTO(
        coverage.source().name(),
        coverage.extractionDate(),
        coverage.catalogPlayers(),
        coverage.eligiblePlayers(),
        coverage.excludedWithoutReference(),
        coverage.completedPlayers(),
        coverage.pendingPlayers(),
        coverage.failedPlayers(),
        coverage.coverageComplete(),
        coverage.systemicallyAbsentMetrics(),
        coverage.lastRunId(),
        coverage.lastRunFinishedAt());
  }
}
