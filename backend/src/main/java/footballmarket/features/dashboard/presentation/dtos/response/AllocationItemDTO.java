package footballmarket.features.dashboard.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record AllocationItemDTO(
    @Schema(
            description = "Clave de categoría (ej: DELANTERO, PREMIER_LEAGUE).",
            example = "DELANTERO",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String categoryKey,
    @Schema(
            description = "Etiqueta legible para la UI.",
            example = "Delanteros",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String label,
    @Schema(
            description = "Valor total de la categoría en centésimas de crédito.",
            example = "720000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalValue,
    @Schema(
            description = "Porcentaje de peso sobre la cartera.",
            example = "56.03",
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal percentage,
    @Schema(
            description = "Cantidad de tokens de esta categoría.",
            example = "40",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int tokenCount) {}
