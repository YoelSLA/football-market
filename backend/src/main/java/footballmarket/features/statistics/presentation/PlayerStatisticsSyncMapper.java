package footballmarket.features.statistics.presentation;

import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncSummary;
import footballmarket.features.statistics.presentation.dtos.response.PlayerStatisticsSyncResponseDTO;

/** Transformación del resumen de sincronización a DTO HTTP sin lógica de negocio. */
public final class PlayerStatisticsSyncMapper {
  private PlayerStatisticsSyncMapper() {}

  public static PlayerStatisticsSyncResponseDTO toResponse(PlayerStatisticsSyncSummary summary) {
    return new PlayerStatisticsSyncResponseDTO(
        summary.runId(),
        summary.source().name(),
        summary.extractionDate(),
        summary.status().name(),
        summary.startedAt(),
        summary.finishedAt(),
        new PlayerStatisticsSyncResponseDTO.Players(
            summary.counters().eligible(),
            summary.counters().excludedWithoutReference(),
            summary.counters().selected(),
            summary.counters().completed(),
            summary.counters().withoutData(),
            summary.counters().failed(),
            summary.counters().pending()),
        new PlayerStatisticsSyncResponseDTO.Metrics(
            summary.counters().present(),
            summary.counters().absent(),
            summary.counters().notDerivable()),
        summary.providerCalls(),
        summary.coverageComplete());
  }
}
