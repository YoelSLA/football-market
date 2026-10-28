package footballmarket.features.identity.application.internal;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.player.PlayerExternalReference;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.catalog.infraestructure.LeagueRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.identity.application.exceptions.PlayerIdentityRunNotFoundException;
import footballmarket.features.identity.application.exceptions.PlayerIdentitySourceNotEligibleException;
import footballmarket.features.identity.application.exceptions.PlayerIdentitySynchronizationException;
import footballmarket.features.identity.domain.enums.*;
import footballmarket.features.identity.domain.enums.PlayerIdentityPassType;
import footballmarket.features.identity.domain.enums.PlayerIdentityRunStatus;
import footballmarket.features.identity.domain.models.*;
import footballmarket.features.identity.domain.records.*;
import footballmarket.features.identity.infraestructure.*;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Captura coherente del caso de uso, antes de cualquier solicitud externa. */
@Component
public class PlayerIdentitySnapshotCapture {
  private final WhoScoredProperties properties;
  private final LeagueRepository leagues;
  private final PlayerRepository players;
  private final PlayerIdentitySyncRunRepository runs;
  private final PlayerIdentityLeagueDiscoveryRepository discoveries;
  private final PlayerIdentitySyncItemRepository items;
  private final TransactionTemplate transaction;

  /** Valores materializados; ninguna entidad del catálogo se guarda después como snapshot. */
  public record Capture(
      Long runId,
      PlayerIdentityPassType passType,
      List<PlayerIdentityLeagueDiscovery> leagues,
      List<PlayerIdentitySyncItem> items) {
    public Capture {
      leagues = List.copyOf(leagues);
      items = List.copyOf(items);
    }
  }

  public PlayerIdentitySnapshotCapture(
      WhoScoredProperties properties,
      LeagueRepository leagues,
      PlayerRepository players,
      PlayerIdentitySyncRunRepository runs,
      PlayerIdentityLeagueDiscoveryRepository discoveries,
      PlayerIdentitySyncItemRepository items,
      PlatformTransactionManager manager) {
    this.properties = properties;
    this.leagues = leagues;
    this.players = players;
    this.runs = runs;
    this.discoveries = discoveries;
    this.items = items;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }

  public Capture current() {
    return this.capture(null);
  }

  /** Captura nuevos atributos de pendientes del actual terminado, sin modificar el origen. */
  public Capture historical(Long sourceRunId) {
    return this.capture(sourceRunId);
  }

  private Capture capture(Long sourceRunId) {
    WhoScoredTeamAliases.normative();
    try {
      return this.transaction.execute(
          status -> {
            if (sourceRunId != null) {
              PlayerIdentitySyncRun source =
                  this.runs
                      .findById(sourceRunId)
                      .orElseThrow(PlayerIdentityRunNotFoundException::new);
              if (source.getPassType() != PlayerIdentityPassType.CURRENT
                  || source.getFinishedAt() == null
                  || (source.getStatus() != PlayerIdentityRunStatus.COMPLETED
                      && source.getStatus() != PlayerIdentityRunStatus.PARTIAL)) {
                throw new PlayerIdentitySourceNotEligibleException();
              }
            }
            List<League> bound = new ArrayList<>();
            for (WhoScoredProperties.Competition competition : this.properties.competitions()) {
              bound.add(
                  this.leagues
                      .findById(competition.leagueId())
                      .orElseThrow(PlayerIdentitySynchronizationException::new));
            }
            List<Player> selected;
            PlayerIdentityPassType passType =
                sourceRunId == null
                    ? PlayerIdentityPassType.CURRENT
                    : PlayerIdentityPassType.HISTORICAL;
            if (sourceRunId == null) {
              selected =
                  this.players.findIdentitySnapshot(bound.stream().map(League::getId).toList());
            } else {
              List<Long> pending = this.items.findHistoricalPendingPlayerIds(sourceRunId);
              selected =
                  pending.isEmpty()
                      ? List.of()
                      : this.players.findHistoricalIdentitySnapshot(pending);
            }
            PlayerIdentitySyncRun run =
                this.runs.saveAndFlush(
                    new PlayerIdentitySyncRun(passType, sourceRunId, Instant.now()));
            List<PlayerIdentityLeagueDiscovery> scopes = new ArrayList<>();
            for (int index = 0; index < bound.size(); index++) {
              League league = bound.get(index);
              scopes.add(
                  this.discoveries.save(
                      new PlayerIdentityLeagueDiscovery(
                          run.getId(),
                          league.getId(),
                          league.getName(),
                          this.properties.competitions().get(index).tournamentId())));
            }
            List<PlayerIdentitySyncItem> snapshots = new ArrayList<>();
            for (Player player : selected) {
              Team team = player.getTeam();
              League league = player.getLeague();
              List<String> references =
                  player.getExternalReferences().stream()
                      .filter(reference -> reference.getProvider() == ExternalProvider.WHOSCORED)
                      .map(PlayerExternalReference::getExternalId)
                      .toList();
              snapshots.add(
                  this.items.save(
                      new PlayerIdentitySyncItem(
                          run.getId(),
                          player.getId(),
                          player.getName(),
                          team.getId(),
                          team.getName(),
                          league.getId(),
                          league.getName(),
                          references)));
            }
            this.items.flush();
            this.discoveries.flush();
            return new Capture(run.getId(), passType, scopes, snapshots);
          });
    } catch (PlayerIdentityRunNotFoundException
        | PlayerIdentitySourceNotEligibleException failure) {
      throw failure;
    } catch (RuntimeException failure) {
      throw new PlayerIdentitySynchronizationException();
    }
  }
}
