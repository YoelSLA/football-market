package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Rechazo indistinguible para cierre, vencimiento o falta de cuenta receptora. */
public class ListingNotAvailableException extends ApplicationException {
  public ListingNotAvailableException() {
    super("La publicación no está disponible.");
  }
}
