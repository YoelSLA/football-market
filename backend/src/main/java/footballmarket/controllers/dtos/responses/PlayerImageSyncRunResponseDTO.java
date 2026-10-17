package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Resumen HTTP de una ejecución, sin incorporar resultados individuales. */
public record PlayerImageSyncRunResponseDTO(
    Long id,
    @Schema(description = "RUNNING, COMPLETED, PARTIAL o FAILED") String status,
    boolean force,
    Instant startedAt,
    @Schema(nullable = true) Instant finishedAt,
    @Schema(nullable = true) Long durationMillis,
    @Schema(nullable = true) String failureReason,
    PlayerImageSyncCountersResponseDTO counters) {}
