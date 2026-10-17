package footballmarket.controllers.dtos.requests;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Deserializador que rechaza fracciones en un campo numérico entero.
 *
 * <p>Por defecto la biblioteca JSON convierte {@code 1.5} en {@code 1} de forma silenciosa. Eso
 * contradice el requisito funcional que exige una cantidad entera sin redondeo ni truncamiento, y
 * convertiría una entrada inválida en una compra legítima. Por eso el campo se deserializa de forma
 * estricta y el valor no entero se rechaza como error estructural, con respuesta 400.
 *
 * <p>Solo se acepta un entero JSON dentro del rango representable: una cadena numérica, un
 * booleano, una fracción o un entero fuera de rango se rechazan en lugar de convertirse.
 *
 * <p>El valor nulo se devuelve sin alterar para que la validación declarativa del DTO siga siendo
 * la responsable de esa entrada.
 */
public class StrictIntegerDeserializer extends ValueDeserializer<Integer> {

  private static final String NON_INTEGER_QUANTITY_MESSAGE =
      "La cantidad debe ser un número entero.";

  /** {@inheritDoc} */
  @Override
  public Integer deserialize(JsonParser parser, DeserializationContext context)
      throws JacksonException {
    JsonNode node = context.readTree(parser);

    if (node == null || node.isNull()) {
      return null;
    }

    if (!node.isIntegralNumber() || !node.canConvertToInt()) {
      throw InvalidFormatException.from(parser, NON_INTEGER_QUANTITY_MESSAGE, node, Integer.class);
    }

    return node.intValue();
  }
}
