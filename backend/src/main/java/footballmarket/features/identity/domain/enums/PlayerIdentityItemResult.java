package footballmarket.features.identity.domain.enums;

/** Resultados mutuamente excluyentes por jugador seleccionado. */
public enum PlayerIdentityItemResult {
  PENDING,
  RESOLVED,
  ALREADY_REFERENCED,
  CONFLICT,
  NO_MATCH,
  AMBIGUOUS,
  LOCAL_DATA_ERROR,
  DISCOVERY_ERROR,
  PERSISTENCE_ERROR,
  INTERRUPTED
}
