package footballmarket.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Políticas locales de caché para las consultas frecuentes. */
@Configuration
@EnableCaching
public class QueryCacheConfiguration {

  public static final String SCORE_RANKINGS = "score-rankings";
  public static final String PLAYER_CATALOG_FILTERS = "player-catalog-filters";
  public static final String PLAYER_CATALOG = "player-catalog";
  public static final String PLAYER_DETAILS = "player-details";
  public static final String OFFICIAL_MARKET = "official-market";
  public static final String P2P_MARKET = "p2p-market";

  /**
   * Crea las seis cachés locales con sus límites independientes de duración y capacidad.
   *
   * @return administrador de cachés Caffeine
   */
  @Bean
  public CacheManager queryCacheManager() {
    SimpleCacheManager cacheManager = new SimpleCacheManager();
    cacheManager.setCaches(
        List.of(
            createCache(SCORE_RANKINGS, Duration.ofMinutes(5), 128),
            createCache(PLAYER_CATALOG_FILTERS, Duration.ofMinutes(60), 1),
            createCache(PLAYER_CATALOG, Duration.ofMinutes(5), 256),
            createCache(PLAYER_DETAILS, Duration.ofMinutes(30), 1_024),
            createCache(OFFICIAL_MARKET, Duration.ofMinutes(1), 128),
            createCache(P2P_MARKET, Duration.ofSeconds(10), 1_024)));
    return cacheManager;
  }

  private CaffeineCache createCache(String name, Duration ttl, long maximumSize) {
    return new CaffeineCache(
        name, Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maximumSize).build());
  }
}
