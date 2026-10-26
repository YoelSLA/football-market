package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.responses.PlayerStatisticsCoverageResponseDTO;
import footballmarket.models.records.PlayerStatisticsCoverage;

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
