package footballmarket.features.dashboard.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record DashboardMarketPulseDTO(
    @Schema(
            description = "Capitalización total del mercado en centésimas.",
            example = "154200000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalMarketCap,
    @Schema(
            description = "Volumen transaccionado reciente en centésimas.",
            example = "3250000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long volume24h,
    @Schema(
            description = "Total histórico de transacciones confirmadas en la plataforma.",
            example = "1420",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalTransactionsCount,
    @Schema(
            description = "Total de publicaciones de venta P2P activas en este momento.",
            example = "48",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int activeListingsCount) {}
