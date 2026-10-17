package footballmarket.services.internal;

import footballmarket.models.*;
import footballmarket.models.enums.*;
import footballmarket.repositories.*;
import footballmarket.services.exceptions.PlayerIdentitySynchronizationException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.postgresql.util.PSQLException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Confirma referencia y decisión atómicamente; audita fallos después del rollback en otra
 * transacción.
 */
@Component
public class PlayerIdentityReferenceConfirmation {
  private final PlayerRepository players;
  private final PlayerExternalReferenceRepository references;
  private final PlayerIdentitySyncItemRepository items;
  private final TransactionTemplate transaction;

  public PlayerIdentityReferenceConfirmation(
      PlayerRepository players,
      PlayerExternalReferenceRepository references,
      PlayerIdentitySyncItemRepository items,
      PlatformTransactionManager manager) {
    this.players = players;
    this.references = references;
    this.items = items;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public void confirm(Long itemId, WhoScoredIdentityMatcher.Decision decision) {
    try {
      this.transaction.executeWithoutResult(status -> this.apply(itemId, decision));
    } catch (RuntimeException failedWrite) {
      boolean unique = knownUniqueness(failedWrite);
      try {
        this.transaction.executeWithoutResult(
            status -> {
              PlayerIdentitySyncItem item = this.items.findById(itemId).orElseThrow();
              if (unique && decision.externalId() != null) {
                Player player = this.players.lockIdentityById(item.getPlayerId()).orElse(null);
                if (player != null && this.classifyCurrent(item, player, decision)) {
                  return;
                }
              }
              item.decide(
                  PlayerIdentityItemResult.PERSISTENCE_ERROR,
                  "REFERENCE_CONFIRMATION_FAILED",
                  "No se pudo confirmar la referencia",
                  decision.candidates(),
                  null,
                  Instant.now());
            });
      } catch (RuntimeException failedAudit) {
        throw new PlayerIdentitySynchronizationException();
      }
    }
  }

  private void apply(Long itemId, WhoScoredIdentityMatcher.Decision decision) {
    PlayerIdentitySyncItem item = this.items.findById(itemId).orElseThrow();
    if (decision.result() != PlayerIdentityItemResult.RESOLVED) {
      item.decide(
          decision.result(),
          decision.reasonCode(),
          "Resultado de identificación registrado",
          decision.candidates(),
          decision.externalId(),
          Instant.now());
      return;
    }
    Player player = this.players.lockIdentityById(item.getPlayerId()).orElse(null);
    if (player == null) {
      item.decide(
          PlayerIdentityItemResult.PERSISTENCE_ERROR,
          "PLAYER_NO_LONGER_EXISTS",
          "El jugador ya no existe",
          decision.candidates(),
          null,
          Instant.now());
    } else if (!this.classifyCurrent(item, player, decision)) {
      player.addExternalReference(ExternalProvider.WHOSCORED, decision.externalId());
      item.decide(
          PlayerIdentityItemResult.RESOLVED,
          decision.reasonCode(),
          "Coincidencia única por nombre y equipo",
          decision.candidates(),
          decision.externalId(),
          Instant.now());
      this.players.flush();
    }
  }

  private boolean classifyCurrent(
      PlayerIdentitySyncItem item, Player player, WhoScoredIdentityMatcher.Decision decision) {
    List<String> current =
        player.getExternalReferences().stream()
            .filter(reference -> reference.getProvider() == ExternalProvider.WHOSCORED)
            .map(PlayerExternalReference::getExternalId)
            .distinct()
            .toList();
    if (!current.isEmpty()) {
      boolean same = current.size() == 1 && current.getFirst().equals(decision.externalId());
      item.decide(
          same ? PlayerIdentityItemResult.ALREADY_REFERENCED : PlayerIdentityItemResult.CONFLICT,
          same ? "REFERENCE_CONFIRMED_CONCURRENTLY" : "PLAYER_REFERENCE_CONFLICT",
          "Referencia concurrente conservada",
          decision.candidates(),
          same ? decision.externalId() : null,
          Instant.now());
      return true;
    }
    PlayerExternalReference owner =
        this.references
            .findByProviderAndExternalId(ExternalProvider.WHOSCORED, decision.externalId())
            .orElse(null);
    if (owner != null) {
      item.decide(
          PlayerIdentityItemResult.CONFLICT,
          "EXTERNAL_ID_ALREADY_OWNED",
          "La identidad pertenece a otro jugador",
          decision.candidates(),
          null,
          Instant.now());
      return true;
    }
    return false;
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
