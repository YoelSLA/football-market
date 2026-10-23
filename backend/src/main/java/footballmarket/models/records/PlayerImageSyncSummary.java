package footballmarket.models.records;

import footballmarket.models.enums.PlayerImageSyncRunStatus;
import java.time.Instant;

/** Proyección de una ejecución para auditoría; no es una entidad persistida ni un DTO HTTP. */
public record PlayerImageSyncSummary(
    Long id,
    PlayerImageSyncRunStatus status,
    boolean force,
    Instant startedAt,
    Instant finishedAt,
    Long durationMillis,
    String failureReason,
    PlayerImageSyncCounters counters) {}
