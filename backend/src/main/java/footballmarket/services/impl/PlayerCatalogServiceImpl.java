package footballmarket.services.impl;

import footballmarket.models.League;
import footballmarket.models.Player;
import footballmarket.models.Team;
import footballmarket.models.enums.ReviewSubjectType;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.repositories.PlayerRepository;
import footballmarket.services.PlayerCatalogService;
import footballmarket.services.exceptions.IncompletePhotoException;
import footballmarket.services.exceptions.PlayerNotFoundException;
import footballmarket.services.exceptions.PlayerSynchronizationPersistenceException;
import footballmarket.services.internal.LeagueResolution;
import footballmarket.services.internal.PlayerApplication;
import footballmarket.services.internal.PlayerResolution;
import footballmarket.services.internal.SynchronizationAttempt;
import footballmarket.services.internal.TeamResolution;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Aplica una foto completa al catálogo en un único commit.
 *
 * <p>Coordina la resolución de ligas, equipos y jugadores en orden y mantiene los límites
 * transaccionales: cada intento corre en su propia transacción y se revierte completo ante un error
 * técnico. Un intento solo se reintenta cuando la violación de unicidad corresponde a una identidad
 * externa reconocida, y en ese caso el candidato responsable queda excluido de los intentos
 * siguientes. Las reglas de resolución pertenecen a cada colaborador.
 */
@Service
public class PlayerCatalogServiceImpl implements PlayerCatalogService {
  private static final Logger LOG = LoggerFactory.getLogger(PlayerCatalogServiceImpl.class);
  private final PlayerRepository players;
  private final LeagueResolution leagues;
  private final TeamResolution teams;
  private final PlayerResolution playerResolution;
  private final TransactionTemplate transaction;

  public PlayerCatalogServiceImpl(
      PlayerRepository players,
      LeagueResolution leagues,
      TeamResolution teams,
      PlayerResolution playerResolution,
      PlatformTransactionManager manager) {
    this.players = players;
    this.leagues = leagues;
    this.teams = teams;
    this.playerResolution = playerResolution;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  @Override
  public PlayerSynchronizationResult applySynchronization(PlayerSnapshot snapshot) {
    Map<ReviewSubjectType, Set<String>> exclusions = new EnumMap<>(ReviewSubjectType.class);
    for (ReviewSubjectType type : ReviewSubjectType.values()) {
      exclusions.put(type, new LinkedHashSet<>());
    }
    int remaining =
        snapshot.players().size() + snapshot.teams().size() + snapshot.leagues().size() + 1;
    while (remaining-- > 0) {
      SynchronizationAttempt attempt = new SynchronizationAttempt();
      try {
        PlayerSynchronizationResult result =
            Objects.requireNonNull(
                this.transaction.execute(status -> this.apply(snapshot, exclusions, attempt)));
        LOG.info(
            "Sincronización confirmada: obtenidos={}, creados={}, actualizados={}, "
                + "inactivos={}, descartados={}",
            result.obtained(),
            result.created(),
            result.updated(),
            result.markedInactive(),
            result.discardedInvalid());
        return result;
      } catch (IncompletePhotoException incomplete) {
        LOG.warn("Sincronización abstida: {}", incomplete.getMessage());
        return new PlayerSynchronizationResult(0, 0, 0, 0, 0);
      } catch (RuntimeException failure) {
        if (!this.excludeConflictedIdentity(failure, attempt, exclusions)) {
          LOG.error("Sincronización fallida: error técnico de persistencia", failure);
          throw new PlayerSynchronizationPersistenceException();
        }
      }
    }
    throw new PlayerSynchronizationPersistenceException();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Player> getActivePlayers(int page, int size) {
    return this.players.findByActiveTrue(
        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
  }

  @Override
  public Player getVisiblePlayerById(long id) {
    return this.players
        .findVisibleById(id)
        .orElseThrow(() -> new PlayerNotFoundException("No encontramos el jugador solicitado"));
  }

  /**
   * Orden de resolución: la liga antes del equipo que la referencia, y el equipo antes del jugador.
   */
  private PlayerSynchronizationResult apply(
      PlayerSnapshot snapshot,
      Map<ReviewSubjectType, Set<String>> exclusions,
      SynchronizationAttempt attempt) {
    Set<Long> protectedPlayers = new HashSet<>();
    Map<String, League> resolvedLeagues =
        this.leagues.resolve(snapshot.leagues(), exclusions.get(ReviewSubjectType.LEAGUE), attempt);
    Map<String, Team> resolvedTeams =
        this.teams.resolve(
            snapshot.teams(),
            resolvedLeagues,
            exclusions.get(ReviewSubjectType.TEAM),
            exclusions.get(ReviewSubjectType.LEAGUE),
            protectedPlayers,
            attempt);
    PlayerApplication applied =
        this.playerResolution.apply(
            snapshot.players(),
            snapshot.invalidPlayers(),
            resolvedTeams,
            exclusions.get(ReviewSubjectType.PLAYER),
            protectedPlayers,
            attempt);
    return new PlayerSynchronizationResult(
        snapshot.obtained(),
        applied.created(),
        applied.updated(),
        applied.inactive(),
        snapshot.discardedInvalid() + applied.discarded());
  }

  /**
   * Reconoce la violación de unicidad de una identidad externa y acota el siguiente intento
   * excluyendo ese candidato. Cualquier otro error revierte la ejecución completa.
   *
   * @return {@code true} si el candidato quedó excluido y el intento debe repetirse
   */
  private boolean excludeConflictedIdentity(
      Throwable failure,
      SynchronizationAttempt attempt,
      Map<ReviewSubjectType, Set<String>> exclusions) {
    if (attempt.type() == null || attempt.externalId() == null) {
      return false;
    }
    String table =
        switch (attempt.type()) {
          case PLAYER -> "player_external_references";
          case TEAM -> "team_external_references";
          case LEAGUE -> "league_external_references";
        };
    Set<Throwable> visited = new HashSet<>();
    for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
      if (cause instanceof PSQLException postgres
          && "23505".equals(postgres.getSQLState())
          && postgres.getServerErrorMessage() != null
          && table.equals(postgres.getServerErrorMessage().getTable())
          && ("uk_" + table + "_provider_external_id")
              .equals(postgres.getServerErrorMessage().getConstraint())
          && exclusions.get(attempt.type()).add(attempt.externalId())) {
        LOG.warn(
            "{} descartado: conflicto de referencia FOOTBALL_DATA, externalId={}",
            subjectName(attempt.type()),
            attempt.externalId());
        return true;
      }
    }
    return false;
  }

  private static String subjectName(ReviewSubjectType type) {
    return switch (type) {
      case PLAYER -> "Jugador";
      case TEAM -> "Equipo";
      case LEAGUE -> "Liga";
    };
  }
}
