package footballmarket.common.config;

import java.util.Set;

/** Evento interno que identifica las cachés afectadas por una actualización completada. */
public record QueryCacheInvalidationEvent(Set<String> cacheNames) {

  /** Conserva un conjunto inmutable de nombres de caché. */
  public QueryCacheInvalidationEvent {
    cacheNames = Set.copyOf(cacheNames);
  }
}
