package footballmarket.services.impl;

import footballmarket.models.PlayerStatistics;
import footballmarket.models.PlayerStatisticsSnapshot;
import footballmarket.models.enums.MetricSource;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.PlayerStatisticsRepository;
import footballmarket.services.PlayerStatisticsQueryService;
import footballmarket.services.exceptions.PlayerNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Mantiene una misma foto transaccional entre selección del ancla y lectura del respaldo. */
@Service
@RequiredArgsConstructor
public class PlayerStatisticsQueryServiceImpl implements PlayerStatisticsQueryService {
  private final PlayerRepository players;
  private final PlayerStatisticsRepository statistics;

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PlayerStatisticsSnapshot getStatistics(long playerId, String seasonKey) {
    this.players
        .findVisibleById(playerId)
        .orElseThrow(() -> new PlayerNotFoundException("No encontramos el jugador solicitado"));
    List<PlayerStatistics> candidates =
        this.statistics.findConfirmedForPlayer(playerId, seasonKey, PageRequest.of(0, 1));
    if (candidates.isEmpty()) {
      return PlayerStatisticsSnapshot.compose(playerId, seasonKey, null, null, null);
    }
    PlayerStatistics anchor = candidates.getFirst();
    PlayerStatistics scraping =
        anchor.getSource() == MetricSource.JSON
            ? this.statistics
                .findConfirmedCompanions(
                    playerId,
                    anchor.getExtractionDate(),
                    anchor.getSeasonKey(),
                    anchor.getProviderCompetitionId())
                .stream()
                .filter(row -> row.getSource() == MetricSource.SCRAPING)
                .findFirst()
                .orElse(null)
            : null;
    // No hay un nombre histórico persistido ni una correspondencia local acreditada.
    // La liga actual del jugador no se utiliza como nombre de la competición del snapshot.
    return PlayerStatisticsSnapshot.compose(playerId, seasonKey, anchor, scraping, null);
  }
}
