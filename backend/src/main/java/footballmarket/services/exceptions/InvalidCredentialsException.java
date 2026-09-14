package footballmarket.services.exceptions;

public class InvalidCredentialsException extends FootballMarketException {

  public InvalidCredentialsException() {
    super("Invalid credentials");
  }
}
