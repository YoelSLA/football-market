package footballmarket.services.exceptions;

import footballmarket.exceptions.DomainException;

/**
 * Indica que el superusuario no tiene tokens suficientes para completar la compra.
 *
 * <p>El mensaje no revela el stock disponible ni ningún campo equivalente.
 */
public class InsufficientStockException extends DomainException {
  public InsufficientStockException() {
    super("No hay suficientes tokens disponibles para completar la compra.");
  }
}
