package footballmarket.features.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.common.config.QueryCacheConfiguration;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.records.PlayerCatalogCriteria;
import footballmarket.features.catalog.domain.records.PlayerCatalogFilters;
import footballmarket.features.catalog.domain.records.PlayerCatalogPage;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.support.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
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
  @Autowired private CacheManager cacheManager;

  @BeforeEach
  void clearsCatalogCaches() {
    cacheManager.getCache(QueryCacheConfiguration.PLAYER_CATALOG).clear();
    cacheManager.getCache(QueryCacheConfiguration.PLAYER_CATALOG_FILTERS).clear();
    cacheManager.getCache(QueryCacheConfiguration.PLAYER_DETAILS).clear();
  }

  @Nested
  @DisplayName("Lectura global del catálogo")
  class Query {
    @Test
    @DisplayName("Reutiliza consultas equivalentes y separa filtros, páginas y detalles")
    void cachesEachCatalogQueryByItsEffectiveKey() {
      // Arrange
      PlayerCatalogQueryData.Fixture player =
          data.player("Jugador cacheado", "Forward", "Argentina", null);
      statisticsData.row(
          player.playerId(),
          LocalDate.of(2026, 10, 23),
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ZERO));
      PlayerCatalogCriteria criteria =
          new PlayerCatalogCriteria(" Jugador cacheado ", null, null, null, null, null, false);
      PlayerCatalogCriteria equivalentCriteria =
          new PlayerCatalogCriteria("Jugador cacheado", null, null, null, null, "id,asc", false);
      PlayerCatalogCriteria differentCriteria =
          new PlayerCatalogCriteria("Otro jugador", null, null, null, null, "name,desc", false);

      // Act
      PlayerCatalogPage firstPage = service.queryCatalog(criteria, 0, 10);
      PlayerCatalogPage repeatedPage = service.queryCatalog(equivalentCriteria, 0, 10);
      PlayerCatalogPage differentPage = service.queryCatalog(differentCriteria, 0, 10);
      PlayerCatalogPage emptyPage = service.queryCatalog(criteria, 10, 10);
      PlayerCatalogFilters filters = service.catalogFilters();
      PlayerCatalogFilters repeatedFilters = service.catalogFilters();
      Player foundPlayer = service.getVisiblePlayerById(player.playerId());
      Player repeatedPlayer = service.getVisiblePlayerById(player.playerId());

      // Assert
      assertThat(repeatedPage).isEqualTo(firstPage);
      assertThat(firstPage.content()).hasSize(1);
      assertThat(differentPage.content()).isEmpty();
      assertThat(emptyPage.content()).isEmpty();
      assertThat(repeatedFilters).isEqualTo(filters);
      assertThat(repeatedPlayer).isSameAs(foundPlayer);
      assertThat(
              cacheManager
                  .getCache(QueryCacheConfiguration.PLAYER_CATALOG)
                  .get(new SimpleKey(criteria, 0, 10))
                  .get())
          .isSameAs(firstPage);
      assertThat(
              cacheManager
                  .getCache(QueryCacheConfiguration.PLAYER_CATALOG)
                  .get(new SimpleKey(differentCriteria, 0, 10)))
          .isNotNull();
      assertThat(
              cacheManager
                  .getCache(QueryCacheConfiguration.PLAYER_CATALOG)
                  .get(new SimpleKey(criteria, 10, 10)))
          .isNotNull();
      assertThat(
              cacheManager
                  .getCache(QueryCacheConfiguration.PLAYER_CATALOG_FILTERS)
                  .get(SimpleKey.EMPTY)
                  .get())
          .isSameAs(filters);
      assertThat(
              cacheManager
                  .getCache(QueryCacheConfiguration.PLAYER_DETAILS)
                  .get(player.playerId())
                  .get())
          .isSameAs(foundPlayer);
    }

    @Test
    @DisplayName("Ordena nombres sin alterar presentación y desempata por ID")
    void ordersByNameAndId() {
      // Arrange
      PlayerCatalogQueryData.Fixture zulu = data.player("Zulu", "Forward", null, null);
      PlayerCatalogQueryData.Fixture first = data.player("Ana", "Midfield", "Spain", null);
      PlayerCatalogQueryData.Fixture second =
          data.player("Ana", "Midfield", "Spain", first.teamId());
      LocalDate date = LocalDate.of(2026, 10, 23);
      for (PlayerCatalogQueryData.Fixture player : java.util.List.of(zulu, first, second)) {
        statisticsData.row(
            player.playerId(),
            date,
            MetricSource.JSON,
            "2026",
            "2",
            PlayerExtractionResult.COMPLETED,
            Map.of("goals_total", BigDecimal.ZERO));
      }
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
    @DisplayName("Excluye jugadores sin métricas y distingue cero JSON de ausencia parcial")
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
      assertThat(page.totalElements()).isEqualTo(1);
      assertThat(page.content().getFirst().statistics().goalsTotal()).isZero();
      assertThat(page.content().getFirst().statistics().sources().goalsTotal())
          .isEqualTo(MetricSource.JSON);
      assertThat(page.content().getFirst().statistics().assistsTotal())
          .isEqualByComparingTo(BigDecimal.ONE);
      assertThat(page.content().getFirst().statistics().appearancesTotal()).isNull();
      assertThat(service.catalogFilters().positions()).containsExactly("Forward");
    }
  }
}
