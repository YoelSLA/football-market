package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;

public record PlayerResponseDTO(
    @Schema(
            description = "Identificador externo del jugador",
            example = "7821",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,
    @Schema(
            description = "Nombre del jugador",
            example = "Joel Robles",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
    @Schema(
            description = "Equipo actual",
            example = "Real Betis Balompié",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String team,
    @Schema(
            description = "Primera competición en el orden configurado",
            example = "Primera Division",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String league,
    @Schema(
            description = "Posición del jugador",
            example = "Goalkeeper",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String position) {}
