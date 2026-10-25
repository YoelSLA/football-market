package footballmarket.controllers.dtos.responses;

import footballmarket.models.records.OperationHistoryEntry.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** Operación financiera confirmada del historial personal. */
public record OperationHistoryResponseDTO(
    @Schema(
            description = "Identificador persistido de la transacción.",
            example = "92",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long transactionId,
    @Schema(
            description = "Tipo de operación desde la perspectiva del usuario: BUY o SELL.",
            example = "SELL",
            requiredMode = Schema.RequiredMode.REQUIRED)
        OperationType type,
    @Schema(
            description = "Identificador interno del jugador negociado.",
            example = "41",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long playerId,
    @Schema(
            description = "Nombre persistido e inmutable del jugador negociado.",
            example = "Joel Robles",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String playerName,
    @Schema(
            description = "Cantidad de tokens transferidos.",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int quantity,
    @Schema(
            description = "Precio histórico por token, en centésimas de crédito.",
            example = "125",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long unitPrice,
    @Schema(
            description = "Importe histórico total, en centésimas de crédito.",
            example = "625",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalAmount,
    @Schema(
            description = "Instante de confirmación en formato ISO-8601 con offset.",
            example = "2026-10-24T14:30:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime executedAt) {}
