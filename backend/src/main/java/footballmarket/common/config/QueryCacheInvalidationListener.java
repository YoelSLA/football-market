package footballmarket.common.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Aplica las invalidaciones internas de caché al finalizar la actualización correspondiente. */
@Component
public class QueryCacheInvalidationListener {

  private final CacheManager cacheManager;

  /**
   * Crea el listener con el administrador local de cachés.
   *
   * @param cacheManager administrador de cachés configurado por la aplicación
   */
  public QueryCacheInvalidationListener(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  /**
   * Limpia únicamente las cachés indicadas cuando la transacción confirmó su commit.
   *
   * @param event evento con los nombres de caché afectados
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void invalidate(QueryCacheInvalidationEvent event) {
    event.cacheNames().stream()
        .map(cacheManager::getCache)
        .filter(cache -> cache != null)
        .forEach(Cache::clear);
  }
}
