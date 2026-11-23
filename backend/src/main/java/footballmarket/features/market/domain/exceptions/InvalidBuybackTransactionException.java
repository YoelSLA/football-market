package footballmarket.features.market.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que un registro de venta oficial no cumple las invariantes de su auditoría. */
public class InvalidBuybackTransactionException extends DomainException {

  /**
   * Crea el rechazo con un motivo legible.
   *
   * @param message motivo del rechazo
   */
  public InvalidBuybackTransactionException(String message) {
    super(message);
  }
}
