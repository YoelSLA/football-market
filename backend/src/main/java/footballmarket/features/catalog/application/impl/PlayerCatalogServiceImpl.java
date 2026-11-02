package footballmarket.features.catalog.application.impl;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.application.exceptions.IncompletePhotoException;
import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.catalog.application.exceptions.PlayerSynchronizationPersistenceException;
import footballmarket.features.catalog.application.internal.LeagueResolution;
import footballmarket.features.catalog.application.internal.PlayerApplication;
import footballmarket.features.catalog.application.internal.PlayerResolution;
import footballmarket.features.catalog.application.internal.SynchronizationAttempt;
import footballmarket.features.catalog.application.internal.TeamResolution;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.catalog.domain.records.PlayerCatalogCriteria;
import footballmarket.features.catalog.domain.records.PlayerCatalogFilters;
import footballmarket.features.catalog.domain.records.PlayerCatalogPage;
import footballmarket.features.catalog.domain.records.PlayerCatalogStatistics;
import footballmarket.features.catalog.domain.records.PlayerSnapshot;
import footballmarket.features.catalog.domain.records.PlayerSynchronizationResult;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.identity.domain.enums.ReviewSubjectType;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.models.PlayerStatistics;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsRepository;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
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
import org.springframework.transaction.annotation.Isolation;
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
  private final PlayerStatisticsRepository catalogStatistics;
  private final LeagueResolution leagues;
  private final TeamResolution teams;
  private final PlayerResolution playerResolution;
  private final TransactionTemplate transaction;

  public PlayerCatalogServiceImpl(
      PlayerRepository players,
      PlayerStatisticsRepository catalogStatistics,
      LeagueResolution leagues,
      TeamResolution teams,
      PlayerResolution playerResolution,
      PlatformTransactionManager manager) {
    this.players = players;
    this.catalogStatistics = catalogStatistics;
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
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PlayerCatalogPage queryCatalog(PlayerCatalogCriteria criteria, int page, int size) {
    Sort sort =
        "id,asc".equals(criteria.sort())
            ? Sort.by("id")
            : Sort.by(
                new Sort.Order(
                        "name,desc".equals(criteria.sort())
                            ? Sort.Direction.DESC
                            : Sort.Direction.ASC,
                        "name")
                    .ignoreCase(),
                Sort.Order.asc("id"));
    Page<Player> result =
        this.players.findCatalog(
            criteria.name(),
            criteria.leagueId(),
            criteria.teamId(),
            criteria.position(),
            criteria.nationality(),
            PageRequest.of(page, size, sort));
    java.util.Map<Long, java.util.List<PlayerStatistics>> byPlayer = new java.util.HashMap<>();
    if (criteria.includeStatistics() && !result.isEmpty()) {
      this.catalogStatistics
          .findCatalogStatistics(result.getContent().stream().map(Player::getId).toList())
          .forEach(
              row ->
                  byPlayer
                      .computeIfAbsent(
                          row.getPlayer().getId(), ignored -> new java.util.ArrayList<>())
                      .add(row));
    }
    return new PlayerCatalogPage(
        result.getContent().stream()
            .map(
                player -> {
                  PlayerCatalogStatistics statistics = null;
                  if (criteria.includeStatistics()) {
                    java.util.List<PlayerStatistics> rows =
                        byPlayer.getOrDefault(player.getId(), java.util.List.of());
                    PlayerStatistics anchor = rows.isEmpty() ? null : rows.getFirst();
                    PlayerStatistics scraping =
                        rows.stream()
                            .filter(row -> row.getSource() == MetricSource.SCRAPING)
                            .findFirst()
                            .orElse(null);
                    statistics = PlayerCatalogStatistics.compose(player.getId(), anchor, scraping);
                  }
                  return new PlayerCatalogPage.Entry(player, statistics);
                })
            .toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PlayerCatalogFilters catalogFilters() {
    Comparator<String> textOrder =
        Comparator.comparing((String value) -> value.toLowerCase(Locale.ROOT))
            .thenComparing(Comparator.naturalOrder());
    return new PlayerCatalogFilters(
        this.players.findCatalogLeagues().stream()
            .map(league -> new PlayerCatalogFilters.LeagueOption(league.getId(), league.getName()))
            .sorted(
                Comparator.comparing(
                        (PlayerCatalogFilters.LeagueOption option) ->
                            option.name().toLowerCase(Locale.ROOT))
                    .thenComparingLong(PlayerCatalogFilters.LeagueOption::id))
            .toList(),
        this.players.findCatalogTeams().stream()
            .map(
                team ->
                    new PlayerCatalogFilters.TeamOption(
                        team.getId(), team.getName(), team.getLeague().getId()))
            .sorted(
                Comparator.comparing(
                        (PlayerCatalogFilters.TeamOption option) ->
                            option.name().toLowerCase(Locale.ROOT))
                    .thenComparingLong(PlayerCatalogFilters.TeamOption::id))
            .toList(),
        this.players.findCatalogPositions().stream().sorted(textOrder).toList(),
        this.players.findCatalogNationalities().stream().sorted(textOrder).toList());
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
