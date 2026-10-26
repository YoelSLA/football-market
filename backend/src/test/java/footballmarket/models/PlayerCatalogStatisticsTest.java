package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.models.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import org.junit.jupiter.api.*;

@Tag("unit")
class PlayerCatalogStatisticsTest {
  @Nested
  @DisplayName("Resumen deportivo del catálogo")
  class Summary {
    @Test
    @DisplayName("El resumen conserva cero JSON y respaldo compatible con su procedencia")
    void preservesOriginalValues() {
      // Arrange
      League league = new League("Liga");
      Team team = new Team("Equipo", league, true);
      Player player = new Player("Jugador", team, "Forward");
      LocalDate date = LocalDate.of(2026, 10, 23);
      Instant time = Instant.parse("2026-10-23T00:00:00Z");
      PlayerStatisticsExtractionRun jsonRun =
          new PlayerStatisticsExtractionRun(date, MetricSource.JSON, time, null);
      PlayerStatisticsExtractionRun scrapingRun =
          new PlayerStatisticsExtractionRun(date, MetricSource.SCRAPING, time, null);
      PlayerStatisticsRunItem jsonItem = new PlayerStatisticsRunItem(jsonRun, player, "123", time);
      PlayerStatisticsRunItem scrapingItem =
          new PlayerStatisticsRunItem(scrapingRun, player, "123", time);
      PlayerStatistics json =
          new PlayerStatistics(
              player, date, MetricSource.JSON, PlayerStatistics.RESOLVED, jsonItem, time);
      PlayerStatistics scraping =
          new PlayerStatistics(
              player, date, MetricSource.SCRAPING, PlayerStatistics.RESOLVED, scrapingItem, time);
      json.resolveContext("2", "3", "2026", "2026/2027");
      scraping.resolveContext("2", "3", "2026", "2026/2027");
      json.applyMetrics(Map.of("goals_total", BigDecimal.ZERO), 1, 59);
      scraping.applyMetrics(
          Map.of("goals_total", BigDecimal.TEN, "assists_total", BigDecimal.ONE), 2, 58);
      // Act
      PlayerCatalogStatistics result = PlayerCatalogStatistics.compose(1L, json, scraping);
      // Assert
      assertThat(result.goalsTotal()).isZero();
      assertThat(result.sources().goalsTotal()).isEqualTo(MetricSource.JSON);
      assertThat(result.assistsTotal()).isEqualByComparingTo(BigDecimal.ONE);
      assertThat(result.sources().assistsTotal()).isEqualTo(MetricSource.SCRAPING);
      assertThat(result.appearancesTotal()).isNull();
      assertThat(result.extractionDate()).isEqualTo(date);
    }

    @Test
    @DisplayName("La falta de snapshot conserva tres ausencias sin datos simulados")
    void preservesMissingValues() {
      // Act
      PlayerCatalogStatistics summary = PlayerCatalogStatistics.compose(1L, null, null);
      // Assert
      assertThat(summary.appearancesTotal()).isNull();
      assertThat(summary.goalsTotal()).isNull();
      assertThat(summary.assistsTotal()).isNull();
      assertThat(summary.extractionDate()).isNull();
      assertThat(summary.sources().goalsTotal()).isNull();
    }
  }
}
