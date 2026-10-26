package footballmarket.models;

import footballmarket.models.enums.MetricSource;
import footballmarket.models.enums.MetricUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Foto de lectura independiente de las valoraciones y de la población de extracción. */
public record PlayerStatisticsSnapshot(
    long playerId,
    String requestedSeasonKey,
    String status,
    String seasonKey,
    String seasonLabel,
    LocalDate extractionDate,
    String competitionKey,
    String competitionName,
    List<Metric> metrics) {

  public PlayerStatisticsSnapshot {
    metrics = List.copyOf(metrics);
  }

  /** Valor original y su procedencia efectiva; null no equivale a cero. */
  public record Metric(String key, MetricUnit unit, BigDecimal value, MetricSource source) {}

  /** Compone exclusivamente el contexto del ancla; el respaldo nunca cambia ese contexto. */
  public static PlayerStatisticsSnapshot compose(
      long playerId,
      String requestedSeasonKey,
      PlayerStatistics anchor,
      PlayerStatistics scraping,
      String competitionName) {
    PlayerStatistics backup = compatible(anchor, scraping) ? scraping : null;
    List<Metric> metrics =
        MetricCatalog.definitions().stream()
            .map(
                definition -> {
                  BigDecimal value = anchor == null ? null : anchor.metricValue(definition.key());
                  MetricSource source = value == null ? null : anchor.getSource();
                  if (value == null
                      && backup != null
                      && definition.sources().contains(MetricSource.SCRAPING)) {
                    value = backup.metricValue(definition.key());
                    source = value == null ? null : MetricSource.SCRAPING;
                  }
                  return new Metric(definition.key(), definition.unit(), value, source);
                })
            .toList();
    long present = metrics.stream().filter(metric -> metric.value() != null).count();
    return new PlayerStatisticsSnapshot(
        playerId,
        requestedSeasonKey,
        present == 0 ? "NO_DATA" : present == metrics.size() ? "AVAILABLE" : "PARTIAL",
        anchor == null ? null : anchor.getSeasonKey(),
        anchor == null ? null : anchor.getSeasonLabel(),
        anchor == null ? null : anchor.getExtractionDate(),
        anchor == null ? null : anchor.getProviderCompetitionId(),
        anchor == null ? null : competitionName,
        metrics);
  }

  private static boolean compatible(PlayerStatistics anchor, PlayerStatistics scraping) {
    return anchor != null
        && scraping != null
        && anchor.getSource() == MetricSource.JSON
        && scraping.getSource() == MetricSource.SCRAPING
        && Objects.equals(anchor.getPlayer().getId(), scraping.getPlayer().getId())
        && Objects.equals(anchor.getExtractionDate(), scraping.getExtractionDate())
        && Objects.equals(anchor.getSeasonKey(), scraping.getSeasonKey())
        && Objects.equals(anchor.getProviderCompetitionId(), scraping.getProviderCompetitionId());
  }
}
