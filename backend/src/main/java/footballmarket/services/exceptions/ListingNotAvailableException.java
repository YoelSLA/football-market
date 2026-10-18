package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

/** Rechazo indistinguible para cierre, vencimiento o falta de cuenta receptora. */
public class ListingNotAvailableException extends ApplicationException {
  public ListingNotAvailableException() {
    super("La publicación no está disponible.");
  }
}
