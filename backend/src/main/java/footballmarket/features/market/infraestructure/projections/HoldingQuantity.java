package footballmarket.features.market.infraestructure.projections;

/**
 * Proyección de la cantidad poseída por un propietario en cada jugador.
 *
 * <p>Proyección de interfaz de Spring Data: la consulta que la produce proyecta identificador de
 * jugador y cantidad, de modo que no materializa las tenencias ni sus jugadores.
 */
public interface HoldingQuantity {
  /** Identificador interno del jugador de la tenencia. */
  Long getPlayerId();

  /** Tokens poseídos por el propietario en ese jugador. */
  Integer getQuantity();
}
