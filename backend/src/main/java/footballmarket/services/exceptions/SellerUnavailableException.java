package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

/**
 * Indica que el superusuario vendedor no existe o no es identificable.
 *
 * <p>Es una garantía de integridad transaccional: la compra falla antes de mover recursos para no
 * dejar una compra parcial. La respuesta pública es un 500 genérico.
 */
public class SellerUnavailableException extends ApplicationException {
  public SellerUnavailableException() {
    super("No hay un vendedor identificable para completar la compra.");
  }
}
