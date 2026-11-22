package footballmarket.features.dashboard.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record DashboardAllocationDTO(
    @Schema(
            description = "Distribución de cartera agrupada por posición táctica.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<AllocationItemDTO> byPosition,
    @Schema(
            description = "Distribución de cartera agrupada por liga/competición.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<AllocationItemDTO> byLeague,
    @Schema(
            description = "Porcentaje de concentración de la posición de mayor valor.",
            example = "35.20",
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal topHoldingConcentrationPercentage,
    @Schema(
            description = "Nombre del futbolista con mayor concentración en cartera.",
            example = "Erling Haaland",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String topHoldingPlayerName) {}
