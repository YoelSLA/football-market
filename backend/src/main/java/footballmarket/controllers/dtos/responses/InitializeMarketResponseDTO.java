package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Respuesta de la inicialización del mercado.
 *
 * @param marketDate fecha de mercado vigente
 * @param initializedPlayers jugadores cotizables con fila inicial vigente
 * @param alreadyInitialized {@code true} cuando el mercado ya estaba inicializado en esa fecha y la
 *     operación no escribió filas
 */
@Schema(
    name = "InitializeMarketResponse",
    description = "Estado del mercado tras la inicialización")
public record InitializeMarketResponseDTO(
    @Schema(description = "Fecha de mercado vigente", example = "2026-10-20") LocalDate marketDate,
    @Schema(description = "Jugadores cotizables con fila inicial vigente", example = "42")
        int initializedPlayers,
    @Schema(
            description =
                "Verdadero cuando el mercado ya estaba inicializado en esa fecha y no se escribieron filas",
            example = "false")
        boolean alreadyInitialized) {}
