package footballmarket.services.impl;

import footballmarket.config.FootballDataProperties;
import footballmarket.integrations.FootballDataIntegration;
import footballmarket.models.Player;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.services.FootballDataPlayerService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
/* Consolida en una sola foto los jugadores de las competiciones configuradas. */
public class FootballDataPlayerServiceImpl implements FootballDataPlayerService {

  private static final List<String> COMPETITIONS =
          List.of("PL", "PD", "BL1", "SA", "FL1");

  private final FootballDataIntegration footballDataIntegration;

  @Override
  /** {@inheritDoc} */
  public PlayerSnapshot fetchSnapshot() {
    Map<Long, Player> players = new LinkedHashMap<>();

    int obtained = 0;
    int discarded = 0;

    for (String code : COMPETITIONS) {
      PlayerSnapshot competition = this.footballDataIntegration.fetchCompetition(code);

      obtained += competition.obtained();
      discarded += competition.discardedInvalid();

      for (Player player : competition.players()) {
        players.putIfAbsent(player.getId(), player);
      }
    }

    return new PlayerSnapshot(List.copyOf(players.values()), obtained, discarded);
  }
}