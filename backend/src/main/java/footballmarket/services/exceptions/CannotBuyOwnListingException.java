package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

/** Rechazo de una compra de publicación propia. */
public class CannotBuyOwnListingException extends ApplicationException {
  public CannotBuyOwnListingException() {
    super("No podés comprar una publicación propia.");
  }
}
