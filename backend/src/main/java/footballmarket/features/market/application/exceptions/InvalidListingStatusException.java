package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** El filtro no coincide exactamente con uno de los cuatro estados efectivos. */
public class InvalidListingStatusException extends ApplicationException {
  public InvalidListingStatusException() {
    super("El parámetro de consulta no tiene un formato válido");
  }
}
