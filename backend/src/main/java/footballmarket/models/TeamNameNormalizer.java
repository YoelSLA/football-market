package footballmarket.models;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Normalización estricta de nombres de equipos, sin aliases, sufijos ni matching aproximado. */
public final class TeamNameNormalizer {
  private static final Pattern MARKS = Pattern.compile("\\p{M}+");
  private static final Pattern SPACES = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

  private TeamNameNormalizer() {}

  /**
   * Produce la clave de comparación estricta; la ausencia no representa una identidad.
   *
   * @param name nombre observado, o {@code null}
   * @return nombre sin diacríticos, en mayúsculas y con espacios normalizados; vacío si ausente
   */
  public static String normalize(String name) {
    if (name == null) {
      return "";
    }
    String withoutMarks =
        MARKS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("");
    return SPACES.matcher(withoutMarks).replaceAll(" ").strip().toUpperCase(Locale.ROOT);
  }
}
