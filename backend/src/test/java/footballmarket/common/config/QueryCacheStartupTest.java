package footballmarket.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;

@Tag("lightweight")
class QueryCacheStartupTest {

  @Nested
  @DisplayName("Arranque de las cachés de consulta")
  class Startup {
    @Test
    @DisplayName("Un contexto nuevo expone las seis cachés sin resultados previos")
    void startsWithEmptyCaches() {
      // Arrange
      ApplicationContextRunner contextRunner =
          new ApplicationContextRunner().withUserConfiguration(QueryCacheConfiguration.class);

      // Act / Assert
      contextRunner.run(
          context -> {
            assertThat(context).hasNotFailed();
            CacheManager cacheManager = context.getBean(CacheManager.class);
            assertEmpty(cacheManager, QueryCacheConfiguration.SCORE_RANKINGS);
            assertEmpty(cacheManager, QueryCacheConfiguration.PLAYER_CATALOG_FILTERS);
            assertEmpty(cacheManager, QueryCacheConfiguration.PLAYER_CATALOG);
            assertEmpty(cacheManager, QueryCacheConfiguration.PLAYER_DETAILS);
            assertEmpty(cacheManager, QueryCacheConfiguration.OFFICIAL_MARKET);
            assertEmpty(cacheManager, QueryCacheConfiguration.P2P_MARKET);
          });
    }
  }

  private static void assertEmpty(CacheManager cacheManager, String cacheName) {
    assertThat(cacheManager.getCache(cacheName)).isNotNull();
    assertThat(cacheManager.getCache(cacheName).get("startup-check")).isNull();
  }
}
