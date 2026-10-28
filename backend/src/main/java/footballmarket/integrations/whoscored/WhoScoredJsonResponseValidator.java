package footballmarket.integrations.whoscored;

import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsStructureChangedException;
import tools.jackson.databind.JsonNode;

/**
 * Valida la forma del cuerpo JSON del proveedor antes de interpretarlo.
 *
 * <p>El MIME declarado se ignora a propósito: el proveedor publica JSON con {@code text/html} y lo
 * que decide es que el cuerpo sea JSON coherente. Cualquier ausencia de las secciones obligatorias
 * se clasifica como estructura cambiada y nunca como dato ausente.
 */
public final class WhoScoredJsonResponseValidator {
  private WhoScoredJsonResponseValidator() {}

  /** Exige objeto raíz, {@code playerTableStats} y {@code statColumns} coherentes. */
  public static JsonNode validate(JsonNode root) {
    if (root == null
        || !root.isObject()
        || !root.path("playerTableStats").isArray()
        || !root.path("statColumns").isArray()
        || root.path("statColumns").isEmpty()) {
      throw new WhoScoredStatisticsStructureChangedException();
    }
    JsonNode rows = root.path("playerTableStats");
    for (JsonNode row : rows) {
      if (!row.isObject()) {
        throw new WhoScoredStatisticsStructureChangedException();
      }
    }
    for (JsonNode column : root.path("statColumns")) {
      if (declaredField(column) == null) {
        throw new WhoScoredStatisticsStructureChangedException();
      }
    }
    return root;
  }

  /**
   * Nombre técnico de la columna declarada; se usa para diagnosticar, no para crear métricas.
   *
   * <p>La forma observada del proveedor es un array de cadenas y es la que se acepta. La forma de
   * objeto con {@code name}, {@code key} o {@code title} también se admite, para que un cambio de
   * presentación del proveedor no se confunda con un cambio de estructura.
   */
  public static String declaredColumn(JsonNode column) {
    if (column == null || (!column.isObject() && !column.isString())) {
      throw new WhoScoredStatisticsStructureChangedException();
    }
    String name = declaredField(column);
    if (name == null) {
      throw new WhoScoredStatisticsStructureChangedException();
    }
    return name;
  }

  private static String declaredField(JsonNode column) {
    if (column.isString()) {
      return column.stringValue().isBlank() ? null : column.stringValue();
    }
    if (!column.isObject()) {
      return null;
    }
    for (String field : new String[] {"name", "key", "title"}) {
      JsonNode value = column.path(field);
      if (value.isString() && !value.stringValue().isBlank()) {
        return value.stringValue();
      }
    }
    return null;
  }
}
