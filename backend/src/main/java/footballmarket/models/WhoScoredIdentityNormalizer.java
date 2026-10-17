package footballmarket.models;

import java.text.Normalizer;
import java.util.Locale;

/** Pipeline Unicode exacto de SPEC-008 FR-005, sin coincidencias aproximadas. */
public final class WhoScoredIdentityNormalizer {
  private WhoScoredIdentityNormalizer() {}

  public static String normalize(String value) {
    if (value == null) {
      return "";
    }
    String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
    StringBuilder result = new StringBuilder();
    boolean latinBase = false;
    boolean space = false;
    for (int offset = 0; offset < decomposed.length(); ) {
      int point = decomposed.codePointAt(offset);
      offset += Character.charCount(point);
      int type = Character.getType(point);
      boolean mark =
          type == Character.NON_SPACING_MARK
              || type == Character.COMBINING_SPACING_MARK
              || type == Character.ENCLOSING_MARK;
      if (mark && latinBase) {
        continue;
      }
      if (!mark) {
        latinBase =
            Character.isLetter(point)
                && Character.UnicodeScript.of(point) == Character.UnicodeScript.LATIN;
      }
      if (type == Character.DASH_PUNCTUATION || whiteSpace(point)) {
        space = !result.isEmpty();
      } else {
        if (space) {
          result.append(' ');
        }
        result.appendCodePoint(point);
        space = false;
      }
    }
    return result.toString().toLowerCase(Locale.ROOT);
  }

  public static boolean validName(String value) {
    return normalize(value).codePoints().anyMatch(Character::isLetterOrDigit);
  }

  private static boolean whiteSpace(int point) {
    return point >= 0x9 && point <= 0xd
        || point == 0x20
        || point == 0x85
        || point == 0xa0
        || point == 0x1680
        || point >= 0x2000 && point <= 0x200a
        || point == 0x2028
        || point == 0x2029
        || point == 0x202f
        || point == 0x205f
        || point == 0x3000;
  }
}
