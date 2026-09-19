package footballmarket.controllers.exceptions;

public class InvalidPlayerPageException extends RuntimeException {
  public InvalidPlayerPageException() {
    super("La página debe ser mayor o igual a 0 y el tamaño debe estar entre 1 y 100");
  }
}
