package footballmarket.controllers.dtos.requests;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

/** Acepta exclusivamente literales enteros JSON representables como Long, sin coerción. */
public class StrictLongDeserializer extends ValueDeserializer<Long> {
  @Override
  public Long deserialize(JsonParser parser, DeserializationContext context)
      throws JacksonException {
    JsonNode node = context.readTree(parser);
    if (node == null || node.isNull()) {
      return null;
    }
    if (!node.isIntegralNumber() || !node.canConvertToLong()) {
      throw InvalidFormatException.from(
          parser, "El identificador debe ser un número entero.", node, Long.class);
    }
    return node.longValue();
  }
}
