package footballmarket.models.exceptions;

import footballmarket.exceptions.FootballMarketException;

public class EmailEmptyException extends FootballMarketException {
  public EmailEmptyException(String message) {
    super(message);
  }
}
