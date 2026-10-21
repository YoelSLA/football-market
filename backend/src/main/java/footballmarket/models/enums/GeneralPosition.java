package footballmarket.models.enums;

import footballmarket.models.exceptions.InvalidPlayerException;

/** Posición general cerrada utilizada para normalizar y ponderar. */
public enum GeneralPosition {
  GOALKEEPER,
  DEFENDER,
  MIDFIELDER,
  FORWARD;

  /**
   * Traduce la posición persistida por el catálogo de jugadores.
   *
   * <p>El catálogo persiste las denominaciones del proveedor ({@code Goalkeeper}, {@code Defence},
   * {@code Midfield}, {@code Offence}) y se aceptan también sus equivalentes en inglés ({@code
   * Defender}, {@code Midfielder}, {@code Forward}). Ambos conjuntos describen las mismas cuatro
   * posiciones generales. Cualquier otro valor es un fallo individual controlado.
   */
  public static GeneralPosition fromPlayerPosition(String position) {
    if (position == null) {
      throw new InvalidPlayerException("La posición del jugador es obligatoria");
    }
    return switch (position) {
      case "Goalkeeper" -> GOALKEEPER;
      case "Defence", "Defender" -> DEFENDER;
      case "Midfield", "Midfielder" -> MIDFIELDER;
      case "Offence", "Forward" -> FORWARD;
      default -> throw new InvalidPlayerException("La posición del jugador no es reconocida");
    };
  }
}
