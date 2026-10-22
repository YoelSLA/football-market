package footballmarket.repositories.projections;

/**
 * Proyección de la cantidad reservada por jugador para un vendedor.
 *
 * <p>Proyección de interfaz de Spring Data: la consulta que la produce agrupa las publicaciones
 * activas y vigentes del vendedor por jugador, sin materializar las publicaciones.
 */
public interface ReservedQuantity {
  /** Identificador interno del jugador sobre el que se reserva. */
  Long getPlayerId();

  /** Tokens reservados en las publicaciones activas y vigentes del vendedor. */
  Long getQuantity();
}
