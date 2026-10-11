package footballmarket.services.impl;

import footballmarket.config.FootballDataProperties;
import footballmarket.integrations.FootballDataIntegration;
import footballmarket.models.records.InvalidPlayerObservation;
import footballmarket.models.records.LeagueCandidate;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.TeamCandidate;
import footballmarket.services.FootballDataPlayerService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Consolida por identidad externa los candidatos inmutables de las competiciones configuradas. */
@Service
@RequiredArgsConstructor
public class FootballDataPlayerServiceImpl implements FootballDataPlayerService {
  private final FootballDataIntegration footballDataIntegration;
  private final FootballDataProperties footballDataProperties;

  /** {@inheritDoc} */
  @Override
  public PlayerSnapshot fetchSnapshot() {
    Map<String, PlayerCandidate> players = new LinkedHashMap<>();
    List<LeagueCandidate> leagues = new ArrayList<>();
    List<TeamCandidate> teams = new ArrayList<>();
    List<InvalidPlayerObservation> invalidPlayers = new ArrayList<>();

    int obtained = 0;
    int discarded = 0;

    for (String code : this.footballDataProperties.competitions()) {
      FootballDataIntegration.CompetitionSnapshot competition =
          this.footballDataIntegration.fetchCompetition(code);

      obtained += competition.obtained();
      discarded += competition.discardedInvalid();
      competition
          .leagues()
          .forEach(league -> leagues.add(new LeagueCandidate(league.externalId(), league.name())));
      competition
          .teams()
          .forEach(
              team ->
                  teams.add(
                      new TeamCandidate(
                          team.externalId(),
                          team.name(),
                          team.leagueExternalId(),
                          team.rosterKnown())));
      competition
          .invalidPlayers()
          .forEach(
              player ->
                  invalidPlayers.add(
                      new InvalidPlayerObservation(
                          player.externalId(),
                          player.teamExternalId(),
                          player.teamName(),
                          player.cause())));

      for (FootballDataIntegration.PlayerData player : competition.players()) {
        PlayerCandidate candidate =
            new PlayerCandidate(
                player.externalId(),
                player.name(),
                player.team(),
                player.league(),
                player.position(),
                player.dateOfBirth(),
                player.nationality(),
                player.teamExternalId());
        players.merge(player.externalId(), candidate, PlayerCandidate::consolidatePresentation);
      }
    }

    return new PlayerSnapshot(
        List.copyOf(players.values()), obtained, discarded, leagues, teams, invalidPlayers);
  }
}
