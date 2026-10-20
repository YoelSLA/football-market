package footballmarket.controllers.exceptions;

import footballmarket.exceptions.ApplicationException;

/** Rechazo estructural de entrada no admitida por la evaluación. */
public class InvalidValuationRequestException extends ApplicationException {
  public InvalidValuationRequestException() {
    super("Esta operación no admite parámetros ni cuerpo");
  }
}
