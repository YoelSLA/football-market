package footballmarket.features.market.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que un importe en centésimas no admite la operación solicitada. */
public class InvalidCreditAmountException extends DomainException {
  public InvalidCreditAmountException(String message) {
    super(message);
  }
}
