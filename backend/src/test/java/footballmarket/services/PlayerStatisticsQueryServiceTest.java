package footballmarket.services;

import static org.assertj.core.api.Assertions.*;

import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.statistics.application.PlayerStatisticsQueryService;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSnapshot;
import footballmarket.support.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, PlayerStatisticsQueryData.class})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class PlayerStatisticsQueryServiceTest {
  @Autowired private PlayerStatisticsQueryService service;
  @Autowired private PlayerStatisticsQueryData data;
  private static final LocalDate DATE = LocalDate.of(2026, 10, 23);

  @Nested
  @DisplayName("Consulta estadística por jugador")
  class Query {
    @Test
    @DisplayName("Compone el snapshot individual sin exigir campaña poblacional terminada")
    void composesIndividualSnapshot() {
      // Arrange
      Long playerId = data.player("Service");
      data.row(
          playerId,
          DATE,
          MetricSource.JSON,
          "2026",
          "2",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ZERO));
      data.row(
          playerId,
          DATE,
          MetricSource.SCRAPING,
          "2026",
          "2",
          PlayerExtractionResult.COMPLETED,
          Map.of("starts_total", BigDecimal.ONE));
      // Act
      PlayerStatisticsSnapshot snapshot = service.getStatistics(playerId, "2026");
      // Assert
      assertThat(snapshot.playerId()).isEqualTo(playerId);
      assertThat(snapshot.extractionDate()).isEqualTo(DATE);
      assertThat(snapshot.metrics()).hasSize(60);
      assertThat(snapshot.metrics())
          .filteredOn(metric -> metric.key().equals("starts_total"))
          .singleElement()
          .satisfies(metric -> assertThat(metric.value()).isEqualByComparingTo(BigDecimal.ONE));
      assertThat(snapshot.competitionName()).isNull();
    }

    @Test
    @DisplayName("Jugador visible sin snapshot responde sin datos, no como jugador inexistente")
    void returnsNoData() {
      // Arrange
      Long playerId = data.player("Empty");
      // Act
      PlayerStatisticsSnapshot snapshot = service.getStatistics(playerId, "2026");
      // Assert
      assertThat(snapshot.status()).isEqualTo("NO_DATA");
      assertThat(snapshot.metrics()).hasSize(60);
    }

    @Test
    @DisplayName("Un jugador inactivo no revela estadísticas")
    void hidesInactivePlayer() {
      // Arrange
      Long playerId = data.player("Inactive");
      data.deactivate(playerId);
      // Act / Assert
      assertThatExceptionOfType(PlayerNotFoundException.class)
          .isThrownBy(() -> service.getStatistics(playerId, null));
    }
  }
}
