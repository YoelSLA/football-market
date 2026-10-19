package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

/** Prohíbe al superusuario publicar en el mercado secundario. */
public class SuperuserCannotSellException extends ApplicationException {
  public SuperuserCannotSellException() {
    super("El superusuario no puede publicar tokens para la venta.");
  }
}
