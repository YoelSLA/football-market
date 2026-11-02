package footballmarket.features.market.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que el saldo de créditos disponible no cubre el importe requerido. */
public class InsufficientUserCreditsException extends DomainException {
  public InsufficientUserCreditsException() {
    super("El comprador no tiene créditos suficientes para completar la compra.");
  }
}
