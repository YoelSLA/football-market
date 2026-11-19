package footballmarket.features.market.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Extremos globales de precios del Mercado Oficial, en centésimas de crédito. */
@Schema(description = "Extremos globales de las cotizaciones vigentes elegibles")
public record OfficialMarketPriceRangeResponseDTO(
    @Schema(description = "Precio mínimo global en centésimas; null si no hay cotizaciones.")
        Long min,
    @Schema(description = "Precio máximo global en centésimas; null si no hay cotizaciones.")
        Long max) {}
