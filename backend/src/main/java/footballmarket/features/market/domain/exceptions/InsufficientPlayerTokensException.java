package footballmarket.features.market.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que la tenencia de tokens disponible no cubre la cantidad requerida. */
public class InsufficientPlayerTokensException extends DomainException {
  public InsufficientPlayerTokensException() {
    super("No hay suficientes tokens disponibles para completar la compra.");
  }
}
