package footballmarket.services.impl;

import footballmarket.config.FootballDataProperties;
import footballmarket.integrations.FootballDataIntegration;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.services.FootballDataPlayerService;
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

    int obtained = 0;
    int discarded = 0;

    for (String code : this.footballDataProperties.competitions()) {
      PlayerSnapshot competition = this.footballDataIntegration.fetchCompetition(code);

      obtained += competition.obtained();
      discarded += competition.discardedInvalid();

      for (PlayerCandidate player : competition.players()) {
        players.putIfAbsent(player.externalId(), player);
      }
    }

    return new PlayerSnapshot(List.copyOf(players.values()), obtained, discarded);
  }
}
