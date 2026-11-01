package footballmarket.features.identity.application.internal;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.player.PlayerExternalReference;
import footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.identity.domain.enums.PlayerIdentityItemResult;
import footballmarket.features.identity.domain.models.WhoScoredIdentityMatcher;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.postgresql.util.PSQLException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Persiste solo referencias, con lock breve y rollback aislado por jugador. */
@Component
public class PlayerIdentityReferenceConfirmation {
  private final PlayerRepository players;
  private final PlayerExternalReferenceRepository references;
  private final TransactionTemplate transaction;

  public PlayerIdentityReferenceConfirmation(
      PlayerRepository players,
      PlayerExternalReferenceRepository references,
      PlatformTransactionManager manager) {
    this.players = players;
    this.references = references;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public PlayerIdentityItemResult confirm(
      Long playerId, WhoScoredIdentityMatcher.Decision decision) {
    if (decision.result() != PlayerIdentityItemResult.RESOLVED) {
      return decision.result();
    }
    try {
      return this.transaction.execute(
          status -> {
            Player player = this.players.lockIdentityById(playerId).orElse(null);
            if (player == null) {
              return PlayerIdentityItemResult.PERSISTENCE_ERROR;
            }
            PlayerIdentityItemResult current = this.classifyCurrent(player, decision.externalId());
            if (current != null) {
              return current;
            }
            player.addExternalReference(ExternalProvider.WHOSCORED, decision.externalId());
            this.players.flush();
            return PlayerIdentityItemResult.RESOLVED;
          });
    } catch (RuntimeException failedWrite) {
      if (knownUniqueness(failedWrite)) {
        try {
          return this.transaction.execute(
              status -> {
                Player player = this.players.lockIdentityById(playerId).orElse(null);
                PlayerIdentityItemResult current =
                    player == null ? null : this.classifyCurrent(player, decision.externalId());
                return current == null ? PlayerIdentityItemResult.PERSISTENCE_ERROR : current;
              });
        } catch (RuntimeException failedRead) {
          return PlayerIdentityItemResult.PERSISTENCE_ERROR;
        }
      }
      return PlayerIdentityItemResult.PERSISTENCE_ERROR;
    }
  }

  private PlayerIdentityItemResult classifyCurrent(Player player, String externalId) {
    List<String> current =
        player.getExternalReferences().stream()
            .filter(reference -> reference.getProvider() == ExternalProvider.WHOSCORED)
            .map(PlayerExternalReference::getExternalId)
            .distinct()
            .toList();
    if (!current.isEmpty()) {
      return current.size() == 1 && current.getFirst().equals(externalId)
          ? PlayerIdentityItemResult.ALREADY_REFERENCED
          : PlayerIdentityItemResult.CONFLICT;
    }
    return this.references
            .findByProviderAndExternalId(ExternalProvider.WHOSCORED, externalId)
            .isPresent()
        ? PlayerIdentityItemResult.CONFLICT
        : null;
  }

  private static boolean knownUniqueness(Throwable failure) {
    Set<Throwable> visited = new HashSet<>();
    for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
      if (cause instanceof PSQLException postgres
          && "23505".equals(postgres.getSQLState())
          && postgres.getServerErrorMessage() != null
          && "player_external_references".equals(postgres.getServerErrorMessage().getTable())
          && postgres.getServerErrorMessage().getConstraint() != null
          && Set.of(
                  "uk_player_external_references_provider_external_id",
                  "uk_player_external_references_whoscored_player")
              .contains(postgres.getServerErrorMessage().getConstraint())) {
        return true;
      }
    }
    return false;
  }
}
