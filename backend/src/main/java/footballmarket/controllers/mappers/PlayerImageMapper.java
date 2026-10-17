package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.responses.PlayerImageSyncCountersResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerImageSyncRunItemPageResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerImageSyncRunItemResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerImageSyncRunPageResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerImageSyncRunResponseDTO;
import footballmarket.models.records.PlayerImageSyncCounters;
import footballmarket.models.records.PlayerImageSyncRunItemRecord;
import footballmarket.models.records.PlayerImageSyncSummary;
import org.springframework.data.domain.Page;

/** Conversión estricta de proyecciones del Service a los contratos HTTP de imágenes. */
public final class PlayerImageMapper {
  private PlayerImageMapper() {}

  public static PlayerImageSyncRunResponseDTO toResponse(PlayerImageSyncSummary summary) {
    PlayerImageSyncCounters counters = summary.counters();
    PlayerImageSyncCountersResponseDTO responseCounters =
        new PlayerImageSyncCountersResponseDTO(
            counters.evaluated(),
            counters.processed(),
            counters.found(),
            counters.notFound(),
            counters.retryableErrors(),
            counters.failed(),
            counters.conflicts(),
            counters.skippedFound(),
            counters.skippedRetryWindow(),
            counters.skippedFailed(),
            counters.interrupted());
    return new PlayerImageSyncRunResponseDTO(
        summary.id(),
        summary.status().name(),
        summary.force(),
        summary.startedAt(),
        summary.finishedAt(),
        summary.durationMillis(),
        summary.failureReason(),
        responseCounters);
  }

  public static PlayerImageSyncRunPageResponseDTO toRunPage(Page<PlayerImageSyncSummary> page) {
    return new PlayerImageSyncRunPageResponseDTO(
        page.getContent().stream().map(PlayerImageMapper::toResponse).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }

  public static PlayerImageSyncRunItemPageResponseDTO toItemPage(
      Page<PlayerImageSyncRunItemRecord> page) {
    return new PlayerImageSyncRunItemPageResponseDTO(
        page.getContent().stream().map(PlayerImageMapper::toResponse).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }

  public static PlayerImageSyncRunItemResponseDTO toResponse(PlayerImageSyncRunItemRecord item) {
    return new PlayerImageSyncRunItemResponseDTO(
        item.playerId(),
        item.previousState().name(),
        item.finalState() == null ? null : item.finalState().name(),
        item.result() == null ? null : item.result().name(),
        item.identityResolved(),
        item.imageFoundOrUpdated(),
        item.skipped(),
        item.skipReason() == null ? null : item.skipReason().name(),
        item.conflict(),
        item.errorOccurred(),
        item.outcomeDetail());
  }
}
