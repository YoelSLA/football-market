package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.requests.PlayerStatisticsSyncRequestDTO;
import footballmarket.controllers.dtos.responses.PlayerStatisticsCoverageResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerStatisticsSyncResponseDTO;
import footballmarket.controllers.exceptions.StatisticsMappingException;
import footballmarket.models.records.*;

/** Transformaciones estructurales del contrato HTTP de estadísticas. */
public final class PlayerStatisticsMapper {
  private PlayerStatisticsMapper() {}

  public static PlayerStatisticsSyncRequest toModel(PlayerStatisticsSyncRequestDTO request) {
    if (request == null) {
      throw new StatisticsMappingException();
    }
    return new PlayerStatisticsSyncRequest(request.extractionDate(), request.maxPlayers());
  }

  public static PlayerStatisticsSyncResponseDTO toResponse(PlayerStatisticsSyncSummary summary) {
    if (summary == null || summary.counters() == null) {
      throw new StatisticsMappingException();
    }
    PlayerStatisticsSyncCounters counters = summary.counters();
    return new PlayerStatisticsSyncResponseDTO(
        summary.runId(),
        summary.source().name(),
        summary.extractionDate(),
        summary.status().name(),
        summary.startedAt(),
        summary.finishedAt(),
        new PlayerStatisticsSyncResponseDTO.Players(
            counters.eligible(),
            counters.excludedWithoutReference(),
            counters.selected(),
            counters.completed(),
            counters.withoutData(),
            counters.failed(),
            counters.pending()),
        new PlayerStatisticsSyncResponseDTO.Metrics(
            counters.present(), counters.absent(), counters.notDerivable()),
        summary.providerCalls(),
        summary.coverageComplete());
  }

  public static PlayerStatisticsCoverageResponseDTO toResponse(PlayerStatisticsCoverage coverage) {
    if (coverage == null) {
      throw new StatisticsMappingException();
    }
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
