package footballmarket.services.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que el superusuario está autenticado e intenta comprar tokens. */
public class SuperuserCannotBuyException extends DomainException {
  public SuperuserCannotBuyException() {
    super("El superusuario no puede comprar tokens");
  }
}
