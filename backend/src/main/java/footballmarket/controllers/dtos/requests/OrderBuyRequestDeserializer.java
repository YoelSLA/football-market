package footballmarket.controllers.dtos.requests;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.InvalidFormatException;

/** Selecciona la modalidad antes de deserializar campos exclusivos de la compra directa. */
public class OrderBuyRequestDeserializer extends ValueDeserializer<OrderBuyRequestDTO> {
  @Override
  public OrderBuyRequestDTO deserialize(JsonParser parser, DeserializationContext context)
      throws JacksonException {
    JsonNode node = context.readTree(parser);
    if (node == null || node.isNull()) {
      return null;
    }
    if (!node.isObject()) {
      throw InvalidFormatException.from(
          parser,
          "La solicitud de compra debe ser un objeto JSON.",
          node,
          OrderBuyRequestDTO.class);
    }
    JsonNode listingId = node.get("listingId");
    if (listingId != null && !listingId.isNull()) {
      if (!listingId.isIntegralNumber() || !listingId.canConvertToLong()) {
        throw InvalidFormatException.from(
            parser, "El identificador debe ser un número entero.", listingId, Long.class);
      }
      return new OrderBuyRequestDTO(null, null, listingId.longValue());
    }
    // Conserva el binding Long original y la cantidad estricta de SPEC-010. Un listingId
    // ausente o null no selecciona la modalidad secundaria, como en el contrato vigente.
    DirectPurchase direct = context.readTreeAsValue(node, DirectPurchase.class);
    return new OrderBuyRequestDTO(direct.playerId(), direct.quantity());
  }

  /** Binding directo sin recursión al deserializador de la solicitud de ambas modalidades. */
  private record DirectPurchase(
      Long playerId,
      @JsonDeserialize(using = StrictIntegerDeserializer.class) Integer quantity,
      Long listingId) {}
}
