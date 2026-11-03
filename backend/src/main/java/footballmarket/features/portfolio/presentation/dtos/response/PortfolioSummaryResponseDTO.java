package footballmarket.features.portfolio.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Indicadores globales del portfolio.
 *
 * <p>Los importes son enteros en centésimas de crédito. Los indicadores que dependen de la
 * cotización son nulos cuando alguna posición abierta carece de cotización vigente, y nunca se
 * sustituyen por cero.
 */
public record PortfolioSummaryResponseDTO(
    @Schema(
            description = "Cantidad global de posiciones abiertas del portfolio.",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int totalPositions,
    @Schema(
            description = "Tokens abiertos poseídos en todas las posiciones.",
            example = "120",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int totalTokens,
    @Schema(
            description = "Tokens reservados por publicaciones activas y vigentes.",
            example = "20",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int totalReservedTokens,
    @Schema(
            description = "Tokens disponibles: poseídos menos reservados.",
            example = "100",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int totalAvailableTokens,
    @Schema(
            description =
                "Costo de adquisición de las posiciones abiertas, en centésimas de crédito.",
            example = "12000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalInvested,
    @Schema(
            description =
                "Valor actual total del portfolio, en centésimas de crédito; nulo si alguna posición abierta carece de cotización.",
            example = "13500",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long totalCurrentValue,
    @Schema(
            description =
                "Resultado realizado acumulado, incluidos los ciclos cerrados, en centésimas de crédito.",
            example = "500",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalRealizedProfitLoss,
    @Schema(
            description =
                "Resultado no realizado total, en centésimas de crédito; nulo si alguna posición abierta carece de cotización.",
            example = "1000",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long totalUnrealizedProfitLoss,
    @Schema(
            description =
                "Resultado total del portfolio, en centésimas de crédito; nulo si alguna posición abierta carece de cotización.",
            example = "1500",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long totalProfitLoss,
    @Schema(
            description =
                "Resultado total sobre el costo histórico total pagado, en puntos porcentuales con dos decimales; nulo si el denominador es cero o falta una cotización.",
            example = "12.50",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalProfitLossPercentage,
    @Schema(
            description =
                "Operaciones confirmadas en las que el usuario es comprador, en todas sus posiciones.",
            example = "4",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalBuyTransactions,
    @Schema(
            description =
                "Operaciones confirmadas en las que el usuario es vendedor, en todas sus posiciones.",
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalSellTransactions,
    @Schema(
            description = "Ausencia de posiciones abiertas sin cotización vigente.",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean valuationComplete) {}
