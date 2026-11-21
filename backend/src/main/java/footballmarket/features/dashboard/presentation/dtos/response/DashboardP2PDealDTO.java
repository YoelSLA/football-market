package footballmarket.features.dashboard.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record DashboardP2PDealDTO(
    @Schema(
            description = "Identificador de la publicación en token_listings.",
            example = "84",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long listingId,
    @Schema(
            description = "Identificador del jugador.",
            example = "101",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long playerId,
    @Schema(
            description = "Nombre completo del jugador.",
            example = "Kylian Mbappé",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String playerName,
    @Schema(
            description = "Nombre del equipo.",
            example = "Real Madrid",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String teamName,
    @Schema(
            description = "Nombre de la liga.",
            example = "La Liga",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String leagueName,
    @Schema(
            description = "URL de la imagen del jugador.",
            example = "https://media.footballmarket.com/players/mbappe.png",
            nullable = true)
        String imageUrl,
    @Schema(
            description = "Tokens disponibles en el lote de la oferta.",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int availableTokens,
    @Schema(
            description = "Precio unitario ofertado en P2P en centésimas.",
            example = "22000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long p2pUnitPrice,
    @Schema(
            description = "Cotización oficial vigente en centésimas.",
            example = "24500",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long officialQuotePrice,
    @Schema(
            description = "Porcentaje de descuento respecto a la cotización oficial.",
            example = "10.20",
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal discountPercentage) {}
