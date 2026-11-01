package footballmarket.features.identity.application.internal;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.LeagueRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.identity.application.exceptions.PlayerIdentitySynchronizationException;
import footballmarket.features.identity.domain.enums.PlayerIdentityPassType;
import footballmarket.features.identity.domain.models.PlayerIdentitySyncItem;
import footballmarket.features.identity.domain.models.WhoScoredTeamAliases;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Captura valores locales coherentes en una transacción breve, sin entidades de auditoría. */
@Component
public class PlayerIdentitySnapshotCapture {
  private final WhoScoredProperties properties;
  private final LeagueRepository leagues;
  private final PlayerRepository players;
  private final TransactionTemplate transaction;

  /** Trabajo materializado antes de red; los contratos del proveedor no se usan como dominio. */
  public record Capture(
      PlayerIdentityPassType passType,
      List<WhoScoredProperties.Competition> competitions,
      List<PlayerIdentitySyncItem> items) {
    public Capture {
      competitions = List.copyOf(competitions);
      items = List.copyOf(items);
    }
  }

  public PlayerIdentitySnapshotCapture(
      WhoScoredProperties properties,
      LeagueRepository leagues,
      PlayerRepository players,
      PlatformTransactionManager manager) {
    this.properties = properties;
    this.leagues = leagues;
    this.players = players;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }

  public Capture current() {
    return this.capture(null);
  }

  /** Recaptura solo los IDs elegibles del pase actual, incluyendo referencias concurrentes. */
  public Capture historical(List<Long> playerIds) {
    return this.capture(playerIds);
  }

  private Capture capture(List<Long> playerIds) {
    WhoScoredTeamAliases.normative();
    try {
      return this.transaction.execute(
          status -> {
            List<WhoScoredProperties.Competition> bindings =
                List.copyOf(this.properties.competitions());
            for (WhoScoredProperties.Competition binding : bindings) {
              if (!this.leagues.existsById(binding.leagueId())) {
                throw new PlayerIdentitySynchronizationException();
              }
            }
            List<Player> selected =
                playerIds == null
                    ? this.players.findIdentitySnapshot(
                        bindings.stream().map(WhoScoredProperties.Competition::leagueId).toList())
                    : playerIds.isEmpty()
                        ? List.of()
                        : this.players.findHistoricalIdentitySnapshot(playerIds);
            List<PlayerIdentitySyncItem> snapshots =
                selected.stream()
                    .map(
                        player ->
                            new PlayerIdentitySyncItem(
                                player.getId(),
                                player.getName(),
                                player.getTeam().getName(),
                                player.getLeague().getId(),
                                player.getExternalReferences().stream()
                                    .filter(
                                        reference ->
                                            reference.getProvider() == ExternalProvider.WHOSCORED)
                                    .map(reference -> reference.getExternalId())
                                    .toList()))
                    .toList();
            return new Capture(
                playerIds == null
                    ? PlayerIdentityPassType.CURRENT
                    : PlayerIdentityPassType.HISTORICAL,
                bindings,
                snapshots);
          });
    } catch (RuntimeException failure) {
      throw new PlayerIdentitySynchronizationException();
    }
  }
}
