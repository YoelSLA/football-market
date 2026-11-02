package footballmarket.features.catalog.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que los datos requeridos para construir o actualizar un jugador son inválidos. */
public class InvalidPlayerException extends DomainException {

  /**
   * Crea el error con la causa de validación correspondiente.
   *
   * @param message mensaje que describe la validación incumplida
   */
  public InvalidPlayerException(String message) {
    super(message);
  }
}
