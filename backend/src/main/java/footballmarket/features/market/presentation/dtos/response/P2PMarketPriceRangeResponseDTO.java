package footballmarket.features.market.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Rango de precios extremos disponible en el catálogo del Mercado P2P. */
@Schema(
    name = "P2PMarketPriceRangeResponse",
    description = "Extremos globales de precios unitarios disponibles en el Mercado P2P")
public record P2PMarketPriceRangeResponseDTO(
    @Schema(
            description = "Precio mínimo global en centésimas de crédito o nulo si no hay ofertas",
            example = "1000",
            nullable = true)
        Long min,
    @Schema(
            description = "Precio máximo global en centésimas de crédito o nulo si no hay ofertas",
            example = "50000",
            nullable = true)
        Long max) {}
