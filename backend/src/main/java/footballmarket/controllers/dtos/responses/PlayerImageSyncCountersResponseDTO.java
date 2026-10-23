package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;

/** Contabilidad del run: conflictos son subconjunto de failed, no un total adicional. */
@Schema(description = "Contadores separados de cada resultado y omisión")
public record PlayerImageSyncCountersResponseDTO(
    int evaluated,
    int processed,
    int found,
    int notFound,
    int retryableErrors,
    int failed,
    int conflicts,
    int skippedFound,
    int skippedRetryWindow,
    int skippedFailed,
    int interrupted) {}
