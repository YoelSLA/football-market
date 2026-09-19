package footballmarket.integrations.footballdata;

public class FootballDataUnavailableException extends RuntimeException {
  public FootballDataUnavailableException() {
    super("No se pudo completar la lectura del proveedor de jugadores");
  }
}
