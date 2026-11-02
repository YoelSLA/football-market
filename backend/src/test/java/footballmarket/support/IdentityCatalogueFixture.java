package footballmarket.support;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.records.LeagueCandidate;
import footballmarket.features.catalog.domain.records.PlayerCandidate;
import footballmarket.features.catalog.domain.records.PlayerSnapshot;
import footballmarket.features.catalog.domain.records.TeamCandidate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * Preparación por Service; la transacción permite materializar la lectura antes de salir de
 * Arrange.
 */
public class IdentityCatalogueFixture {
  private final PlayerCatalogService catalogue;

  public IdentityCatalogueFixture(PlayerCatalogService catalogue) {
    this.catalogue = catalogue;
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
    List<Player> persisted = this.catalogue.getActivePlayers(0, 10).getContent();
    return persisted.stream().map(player -> player.getLeague().getId()).toList();
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
