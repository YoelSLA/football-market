package footballmarket.features.market.presentation.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Solicitud estructural de publicación; la disponibilidad y el rango pertenecen al caso de uso. */
public record OrderSellRequestDTO(
    @NotNull(message = "El identificador del jugador es obligatorio")
        @JsonDeserialize(using = StrictLongDeserializer.class)
        @Schema(description = "Jugador visible del que se ofrecen tokens.", example = "7821")
        Long playerId,
    @NotNull(message = "La cantidad es obligatoria")
        @JsonDeserialize(using = StrictIntegerDeserializer.class)
        @Schema(
            description = "Cantidad entera de tokens a reservar, entre 1 y 100.",
            minimum = "1",
            maximum = "100",
            example = "5")
        Integer quantity,
    @JsonDeserialize(using = StrictLongDeserializer.class)
        @Schema(
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            description =
                "Precio unitario opcional en centésimas. Si se omite, se aplica la cotización"
                    + " vigente. El precio informado debe estar entre el 80% y el 120% de esa"
                    + " cotización, inclusive.",
            minimum = "1",
            example = "85")
        Long unitPrice) {}
