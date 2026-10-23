package footballmarket.services.exceptions;

import footballmarket.exceptions.DomainException;

/**
 * Indica que el comprador no tiene créditos suficientes para completar la compra.
 *
 * <p>El mensaje no revela el saldo disponible ni el importe requerido.
 */
public class InsufficientCreditsException extends DomainException {
  public InsufficientCreditsException() {
    super("No tenés créditos suficientes para completar la compra.");
  }
}
