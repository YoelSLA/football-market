package footballmarket.controllers.exceptions;

import footballmarket.exceptions.ApplicationException;

/** Identificador HTTP que no representa un entero positivo de jugador. */
public class InvalidPlayerIdException extends ApplicationException {
  public InvalidPlayerIdException() {
    super("El identificador del jugador debe ser un entero positivo");
  }
}
