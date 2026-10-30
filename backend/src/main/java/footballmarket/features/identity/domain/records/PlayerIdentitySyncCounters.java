package footballmarket.features.identity.domain.records;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.identity.domain.enums.PlayerIdentityItemResult;
import java.util.Map;

/** Conteos reconciliados; jugadores, identidades, ligas y filas son unidades separadas. */
public record PlayerIdentitySyncCounters(
    long selectedPlayers,
    long discoveredUnique,
    long resolved,
    long alreadyReferenced,
    long conflicts,
    long noMatch,
    long ambiguous,
    long localDataErrors,
    long discoveryErrors,
    long persistenceErrors,
    long interruptedPlayers,
    long pendingPlayers,
    long providerErrors,
    long discardedRows) {
  public PlayerIdentitySyncCounters {
    if (selectedPlayers < 0
        || discoveredUnique < 0
        || resolved < 0
        || alreadyReferenced < 0
        || conflicts < 0
        || noMatch < 0
        || ambiguous < 0
        || localDataErrors < 0
        || discoveryErrors < 0
        || persistenceErrors < 0
        || interruptedPlayers < 0
        || pendingPlayers < 0
        || providerErrors < 0
        || discardedRows < 0
        || selectedPlayers
            != resolved
                + alreadyReferenced
                + conflicts
                + noMatch
                + ambiguous
                + localDataErrors
                + discoveryErrors
                + persistenceErrors
                + interruptedPlayers) {
      throw new InvalidPlayerException("Los contadores de identidad no reconcilian");
    }
  }

  public long technicalErrors() {
    return this.discoveryErrors + this.persistenceErrors + this.interruptedPlayers;
  }

  public long finalNonTechnicalPlayers() {
    return this.resolved
        + this.alreadyReferenced
        + this.conflicts
        + this.noMatch
        + this.ambiguous
        + this.localDataErrors;
  }

  /**
   * Convierte agregados finales de una ejecución; el pendiente derivado no cierra una ejecución.
   */
  public static PlayerIdentitySyncCounters from(
      Map<PlayerIdentityItemResult, Long> results,
      long discovered,
      long providerErrors,
      long discarded) {
    if (results.getOrDefault(PlayerIdentityItemResult.PENDING, 0L) > 0) {
      throw new InvalidPlayerException("Una ejecución no se cierra con jugadores pendientes");
    }
    long selected = results.values().stream().mapToLong(Long::longValue).sum();
    return new PlayerIdentitySyncCounters(
        selected,
        discovered,
        results.getOrDefault(PlayerIdentityItemResult.RESOLVED, 0L),
        results.getOrDefault(PlayerIdentityItemResult.ALREADY_REFERENCED, 0L),
        results.getOrDefault(PlayerIdentityItemResult.CONFLICT, 0L),
        results.getOrDefault(PlayerIdentityItemResult.NO_MATCH, 0L),
        results.getOrDefault(PlayerIdentityItemResult.AMBIGUOUS, 0L),
        results.getOrDefault(PlayerIdentityItemResult.LOCAL_DATA_ERROR, 0L),
        results.getOrDefault(PlayerIdentityItemResult.DISCOVERY_ERROR, 0L),
        results.getOrDefault(PlayerIdentityItemResult.PERSISTENCE_ERROR, 0L),
        results.getOrDefault(PlayerIdentityItemResult.INTERRUPTED, 0L),
        results.getOrDefault(PlayerIdentityItemResult.PENDING, 0L),
        providerErrors,
        discarded);
  }
}
