package footballmarket.support;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.records.LeagueCandidate;
import footballmarket.features.catalog.domain.records.PlayerCandidate;
import footballmarket.features.catalog.domain.records.PlayerSnapshot;
import footballmarket.features.catalog.domain.records.TeamCandidate;
import footballmarket.features.catalog.infraestructure.LeagueRepository;
import footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * Preparación de identidad para Arrange; la lectura de ligas no depende de la elegibilidad del
 * catálogo ni añade métricas que alterarían los escenarios de extracción.
 */
public class IdentityCatalogueFixture {
  private final PlayerCatalogService catalogue;
  private final LeagueRepository leagueRepository;
  private final PlayerExternalReferenceRepository playerReferences;
  private final PlayerRepository players;

  public IdentityCatalogueFixture(
      PlayerCatalogService catalogue,
      LeagueRepository leagueRepository,
      PlayerExternalReferenceRepository playerReferences,
      PlayerRepository players) {
    this.catalogue = catalogue;
    this.leagueRepository = leagueRepository;
    this.playerReferences = playerReferences;
    this.players = players;
  }

  /** Identidad y liga de una precondición, sin exigir estadísticas existentes. */
  public record PlayerContext(Long playerId, Long leagueId) {}

  /** Snapshot completo de identidad persistida, sin exigir métricas ni elegibilidad. */
  @Transactional(readOnly = true)
  public List<Player> identitySnapshot() {
    List<Long> leagueIds =
        this.leagueRepository.findAll().stream().map(League::getId).toList();
    return this.players.findIdentitySnapshot(leagueIds);
  }

  /** Liga persistida por nombre, sin exigir que sus jugadores sean elegibles en el catálogo. */
  @Transactional(readOnly = true)
  public Long leagueId(String name) {
    return this.leagueRepository.findAll().stream()
        .filter(league -> name.equals(league.getName()))
        .findFirst()
        .orElseThrow()
        .getId();
  }

  /** Resuelve el jugador preparado por referencia externa únicamente durante Arrange. */
  @Transactional(readOnly = true)
  public PlayerContext playerContext(String externalId) {
    var player =
        this.playerReferences
            .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, externalId)
            .orElseThrow()
            .getPlayer();
    return new PlayerContext(player.getId(), player.getTeam().getLeague().getId());
  }

  /** Materializa identidad y asociaciones para preparar tests sin exigir métricas. */
  @Transactional(readOnly = true)
  public Player player(String externalId) {
    Long id = this.playerContext(externalId).playerId();
    return this.players.findHistoricalIdentitySnapshot(List.of(id)).getFirst();
  }

  @Transactional
  public List<Long> prepare() {
    return prepareWithFirstName("Player 1");
  }

  @Transactional
  public List<Long> prepareWithFirstName(String firstName) {
    List<LeagueCandidate> leagues = new ArrayList<>();
    List<TeamCandidate> teams = new ArrayList<>();
    List<PlayerCandidate> players = new ArrayList<>();
    for (int index = 1; index <= 5; index++) {
      String leagueId = Integer.toString(100 + index);
      String teamId = Integer.toString(200 + index);
      leagues.add(new LeagueCandidate(leagueId, "League " + index));
      teams.add(new TeamCandidate(teamId, "Team " + index, leagueId, true));
      players.add(
          new PlayerCandidate(
              Integer.toString(300 + index),
              index == 1 ? firstName : "Player " + index,
              "Team " + index,
              "League " + index,
              "Forward",
              null,
              null,
              teamId));
    }
    this.catalogue.applySynchronization(
        new PlayerSnapshot(players, 5, 0, leagues, teams, List.of()));
    var persistedLeagues = this.leagueRepository.findAll();
    return leagues.stream()
        .map(
            candidate ->
                persistedLeagues.stream()
                    .filter(league -> candidate.name().equals(league.getName()))
                    .findFirst()
                    .orElseThrow()
                    .getId())
        .toList();
  }

  public void movePlayersOutsideScope() {
    LeagueCandidate league = new LeagueCandidate("999", "Outside League");
    TeamCandidate team = new TeamCandidate("999", "Outside Team", "999", true);
    List<PlayerCandidate> players = new ArrayList<>();
    for (int index = 1; index <= 5; index++) {
      players.add(
          new PlayerCandidate(
              Integer.toString(300 + index),
              "Player " + index,
              "Outside Team",
              "Outside League",
              "Forward",
              null,
              null,
              "999"));
    }
    catalogue.applySynchronization(
        new PlayerSnapshot(players, 5, 0, List.of(league), List.of(team), List.of()));
  }
}
