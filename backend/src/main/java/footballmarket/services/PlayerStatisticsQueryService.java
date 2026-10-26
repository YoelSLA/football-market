package footballmarket.services;

import footballmarket.models.PlayerStatisticsSnapshot;

/** Consulta individual de estadísticas reales, sin efectos sobre campañas o valoraciones. */
public interface PlayerStatisticsQueryService {
  PlayerStatisticsSnapshot getStatistics(long playerId, String seasonKey);
}
