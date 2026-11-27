package footballmarket.features.market.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Metadatos de paginación para consultas de mercado. */
public record MarketPaginationResponseDTO(
    @Schema(description = "Número de página solicitada, desde cero.", example = "0") int page,
    @Schema(description = "Tamaño de página solicitado.", example = "10") int size,
    @Schema(description = "Cantidad total de elementos coincidentes.", example = "24")
        long totalElements,
    @Schema(description = "Cantidad total de páginas disponibles.", example = "3")
        int totalPages) {}
