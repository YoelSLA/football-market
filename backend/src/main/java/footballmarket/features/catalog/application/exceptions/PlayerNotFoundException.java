package footballmarket.features.catalog.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que no existe un jugador visible para el catálogo con el identificador solicitado. */
public class PlayerNotFoundException extends DomainException {
  public PlayerNotFoundException(String message) {
    super(message);
  }
}
