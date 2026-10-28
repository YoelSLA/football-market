package footballmarket.support;

import footballmarket.models.League;
import footballmarket.models.Player;
import footballmarket.models.Team;
import footballmarket.repositories.LeagueRepository;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.TeamRepository;
import org.springframework.transaction.annotation.Transactional;

/** Datos funcionales compartidos, utilizados exclusivamente en Arrange de los tests de Service. */
public class PlayerCatalogQueryData {
  private final LeagueRepository leagues;
  private final TeamRepository teams;
  private final PlayerRepository players;

  public PlayerCatalogQueryData(
      LeagueRepository leagues, TeamRepository teams, PlayerRepository players) {
    this.leagues = leagues;
    this.teams = teams;
    this.players = players;
  }

  /** Identificadores de la precondición, sin exponer repositorios al test. */
  public record Fixture(long playerId, long teamId, long leagueId) {}

  @Transactional
  public Fixture player(String name, String position, String country, Long teamId) {
    Team team;
    if (teamId == null) {
      League league = this.leagues.save(new League(name + " League"));
      team = this.teams.save(new Team(name + " Team", league, true));
    } else {
      team = this.teams.findById(teamId).orElseThrow();
    }
    Player player = new Player(name, team, position);
    player.updateOptionalDetails(null, country);
    this.players.saveAndFlush(player);
    return new Fixture(player.getId(), team.getId(), team.getLeague().getId());
  }
}
