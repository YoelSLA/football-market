package footballmarket.common.config;

import static org.assertj.core.api.Assertions.*;

import com.github.benmanes.caffeine.cache.Cache;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

/** Verifica los nombres y las políticas Caffeine de las cachés de consultas. */
@Tag("lightweight")
class QueryCacheConfigurationTest {

  private final CacheManager cacheManager = createCacheManager();

  @Nested
  @DisplayName("Políticas de caché")
  class CachePolicies {

    @Test
    @DisplayName("Configura las seis cachés con su expiración y capacidad")
    void configuresEachCachePolicy() {
      Map<String, CachePolicy> expectedPolicies =
          Map.of(
              QueryCacheConfiguration.SCORE_RANKINGS, new CachePolicy(300, 128),
              QueryCacheConfiguration.PLAYER_CATALOG_FILTERS, new CachePolicy(3_600, 1),
              QueryCacheConfiguration.PLAYER_CATALOG, new CachePolicy(300, 256),
              QueryCacheConfiguration.PLAYER_DETAILS, new CachePolicy(1_800, 1_024),
              QueryCacheConfiguration.OFFICIAL_MARKET, new CachePolicy(60, 128),
              QueryCacheConfiguration.P2P_MARKET, new CachePolicy(10, 1_024));

      assertThat(cacheManager.getCacheNames())
          .containsExactlyInAnyOrderElementsOf(expectedPolicies.keySet());

      expectedPolicies.forEach(
          (name, expected) -> {
            CaffeineCache cache = (CaffeineCache) cacheManager.getCache(name);
            Cache<Object, Object> nativeCache = cache.getNativeCache();

            assertThat(
                    nativeCache
                        .policy()
                        .expireAfterWrite()
                        .orElseThrow()
                        .getExpiresAfter(TimeUnit.SECONDS))
                .isEqualTo(expected.ttlSeconds());
            assertThat(nativeCache.policy().eviction().orElseThrow().getMaximum())
                .isEqualTo(expected.maximumSize());
          });
    }
  }

  private CacheManager createCacheManager() {
    CacheManager manager = new QueryCacheConfiguration().queryCacheManager();
    ((org.springframework.cache.support.SimpleCacheManager) manager).afterPropertiesSet();
    return manager;
  }

  private record CachePolicy(long ttlSeconds, long maximumSize) {}
}
