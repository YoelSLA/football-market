package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que una fila de cotización se quiere construir con invariantes de origen incumplidas. */
public class InvalidQuoteException extends DomainException {

  /**
   * Crea el error con la causa de validación correspondiente.
   *
   * @param message mensaje que describe la validación incumplida
   */
  public InvalidQuoteException(String message) {
    super(message);
  }
}
