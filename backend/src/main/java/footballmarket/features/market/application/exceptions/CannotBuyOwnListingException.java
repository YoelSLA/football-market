package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Rechazo de una compra de publicación propia. */
public class CannotBuyOwnListingException extends ApplicationException {
  public CannotBuyOwnListingException() {
    super("No podés comprar una publicación propia.");
  }
}
