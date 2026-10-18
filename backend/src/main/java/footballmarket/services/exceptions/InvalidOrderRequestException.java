package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

/** Intención de compra incompleta sin identificador de publicación. */
public class InvalidOrderRequestException extends ApplicationException {
  public InvalidOrderRequestException() {
    super("La solicitud de compra está incompleta.");
  }
}
