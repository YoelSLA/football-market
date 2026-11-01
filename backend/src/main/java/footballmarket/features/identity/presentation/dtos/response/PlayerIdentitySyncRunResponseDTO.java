package footballmarket.features.identity.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resultado único de ambos pases; cada jugador local pertenece a una sola categoría final. */
@Schema(
    description =
        "Resultado global: selectedPlayers = referencedPlayers + noMatchPlayers + unresolvedPlayers")
public record PlayerIdentitySyncRunResponseDTO(
    @Schema(
            description = "Finalización del trabajo, independiente de la cobertura",
            allowableValues = {"COMPLETED", "PARTIAL", "FAILED"})
        String status,
    @Schema(description = "Jugadores locales seleccionados al iniciar", minimum = "0")
        long selectedPlayers,
    @Schema(
            description = "Jugadores con referencia válida preexistente o conseguida",
            minimum = "0")
        long referencedPlayers,
    @Schema(description = "Jugadores sin coincidencia al finalizar", minimum = "0")
        long noMatchPlayers,
    @Schema(description = "Jugadores ambiguos, en conflicto o con errores", minimum = "0")
        long unresolvedPlayers) {}
