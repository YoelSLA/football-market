package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;

public record PlayerSyncResponseDTO(
    @Schema(
            description = "Registros leídos antes de validar y consolidar",
            example = "500",
            minimum = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int obtained,
    @Schema(
            description = "Identificadores nuevos incorporados",
            example = "10",
            minimum = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int created,
    @Schema(
            description = "Identificadores existentes procesados, incluidas reactivaciones",
            example = "475",
            minimum = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int updated,
    @Schema(
            description = "Transiciones efectivas de activo a inactivo",
            example = "3",
            minimum = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int markedInactive,
    @Schema(
            description = "Registros descartados por campos obligatorios ausentes",
            example = "2",
            minimum = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int discardedInvalid) {}
