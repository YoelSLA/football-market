package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.records.PlayerCatalogCriteria;
import footballmarket.features.catalog.domain.records.PlayerCatalogPage;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.models.enums.*;
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
@Import({
  TestcontainersConfiguration.class,
  PlayerCatalogQueryData.class,
  PlayerStatisticsQueryData.class
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class PlayerCatalogQueryServiceTest {
  @Autowired private PlayerCatalogService service;
  @Autowired private PlayerCatalogQueryData data;
  @Autowired private PlayerStatisticsQueryData statisticsData;

  @Nested
  @DisplayName("Lectura global del catálogo")
  class Query {
    @Test
    @DisplayName("Ordena nombres sin alterar presentación y desempata por ID")
    void ordersByNameAndId() {
      // Arrange
      data.player("Zulu", "Forward", null, null);
      PlayerCatalogQueryData.Fixture first = data.player("Ana", "Midfield", "Spain", null);
      PlayerCatalogQueryData.Fixture second =
          data.player("Ana", "Midfield", "Spain", first.teamId());
      PlayerCatalogCriteria criteria =
          new PlayerCatalogCriteria(null, null, null, null, null, "name,asc", false);
      // Act
      PlayerCatalogPage page = service.queryCatalog(criteria, 0, 2);
      // Assert
      assertThat(page.totalElements()).isEqualTo(3);
      assertThat(page.content())
          .extracting(entry -> entry.player().getId())
          .containsExactly(first.playerId(), second.playerId());
      assertThat(page.content()).allSatisfy(entry -> assertThat(entry.statistics()).isNull());
      assertThat(service.catalogFilters().nationalities()).containsExactly("Spain");
      // Act
      PlayerCatalogCriteria descending =
          new PlayerCatalogCriteria(null, null, null, null, null, "name,desc", false);
      PlayerCatalogPage reversed = service.queryCatalog(descending, 0, 12);
      // Assert
      assertThat(reversed.content())
          .extracting(entry -> entry.player().getName())
          .containsExactly("Zulu", "Ana", "Ana");
      assertThat(reversed.content().get(1).player().getId())
          .isLessThan(reversed.content().get(2).player().getId());
    }

    @Test
    @DisplayName("Mantiene jugadores sin métricas y distingue cero JSON de ausencia")
    void enrichesWithoutExcludingPlayers() {
      // Arrange
      PlayerCatalogQueryData.Fixture first = data.player("Con datos", "Forward", null, null);
      data.player("Sin datos", "Goalkeeper", null, null);
      LocalDate date = LocalDate.of(2026, 10, 23);
      statisticsData.row(
          first.playerId(),
          date,
          MetricSource.JSON,
          "2026",
          "2",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ZERO));
      statisticsData.row(
          first.playerId(),
          date,
          MetricSource.SCRAPING,
          "2026",
          "2",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.TEN, "assists_total", BigDecimal.ONE));
      PlayerCatalogCriteria criteria =
          new PlayerCatalogCriteria(null, null, null, null, null, "id,asc", true);
      // Act
      PlayerCatalogPage page = service.queryCatalog(criteria, 0, 12);
      // Assert
      assertThat(page.totalElements()).isEqualTo(2);
      assertThat(page.content().getFirst().statistics().goalsTotal()).isZero();
      assertThat(page.content().getFirst().statistics().sources().goalsTotal())
          .isEqualTo(MetricSource.JSON);
      assertThat(page.content().getFirst().statistics().assistsTotal())
          .isEqualByComparingTo(BigDecimal.ONE);
      assertThat(page.content().get(1).statistics().goalsTotal()).isNull();
    }
  }
}
