package footballmarket.services.exceptions;

import footballmarket.exceptions.FootballMarketException;

public class UserNotFoundException extends FootballMarketException {
  public UserNotFoundException(String message) {
    super(message);
  }
}
