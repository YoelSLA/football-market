package footballmarket.models;

import footballmarket.models.enums.MetricSource;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Tres métricas originales de la misma foto individual, sin nuevas tasas ni imputación. */
public record PlayerCatalogStatistics(
    String seasonKey,
    String seasonLabel,
    LocalDate extractionDate,
    String competitionKey,
    BigDecimal appearancesTotal,
    BigDecimal goalsTotal,
    BigDecimal assistsTotal,
    Sources sources) {
  /** Procedencia por valor, conservada aunque no se muestre en las tarjetas. */
  public record Sources(
      MetricSource appearancesTotal, MetricSource goalsTotal, MetricSource assistsTotal) {}

  public static PlayerCatalogStatistics compose(
      long playerId, PlayerStatistics anchor, PlayerStatistics scraping) {
    PlayerStatisticsSnapshot snapshot =
        PlayerStatisticsSnapshot.compose(playerId, null, anchor, scraping, null);
    PlayerStatisticsSnapshot.Metric appearances = metric(snapshot, "appearances_total");
    PlayerStatisticsSnapshot.Metric goals = metric(snapshot, "goals_total");
    PlayerStatisticsSnapshot.Metric assists = metric(snapshot, "assists_total");
    return new PlayerCatalogStatistics(
        snapshot.seasonKey(),
        snapshot.seasonLabel(),
        snapshot.extractionDate(),
        snapshot.competitionKey(),
        appearances.value(),
        goals.value(),
        assists.value(),
        new Sources(appearances.source(), goals.source(), assists.source()));
  }

  private static PlayerStatisticsSnapshot.Metric metric(
      PlayerStatisticsSnapshot snapshot, String key) {
    return snapshot.metrics().stream()
        .filter(value -> value.key().equals(key))
        .findFirst()
        .orElseThrow();
  }
}
