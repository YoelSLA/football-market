package footballmarket.features.dashboard.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

public record DashboardOverviewResponseDTO(
    @Schema(
            description = "Resumen de posición financiera y patrimonio del usuario.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        DashboardFinancialSummaryDTO financialSummary,
    @Schema(
            description = "Distribución y asignación de activos en cartera.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        DashboardAllocationDTO allocation,
    @Schema(
            description = "Métricas agregadas macro del mercado.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        DashboardMarketPulseDTO marketPulse,
    @Schema(
            description =
                "Los 5 jugadores con mayor apreciación porcentual en el último recálculo.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<DashboardPlayerMoverDTO> topGainers,
    @Schema(
            description = "Los 5 jugadores con mayor corrección porcentual en el último recálculo.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<DashboardPlayerMoverDTO> topLosers,
    @Schema(
            description = "Oportunidades destacadas de valor según ratio score/precio.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<DashboardValuePickDTO> valuePicks,
    @Schema(
            description =
                "Ofertas activas de arbitraje P2P con descuento respecto a la cotización oficial.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<DashboardP2PDealDTO> bestP2PDeals,
    @Schema(
            description = "Últimas transacciones confirmadas del usuario.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<DashboardRecentActivityDTO> recentActivity,
    @Schema(
            description = "Instante de generación del resumen.",
            example = "2026-11-20T20:15:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime generatedAt) {}
