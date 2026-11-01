package footballmarket.features.identity.domain.records;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.identity.domain.enums.PlayerIdentityItemResult;
import footballmarket.features.identity.domain.enums.PlayerIdentityRunStatus;
import java.util.Collection;

/** Resultado final de jugadores locales, sin duplicarlos entre los dos pases. */
public record PlayerIdentitySyncSummary(
    PlayerIdentityRunStatus status,
    long selectedPlayers,
    long referencedPlayers,
    long noMatchPlayers,
    long unresolvedPlayers) {
  public PlayerIdentitySyncSummary {
    if (status == null
        || selectedPlayers < 0
        || referencedPlayers < 0
        || noMatchPlayers < 0
        || unresolvedPlayers < 0
        || referencedPlayers > selectedPlayers
        || noMatchPlayers > selectedPlayers - referencedPlayers
        || unresolvedPlayers != selectedPlayers - referencedPlayers - noMatchPlayers) {
      throw new InvalidPlayerException("Los totales de jugadores no reconcilian");
    }
  }

  public static PlayerIdentitySyncSummary from(
      PlayerIdentityRunStatus status, Collection<PlayerIdentityItemResult> results) {
    long referenced =
        results.stream()
            .filter(
                result ->
                    result == PlayerIdentityItemResult.RESOLVED
                        || result == PlayerIdentityItemResult.ALREADY_REFERENCED)
            .count();
    long noMatch =
        results.stream().filter(result -> result == PlayerIdentityItemResult.NO_MATCH).count();
    return new PlayerIdentitySyncSummary(
        status, results.size(), referenced, noMatch, results.size() - referenced - noMatch);
  }
}
