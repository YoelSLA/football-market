package footballmarket.features.dashboard.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record DashboardRecentActivityDTO(
    @Schema(
            description = "Identificador de la transacción.",
            example = "512",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long transactionId,
    @Schema(
            description = "Tipo de operación: BUY o SELL.",
            example = "BUY",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String type,
    @Schema(
            description = "Mercado de origen: OFFICIAL o P2P.",
            example = "OFFICIAL",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String marketType,
    @Schema(
            description = "Identificador del futbolista.",
            example = "101",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long playerId,
    @Schema(
            description = "Nombre del futbolista.",
            example = "Kylian Mbappé",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String playerName,
    @Schema(
            description = "Cantidad de tokens transaccionados.",
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int quantity,
    @Schema(
            description = "Precio unitario de ejecución en centésimas.",
            example = "24500",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long unitPrice,
    @Schema(
            description = "Importe total en centésimas de crédito.",
            example = "49000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalAmount,
    @Schema(
            description = "Fecha y hora de ejecución.",
            example = "2026-11-20T18:15:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime executedAt) {}
