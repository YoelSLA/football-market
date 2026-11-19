package footballmarket.features.market.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Contenedor paginado del catálogo del Mercado P2P. */
@Schema(
    name = "P2PMarketPageResponse",
    description = "Respuesta paginada del catálogo de la comunidad en el Mercado P2P")
public record P2PMarketPageResponseDTO(
    @Schema(description = "Jugadores con ofertas activas de la página actual")
        List<P2PMarketPlayerResponseDTO> content,
    @Schema(description = "Índice de página actual (base 0)", example = "0") int page,
    @Schema(description = "Tamaño de página solicitado o efectivo", example = "8") int size,
    @Schema(description = "Total de jugadores que coinciden con todos los filtros", example = "15")
        long totalElements,
    @Schema(description = "Total de páginas disponibles", example = "2") int totalPages,
    @Schema(description = "Extremos globales de precios en el mercado P2P")
        P2PMarketPriceRangeResponseDTO priceRange) {}
