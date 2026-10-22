package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;

/** Metadatos de la página solicitada. */
public record PortfolioPaginationResponseDTO(
    @Schema(
            description = "Número de página solicitada, desde cero.",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int page,
    @Schema(
            description = "Tamaño de página solicitado.",
            example = "20",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int size,
    @Schema(
            description = "Cantidad de posiciones del conjunto filtrado.",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalElements,
    @Schema(
            description = "Cantidad de páginas del conjunto filtrado.",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int totalPages) {}
