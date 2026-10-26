package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.responses.PlayerStatisticsResponseDTO;
import footballmarket.models.PlayerStatisticsSnapshot;

/** Transformación de la lectura individual sin seleccionar ni reinterpretar sus datos. */
public final class PlayerStatisticsMapper {
  private PlayerStatisticsMapper() {}

  public static PlayerStatisticsResponseDTO toResponse(PlayerStatisticsSnapshot snapshot) {
    return new PlayerStatisticsResponseDTO(
        snapshot.playerId(),
        snapshot.requestedSeasonKey(),
        snapshot.status(),
        snapshot.seasonKey(),
        snapshot.seasonLabel(),
        snapshot.extractionDate(),
        snapshot.competitionKey(),
        snapshot.competitionName(),
        snapshot.metrics().stream()
            .map(
                metric ->
                    new PlayerStatisticsResponseDTO.MetricDTO(
                        metric.key(),
                        metric.unit().name(),
                        metric.value(),
                        metric.source() == null ? null : metric.source().name()))
            .toList());
  }
}
