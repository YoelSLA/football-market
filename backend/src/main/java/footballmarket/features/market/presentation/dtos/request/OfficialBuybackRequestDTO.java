package footballmarket.features.market.presentation.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Solicitud estructural de venta oficial; el rango y la elegibilidad pertenecen al caso de uso. */
public record OfficialBuybackRequestDTO(
    @NotNull(message = "El identificador del jugador es obligatorio")
        @JsonDeserialize(using = StrictLongDeserializer.class)
        @Schema(description = "Jugador visible del que se venden tokens.", example = "7821")
        Long playerId,
    @NotNull(message = "La cantidad es obligatoria")
        @JsonDeserialize(using = StrictIntegerDeserializer.class)
        @Schema(
            description = "Cantidad entera de tokens a vender, entre 1 y 100.",
            minimum = "1",
            maximum = "100",
            example = "5")
        Integer quantity) {}
