package footballmarket.controllers.dtos.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Solicitud de compra de tokens de un jugador.
 *
 * @param playerId identificador interno del jugador que se quiere comprar
 * @param quantity cantidad de tokens a comprar, entero entre 1 y 100
 * @param listingId publicación completa; cuando se informa, selecciona la compra secundaria
 */
@JsonDeserialize(using = OrderBuyRequestDeserializer.class)
public record OrderBuyRequestDTO(
    @Schema(
            description =
                "Identificador interno del jugador. Debe existir y estar visible; un valor no"
                    + " positivo responde 404 PLAYER_NOT_FOUND.",
            example = "7821")
        Long playerId,
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
        @Schema(
            description =
                "Cantidad de tokens a comprar. Entero entre 1 y 100. Una fracción se rechaza con"
                    + " 400 INVALID_REQUEST: no se redondea ni se trunca.",
            example = "10",
            minimum = "1",
            maximum = "100")
        Integer quantity,
    @JsonDeserialize(using = StrictLongDeserializer.class)
        @Schema(
            description =
                "Publicación completa a comprar; si se informa, se ignoran playerId y quantity. Entero Long estricto.",
            example = "25")
        Long listingId) {
  /** Mantiene la construcción de solicitudes directas por consumidores Java existentes. */
  public OrderBuyRequestDTO(Long playerId, Integer quantity) {
    this(playerId, quantity, null);
  }

  /** Obligatoriedad condicional del contrato, sin validar aquí reglas de negocio. */
  @AssertTrue(message = "Indicá listingId o bien playerId y quantity para la compra directa")
  @JsonIgnore
  public boolean isComplete() {
    return this.listingId != null || (this.playerId != null && this.quantity != null);
  }
}
