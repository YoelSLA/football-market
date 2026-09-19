package footballmarket.services;

import footballmarket.config.FootballDataProperties;
import footballmarket.integrations.footballdata.FootballDataIntegration;
import footballmarket.models.Player;
import footballmarket.models.PlayerSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FootballDataPlayerService {
  private final FootballDataIntegration integration;
  private final FootballDataProperties properties;

  public PlayerSnapshot fetchSnapshot() {
    Map<Long, Player> players = new LinkedHashMap<>();
    int obtained = 0;
    int discarded = 0;
    for (String code : properties.competitions()) {
      PlayerSnapshot competition = integration.fetchCompetition(code);
      obtained += competition.obtained();
      discarded += competition.discardedInvalid();
      for (Player player : competition.players()) {
        players.putIfAbsent(player.getId(), player);
      }
    }
    return new PlayerSnapshot(List.copyOf(players.values()), obtained, discarded);
  }
}
