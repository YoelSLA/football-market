package footballmarket.features.catalog.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/**
 * Indica que el jugador existe pero no cumple el predicado vigente de visibilidad.
 *
 * <p>Se distingue de {@link PlayerNotFoundException} porque el jugador sí está persistido: la
 * ausencia de visibilidad no equivale a inexistencia.
 */
public class PlayerNotVisibleException extends DomainException {
  public PlayerNotVisibleException() {
    super("No encontramos el jugador solicitado");
  }
}
