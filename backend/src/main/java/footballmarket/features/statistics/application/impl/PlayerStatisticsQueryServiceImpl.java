package footballmarket.features.statistics.application.impl;

import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.statistics.application.PlayerStatisticsQueryService;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.models.PlayerStatistics;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSnapshot;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsRepository;
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
