package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.models.PlayerStatistics;
import footballmarket.features.statistics.domain.models.PlayerStatisticsExtractionRun;
import footballmarket.features.statistics.domain.models.PlayerStatisticsRunItem;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSnapshot;
import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import org.junit.jupiter.api.*;

@Tag("unit")
class PlayerStatisticsSnapshotTest {
  private static final LocalDate DATE = LocalDate.of(2026, 10, 23);
  private final League league = new League("League");
  private final Team team = new Team("Team", league, true);
  private final Player player = new Player("Player", team, "Forward");

  private PlayerStatistics row(
      MetricSource source, String competition, Map<String, BigDecimal> values) {
    Instant time = Instant.parse("2026-10-23T12:00:00Z");
    PlayerStatisticsExtractionRun run = new PlayerStatisticsExtractionRun(DATE, source, time, null);
    PlayerStatisticsRunItem item = new PlayerStatisticsRunItem(run, player, "123", time);
    PlayerStatistics row =
        new PlayerStatistics(player, DATE, source, PlayerStatistics.RESOLVED, item, time);
    row.resolveContext(competition, "3", "2026", "2026/2027");
    row.applyMetrics(values, values.size(), 60 - values.size());
    return row;
  }

  @Nested
  @DisplayName("Composición de estadísticas individuales")
  class Composition {
    @Test
    @DisplayName("Conserva cero JSON y respalda únicamente las ausencias con su procedencia")
    void preservesZeroAndFillsNull() {
      // Arrange
      PlayerStatistics json = row(MetricSource.JSON, "2", Map.of("goals_total", BigDecimal.ZERO));
      PlayerStatistics scraping =
          row(
              MetricSource.SCRAPING,
              "2",
              Map.of("goals_total", BigDecimal.TEN, "starts_total", BigDecimal.ONE));
      // Act
      PlayerStatisticsSnapshot snapshot =
          PlayerStatisticsSnapshot.compose(1L, "2026", json, scraping, null);
      // Assert
      assertThat(snapshot.metrics()).hasSize(60);
      assertThat(snapshot.metrics())
          .extracting(PlayerStatisticsSnapshot.Metric::key)
          .doesNotHaveDuplicates();
      assertThat(snapshot.metrics())
          .filteredOn(metric -> metric.key().equals("goals_total"))
          .singleElement()
          .satisfies(
              metric -> {
                assertThat(metric.value()).isZero();
                assertThat(metric.source()).isEqualTo(MetricSource.JSON);
              });
      assertThat(snapshot.metrics())
          .filteredOn(metric -> metric.key().equals("starts_total"))
          .singleElement()
          .satisfies(
              metric -> {
                assertThat(metric.value()).isEqualByComparingTo(BigDecimal.ONE);
                assertThat(metric.source()).isEqualTo(MetricSource.SCRAPING);
              });
      assertThat(snapshot.status()).isEqualTo("PARTIAL");
    }

    @Test
    @DisplayName("No combina competiciones diferentes")
    void rejectsForeignContext() {
      // Arrange
      PlayerStatistics json = row(MetricSource.JSON, "2", Map.of());
      PlayerStatistics scraping =
          row(MetricSource.SCRAPING, "99", Map.of("starts_total", BigDecimal.ONE));
      // Act
      PlayerStatisticsSnapshot snapshot =
          PlayerStatisticsSnapshot.compose(1L, "2026", json, scraping, null);
      // Assert
      assertThat(snapshot.metrics())
          .allSatisfy(
              metric -> {
                assertThat(metric.value()).isNull();
                assertThat(metric.source()).isNull();
              });
      assertThat(snapshot.status()).isEqualTo("NO_DATA");
    }

    @Test
    @DisplayName("Sin snapshot mantiene las sesenta ausencias y el contexto solicitado")
    void representsNoData() {
      // Act
      PlayerStatisticsSnapshot snapshot =
          PlayerStatisticsSnapshot.compose(1L, "2026", null, null, null);
      // Assert
      assertThat(snapshot.requestedSeasonKey()).isEqualTo("2026");
      assertThat(snapshot.extractionDate()).isNull();
      assertThat(snapshot.metrics())
          .hasSize(60)
          .allSatisfy(metric -> assertThat(metric.value()).isNull());
    }

    @Test
    @DisplayName("Permite un snapshot exclusivamente de scraping")
    void usesScrapingAlone() {
      // Arrange
      PlayerStatistics scraping =
          row(MetricSource.SCRAPING, "2", Map.of("starts_total", BigDecimal.ONE));
      // Act
      PlayerStatisticsSnapshot snapshot =
          PlayerStatisticsSnapshot.compose(1L, null, scraping, null, null);
      // Assert
      assertThat(snapshot.seasonKey()).isEqualTo("2026");
      assertThat(snapshot.metrics())
          .filteredOn(metric -> metric.value() != null)
          .allSatisfy(metric -> assertThat(metric.source()).isEqualTo(MetricSource.SCRAPING));
    }
  }
}
