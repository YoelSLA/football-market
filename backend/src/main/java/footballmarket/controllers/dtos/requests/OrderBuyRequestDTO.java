package footballmarket.controllers.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Solicitud de compra de tokens de un jugador.
 *
 * @param playerId identificador interno del jugador que se quiere comprar
 * @param quantity cantidad de tokens a comprar, entero entre 1 y 100
 */
public record OrderBuyRequestDTO(
    @NotNull(message = "El identificador del jugador es obligatorio")
        @Schema(
            description =
                "Identificador interno del jugador. Debe existir y estar visible; un valor no"
                    + " positivo responde 404 PLAYER_NOT_FOUND.",
            example = "7821")
        Long playerId,
    @NotNull(message = "La cantidad es obligatoria")
        @JsonDeserialize(using = StrictIntegerDeserializer.class)
        @Schema(
            description =
                "Cantidad de tokens a comprar. Entero entre 1 y 100. Una fracción se rechaza con"
                    + " 400 INVALID_REQUEST: no se redondea ni se trunca.",
            example = "10",
            minimum = "1",
            maximum = "100")
        Integer quantity) {}
