package footballmarket.models.exceptions;

import footballmarket.exceptions.FootballMarketException;

public class PasswordTooShortException extends FootballMarketException {
  public PasswordTooShortException(String message) {
    super(message);
  }
}
