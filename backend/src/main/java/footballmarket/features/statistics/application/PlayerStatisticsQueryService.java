package footballmarket.features.statistics.application;

import footballmarket.features.statistics.domain.records.PlayerStatisticsSnapshot;

/** Consulta individual de estadísticas reales, sin efectos sobre campañas o valoraciones. */
public interface PlayerStatisticsQueryService {
  PlayerStatisticsSnapshot getStatistics(long playerId, String seasonKey);
}
