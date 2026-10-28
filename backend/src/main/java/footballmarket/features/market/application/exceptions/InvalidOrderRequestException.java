package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Intención de compra incompleta sin identificador de publicación. */
public class InvalidOrderRequestException extends ApplicationException {
  public InvalidOrderRequestException() {
    super("La solicitud de compra está incompleta.");
  }
}
