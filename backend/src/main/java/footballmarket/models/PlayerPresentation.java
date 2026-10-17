package footballmarket.models;

import footballmarket.models.exceptions.InvalidCatalogEntityException;
import java.text.Normalizer;
import java.util.Collection;
import java.util.Comparator;

/** Selección de un texto original de presentación, separada de la equivalencia semántica. */
public final class PlayerPresentation {
  private static final Comparator<String> QUALITY =
      Comparator.comparingInt(PlayerPresentation::accentedLetters)
          .reversed()
          .thenComparing(Comparator.comparingInt(PlayerPresentation::naturalCasing).reversed())
          .thenComparing(Comparator.naturalOrder());

  private PlayerPresentation() {}

  /** Compara usando exclusivamente la normalización estricta existente, sin aliases nuevos. */
  public static boolean equivalent(String first, String second) {
    return TeamNameNormalizer.normalize(first).equals(TeamNameNormalizer.normalize(second));
  }

  /**
   * Devuelve una representación recibida; las contradicciones no se resuelven mediante ranking.
   * Prioriza letras con diacríticos, casing mixto y orden lexicográfico del texto original.
   */
  public static String canonical(Collection<String> observations) {
    if (observations == null || observations.isEmpty()) {
      throw new InvalidCatalogEntityException(
          "La presentación requiere observaciones equivalentes");
    }
    String semantic = null;
    for (String text : observations) {
      if (text == null || text.isBlank()) {
        throw new InvalidCatalogEntityException("La presentación requiere texto válido");
      }
      String normalized = TeamNameNormalizer.normalize(text);
      if (semantic != null && !semantic.equals(normalized)) {
        throw new InvalidCatalogEntityException(
            "Las observaciones de presentación son contradictorias");
      }
      semantic = normalized;
    }
    return observations.stream().min(QUALITY).orElseThrow();
  }

  private static int accentedLetters(String text) {
    String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
    int count = 0;
    boolean letter = false;
    boolean counted = false;
    for (int point : decomposed.codePoints().toArray()) {
      int type = Character.getType(point);
      if (type == Character.NON_SPACING_MARK
          || type == Character.COMBINING_SPACING_MARK
          || type == Character.ENCLOSING_MARK) {
        if (letter && !counted) {
          count++;
          counted = true;
        }
      } else {
        letter = Character.isLetter(point);
        counted = false;
      }
    }
    return count;
  }

  private static int naturalCasing(String text) {
    return text.codePoints().anyMatch(Character::isUpperCase)
            && text.codePoints().anyMatch(Character::isLowerCase)
        ? 1
        : 0;
  }
}
