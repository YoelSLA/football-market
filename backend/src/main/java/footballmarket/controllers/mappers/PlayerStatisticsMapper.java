package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.responses.PlayerStatisticsResponseDTO;
import footballmarket.models.PlayerStatisticsSnapshot;

/** Transformación de la lectura individual sin seleccionar ni reinterpretar sus datos. */
public final class PlayerStatisticsMapper {
  private PlayerStatisticsMapper() {}

  public static PlayerStatisticsSyncRequest toModel(PlayerStatisticsSyncRequestDTO request) {
    if (request == null) {
      throw new StatisticsMappingException();
    }
    return new PlayerStatisticsSyncRequest(request.extractionDate());
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
  public static PlayerStatisticsResponseDTO toResponse(PlayerStatisticsSnapshot snapshot) {
    return new PlayerStatisticsResponseDTO(
        snapshot.playerId(),
        snapshot.requestedSeasonKey(),
        snapshot.status(),
        snapshot.seasonKey(),
        snapshot.seasonLabel(),
        snapshot.extractionDate(),
        snapshot.competitionKey(),
        snapshot.competitionName(),
        snapshot.metrics().stream()
            .map(
                metric ->
                    new PlayerStatisticsResponseDTO.MetricDTO(
                        metric.key(),
                        metric.unit().name(),
                        metric.value(),
                        metric.source() == null ? null : metric.source().name()))
            .toList());
  }
}
