package footballmarket.models;

import footballmarket.models.enums.PlayerIdentityItemResult;
import java.util.*;

/** Resuelve todas las identidades antes de escribir; ningún orden determina un propietario. */
public final class WhoScoredIdentityMatcher {
  private final WhoScoredTeamAliases aliases;

  /** Decisión de dominio y evidencia por nombre, sin acceso a persistencia. */
  public record Decision(
      PlayerIdentityItemResult result,
      String reasonCode,
      List<WhoScoredIdentityCandidate> candidates,
      String externalId) {
    public Decision {
      candidates = List.copyOf(candidates);
    }
  }

  public WhoScoredIdentityMatcher(WhoScoredTeamAliases aliases) {
    this.aliases = aliases;
  }

  public Map<Long, Decision> match(
      List<PlayerIdentitySyncItem> items,
      List<WhoScoredIdentityCandidate> candidates,
      Set<Long> completedLeagues) {
    Map<Long, Decision> decisions = new LinkedHashMap<>();
    Map<String, List<Long>> owners = new HashMap<>();
    for (PlayerIdentitySyncItem item : items) {
      Decision decision = this.decide(item, candidates, completedLeagues);
      decisions.put(item.getPlayerId(), decision);
      if (decision.result() == PlayerIdentityItemResult.RESOLVED
          || decision.result() == PlayerIdentityItemResult.AMBIGUOUS) {
        String name = WhoScoredIdentityNormalizer.normalize(item.getPlayerName());
        for (WhoScoredIdentityCandidate candidate : decision.candidates()) {
          if (candidate.getContexts().stream()
              .anyMatch(
                  context ->
                      name.equals(WhoScoredIdentityNormalizer.normalize(context.name()))
                          && this.aliases.matches(item.getTeamName(), context.teamName()))) {
            owners
                .computeIfAbsent(candidate.getExternalId(), ignored -> new ArrayList<>())
                .add(item.getPlayerId());
          }
        }
      }
    }
    for (List<Long> shared : owners.values()) {
      if (shared.size() > 1) {
        for (Long playerId : shared) {
          Decision previous = decisions.get(playerId);
          decisions.put(
              playerId,
              new Decision(
                  PlayerIdentityItemResult.CONFLICT,
                  "MULTIPLE_LOCAL_OWNERS",
                  previous.candidates(),
                  null));
        }
      }
    }
    return Collections.unmodifiableMap(decisions);
  }

  private Decision decide(
      PlayerIdentitySyncItem item,
      List<WhoScoredIdentityCandidate> candidates,
      Set<Long> completedLeagues) {
    if (item.getInitialExternalIds().size() > 1) {
      return new Decision(
          PlayerIdentityItemResult.CONFLICT, "MULTIPLE_INITIAL_REFERENCES", List.of(), null);
    }
    if (item.getInitialExternalId() != null) {
      return new Decision(
          PlayerIdentityItemResult.ALREADY_REFERENCED,
          "REFERENCE_ALREADY_PRESENT",
          List.of(),
          item.getInitialExternalId());
    }
    if (!WhoScoredIdentityNormalizer.validName(item.getPlayerName())) {
      return new Decision(
          PlayerIdentityItemResult.LOCAL_DATA_ERROR, "INVALID_LOCAL_PLAYER_NAME", List.of(), null);
    }
    if (!WhoScoredIdentityNormalizer.validName(item.getTeamName())) {
      return new Decision(
          PlayerIdentityItemResult.LOCAL_DATA_ERROR, "INVALID_LOCAL_TEAM_NAME", List.of(), null);
    }
    String name = WhoScoredIdentityNormalizer.normalize(item.getPlayerName());
    List<WhoScoredIdentityCandidate> observed =
        candidates.stream()
            .filter(
                candidate ->
                    candidate.getContexts().stream()
                        .anyMatch(
                            context ->
                                name.equals(WhoScoredIdentityNormalizer.normalize(context.name()))))
            .sorted(Comparator.comparing(WhoScoredIdentityCandidate::getExternalId))
            .toList();
    List<String> matching =
        observed.stream()
            .filter(
                candidate ->
                    candidate.getContexts().stream()
                        .anyMatch(
                            context ->
                                name.equals(WhoScoredIdentityNormalizer.normalize(context.name()))
                                    && this.aliases.matches(
                                        item.getTeamName(), context.teamName())))
            .map(WhoScoredIdentityCandidate::getExternalId)
            .distinct()
            .toList();
    if (matching.size() > 1) {
      return new Decision(
          PlayerIdentityItemResult.AMBIGUOUS, "MULTIPLE_MATCHING_IDENTITIES", observed, null);
    }
    if (matching.size() == 1) {
      return new Decision(
          PlayerIdentityItemResult.RESOLVED, "UNIQUE_NAME_AND_TEAM", observed, matching.getFirst());
    }
    if (!completedLeagues.contains(item.getLeagueId())) {
      return new Decision(
          PlayerIdentityItemResult.DISCOVERY_ERROR, "LOCAL_LEAGUE_INCOMPLETE", observed, null);
    }
    return new Decision(
        PlayerIdentityItemResult.NO_MATCH,
        observed.isEmpty() ? "NO_NAME_MATCH" : "TEAM_NOT_MATCHED",
        observed,
        null);
  }
}
