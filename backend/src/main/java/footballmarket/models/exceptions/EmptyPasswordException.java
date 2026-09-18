package footballmarket.models.exceptions;

import footballmarket.exceptions.FootballMarketException;

public class EmptyPasswordException extends FootballMarketException {
  public EmptyPasswordException(String message) {
    super(message);
  }
}
