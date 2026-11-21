package footballmarket.features.dashboard.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record DashboardFinancialSummaryDTO(
    @Schema(
            description = "Créditos disponibles en centésimas.",
            example = "450000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long availableCredits,
    @Schema(
            description = "Valor total actual del portfolio en centésimas de crédito.",
            example = "1285000",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long totalPortfolioValue,
    @Schema(
            description = "Patrimonio neto total en centésimas de crédito.",
            example = "1735000",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long netWorth,
    @Schema(
            description = "Costo de adquisición de posiciones abiertas en centésimas.",
            example = "1050000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalInvested,
    @Schema(
            description = "Resultado no realizado acumulado en centésimas.",
            example = "235000",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long totalUnrealizedProfitLoss,
    @Schema(
            description = "Resultado realizado acumulado en centésimas.",
            example = "50000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalRealizedProfitLoss,
    @Schema(
            description = "Resultado total acumulado en centésimas.",
            example = "285000",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long totalProfitLoss,
    @Schema(
            description = "Rentabilidad total porcentual con dos decimales.",
            example = "27.14",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalProfitLossPercentage,
    @Schema(
            description = "Cantidad total de tokens poseídos en cartera.",
            example = "85",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int totalTokens,
    @Schema(
            description = "Tokens disponibles para operar.",
            example = "70",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int availableTokens,
    @Schema(
            description = "Tokens comprometidos en órdenes de venta P2P activas.",
            example = "15",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int reservedTokens,
    @Schema(
            description =
                "Indica si todas las posiciones abiertas tienen cotización oficial vigente.",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean valuationComplete) {}
