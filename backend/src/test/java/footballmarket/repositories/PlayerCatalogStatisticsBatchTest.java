package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.features.statistics.domain.models.PlayerStatistics;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsRepository;
import footballmarket.support.*;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class PlayerCatalogStatisticsBatchTest {
  @Autowired private PlayerStatisticsRepository repository;
  @Autowired private PlayerStatisticsQueryData data;
  @Autowired private EntityManagerFactory factory;

  @Nested
  @DisplayName("Snapshots por lote de catálogo")
  class Batch {
    @ParameterizedTest
    @ValueSource(ints = {1, 12, 100})
    @DisplayName("La consulta estadística mantiene un único statement para cada tamaño de lote")
    void usesOneStatementPerBatch(int size) {
      // Arrange
      List<Long> ids = new ArrayList<>();
      LocalDate date = LocalDate.of(2026, 10, 23);
      for (int index = 0; index < size; index++) {
        Long id = data.player("Batch " + index);
        data.row(
            id, date, MetricSource.JSON, "2026", "2", PlayerExtractionResult.COMPLETED, Map.of());
        ids.add(id);
      }
      org.hibernate.stat.Statistics statistics =
          factory.unwrap(SessionFactory.class).getStatistics();
      boolean enabled = statistics.isStatisticsEnabled();
      statistics.setStatisticsEnabled(true);
      statistics.clear();
      try {
        // Act
        List<PlayerStatistics> rows = repository.findCatalogStatistics(ids);
        // Assert
        assertThat(rows).hasSize(size);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
      } finally {
        statistics.setStatisticsEnabled(enabled);
      }
    }

    @Test
    @DisplayName("Selecciona ancla individual y excluye respaldos de otro contexto")
    void selectsOnlyMatchingRows() {
      // Arrange
      Long first = data.player("Lote A");
      Long second = data.player("Lote B");
      LocalDate date = LocalDate.of(2026, 10, 23);
      Long json =
          data.row(
              first,
              date,
              MetricSource.JSON,
              "2026",
              "2",
              PlayerExtractionResult.COMPLETED,
              Map.of("goals_total", BigDecimal.ZERO));
      data.row(
          first,
          date,
          MetricSource.SCRAPING,
          "2026",
          "99",
          PlayerExtractionResult.COMPLETED,
          Map.of());
      Long scraping =
          data.row(
              second,
              date.minusDays(1),
              MetricSource.SCRAPING,
              "2025",
              "2",
              PlayerExtractionResult.COMPLETED,
              Map.of());
      // Act
      List<PlayerStatistics> rows = repository.findCatalogStatistics(List.of(first, second));
      // Assert
      assertThat(rows).extracting(PlayerStatistics::getId).containsExactly(json, scraping);
    }
  }
}
