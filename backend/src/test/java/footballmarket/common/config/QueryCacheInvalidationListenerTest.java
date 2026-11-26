package footballmarket.common.config;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifica el alcance y el momento de las invalidaciones de caché. */
@Tag("lightweight")
class QueryCacheInvalidationListenerTest {

  @Nested
  @DisplayName("Invalidación por evento")
  class CacheInvalidation {

    @Test
    @DisplayName("Limpia solo las cachés indicadas después del commit")
    void clearsOnlyNamedCachesAfterCommit() {
      try (AnnotationConfigApplicationContext context = createContext()) {
        CacheManager cacheManager = context.getBean(CacheManager.class);
        TransactionTemplate transaction =
            new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        QueryCacheInvalidationEvent event =
            new QueryCacheInvalidationEvent(
                Set.of(
                    QueryCacheConfiguration.SCORE_RANKINGS,
                    QueryCacheConfiguration.OFFICIAL_MARKET));

        putEntryInEveryCache(cacheManager);

        transaction.executeWithoutResult(
            status -> {
              context.publishEvent(event);

              assertThat(cacheManager.getCache(QueryCacheConfiguration.SCORE_RANKINGS).get("key"))
                  .isNotNull();
              assertThat(cacheManager.getCache(QueryCacheConfiguration.OFFICIAL_MARKET).get("key"))
                  .isNotNull();
            });

        assertThat(cacheManager.getCache(QueryCacheConfiguration.SCORE_RANKINGS).get("key"))
            .isNull();
        assertThat(cacheManager.getCache(QueryCacheConfiguration.OFFICIAL_MARKET).get("key"))
            .isNull();
        assertThat(cacheManager.getCache(QueryCacheConfiguration.PLAYER_CATALOG).get("key"))
            .isNotNull();
        assertThat(cacheManager.getCache(QueryCacheConfiguration.PLAYER_CATALOG_FILTERS).get("key"))
            .isNotNull();
        assertThat(cacheManager.getCache(QueryCacheConfiguration.PLAYER_DETAILS).get("key"))
            .isNotNull();
        assertThat(cacheManager.getCache(QueryCacheConfiguration.P2P_MARKET).get("key"))
            .isNotNull();
      }
    }

    @Test
    @DisplayName("Aplica la invalidación cuando el evento no participa de una transacción")
    void clearsNamedCacheWithoutTransaction() {
      try (AnnotationConfigApplicationContext context = createContext()) {
        CacheManager cacheManager = context.getBean(CacheManager.class);
        putEntryInEveryCache(cacheManager);

        context.publishEvent(
            new QueryCacheInvalidationEvent(Set.of(QueryCacheConfiguration.P2P_MARKET)));

        assertThat(cacheManager.getCache(QueryCacheConfiguration.P2P_MARKET).get("key")).isNull();
        assertThat(cacheManager.getCache(QueryCacheConfiguration.SCORE_RANKINGS).get("key"))
            .isNotNull();
        assertThat(cacheManager.getCache(QueryCacheConfiguration.PLAYER_CATALOG_FILTERS).get("key"))
            .isNotNull();
        assertThat(cacheManager.getCache(QueryCacheConfiguration.PLAYER_CATALOG).get("key"))
            .isNotNull();
        assertThat(cacheManager.getCache(QueryCacheConfiguration.PLAYER_DETAILS).get("key"))
            .isNotNull();
        assertThat(cacheManager.getCache(QueryCacheConfiguration.OFFICIAL_MARKET).get("key"))
            .isNotNull();
      }
    }
  }

  private void putEntryInEveryCache(CacheManager cacheManager) {
    cacheManager.getCache(QueryCacheConfiguration.SCORE_RANKINGS).put("key", "value");
    cacheManager.getCache(QueryCacheConfiguration.PLAYER_CATALOG_FILTERS).put("key", "value");
    cacheManager.getCache(QueryCacheConfiguration.PLAYER_CATALOG).put("key", "value");
    cacheManager.getCache(QueryCacheConfiguration.PLAYER_DETAILS).put("key", "value");
    cacheManager.getCache(QueryCacheConfiguration.OFFICIAL_MARKET).put("key", "value");
    cacheManager.getCache(QueryCacheConfiguration.P2P_MARKET).put("key", "value");
  }

  private AnnotationConfigApplicationContext createContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(TestTransactionConfiguration.class);
    context.registerBean(
        "queryCacheManager",
        CacheManager.class,
        () -> new QueryCacheConfiguration().queryCacheManager());
    context.registerBean(QueryCacheInvalidationListener.class);
    context.registerBean(
        "testTransactionManager", PlatformTransactionManager.class, TestTransactionManager::new);
    context.refresh();
    return context;
  }

  @Configuration
  @EnableTransactionManagement
  static class TestTransactionConfiguration {}

  private static class TestTransactionManager extends AbstractPlatformTransactionManager {

    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {}

    @Override
    protected void doCommit(DefaultTransactionStatus status) {}

    @Override
    protected void doRollback(DefaultTransactionStatus status) {}
  }
}
