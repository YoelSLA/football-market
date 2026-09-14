package footballmarket.models.exceptions;

import footballmarket.exceptions.FootballMarketException;

public class EmailInvalidException extends FootballMarketException {
  public EmailInvalidException(String message) {
    super(message);
  }
}
