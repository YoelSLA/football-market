package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/**
 * Indica que una transferencia de tokens intenta mover cantidad entre tenencias de jugadores
 * distintos.
 *
 * <p>La suma de tenencias solo se conserva si ambas pertenecen al mismo jugador. La compra nunca
 * produce este caso porque resuelve emisor y receptor del mismo jugador, de modo que la excepción
 * protege el invariante del modelo frente a usos futuros.
 */
public class TokenPlayerMismatchException extends DomainException {
  public TokenPlayerMismatchException() {
    super("Las tenencias deben pertenecer al mismo jugador para transferir tokens.");
  }
}
