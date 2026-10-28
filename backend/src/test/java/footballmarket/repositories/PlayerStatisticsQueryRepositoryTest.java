package footballmarket.repositories;

import static org.assertj.core.api.Assertions.*;

import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.features.statistics.domain.models.PlayerStatistics;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsRepository;
import footballmarket.support.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, PlayerStatisticsQueryData.class})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class PlayerStatisticsQueryRepositoryTest {
  @Autowired private PlayerStatisticsRepository statistics;
  @Autowired private PlayerStatisticsQueryData data;
  @Autowired private PlatformTransactionManager manager;
  private static final LocalDate DATE = LocalDate.of(2026, 10, 23);

  @Nested
  @DisplayName("Selección individual confirmada")
  class Selection {
    @Test
    @DisplayName("La fecha tiene prioridad sobre el canal y excluye items sin confirmar")
    void selectsLatestConfirmedDate() {
      // Arrange
      Long playerId = data.player("Selection");
      data.row(
          playerId,
          DATE.minusDays(1),
          MetricSource.JSON,
          "2026",
          "2",
          PlayerExtractionResult.COMPLETED,
          Map.of());
      Long latest =
          data.row(
              playerId,
              DATE,
              MetricSource.SCRAPING,
              "2026",
              "2",
              PlayerExtractionResult.COMPLETED,
              Map.of());
      data.row(
          playerId,
          DATE.plusDays(1),
          MetricSource.JSON,
          "2026",
          "2",
          PlayerExtractionResult.INCOMPLETE,
          Map.of());
      // Act
      List<PlayerStatistics> rows =
          statistics.findConfirmedForPlayer(playerId, "2026", PageRequest.of(0, 1));
      // Assert
      assertThat(rows).singleElement().satisfies(row -> assertThat(row.getId()).isEqualTo(latest));
      assertThat(statistics.findConfirmedForPlayer(playerId, "other", PageRequest.of(0, 1)))
          .isEmpty();
    }

    @Test
    @DisplayName(
        "Una lectura repeatable-read no incorpora respaldo confirmado después de elegir el ancla")
    void retainsSnapshotDuringConcurrentExtraction() throws Exception {
      // Arrange
      Long playerId = data.player("Concurrent");
      data.row(
          playerId,
          DATE,
          MetricSource.JSON,
          "2026",
          "2",
          PlayerExtractionResult.COMPLETED,
          Map.of());
      TransactionTemplate reader = new TransactionTemplate(manager);
      reader.setReadOnly(true);
      reader.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
      ExecutorService writer = Executors.newSingleThreadExecutor();
      try {
        // Act
        List<PlayerStatistics> observed =
            reader.execute(
                status -> {
                  PlayerStatistics anchor =
                      statistics
                          .findConfirmedForPlayer(playerId, "2026", PageRequest.of(0, 1))
                          .getFirst();
                  Future<Long> inserted =
                      writer.submit(
                          () ->
                              data.row(
                                  playerId,
                                  DATE,
                                  MetricSource.SCRAPING,
                                  "2026",
                                  "2",
                                  PlayerExtractionResult.COMPLETED,
                                  Map.of("starts_total", BigDecimal.ONE)));
                  try {
                    inserted.get(20, TimeUnit.SECONDS);
                  } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                  } catch (ExecutionException | TimeoutException exception) {
                    throw new AssertionError(exception);
                  }
                  return statistics.findConfirmedCompanions(
                      playerId,
                      anchor.getExtractionDate(),
                      anchor.getSeasonKey(),
                      anchor.getProviderCompetitionId());
                });
        // Assert
        assertThat(observed).hasSize(1);
        assertThat(statistics.findConfirmedCompanions(playerId, DATE, "2026", "2")).hasSize(2);
      } finally {
        writer.shutdownNow();
      }
    }
  }
}
