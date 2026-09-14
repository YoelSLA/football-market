package footballmarket.services.exceptions;

public class EmailAlreadyRegisteredException extends FootballMarketException {

  public EmailAlreadyRegisteredException() {
    super("Email is already registered");
  }
}
