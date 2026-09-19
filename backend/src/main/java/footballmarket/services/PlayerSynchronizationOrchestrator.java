package footballmarket.services;

import footballmarket.models.PlayerSynchronizationResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerSynchronizationOrchestrator {
  private static final Logger log =
      LoggerFactory.getLogger(PlayerSynchronizationOrchestrator.class);
  private final FootballDataPlayerService source;
  private final PlayerCatalogService catalog;

  /** Serializa la obtención y aplicación dentro de la única instancia de la aplicación. */
  public synchronized PlayerSynchronizationResult synchronize() {
    log.info("Inicio de sincronización de jugadores");
    try {
      var snapshot = source.fetchSnapshot();
      var result = catalog.applySynchronization(snapshot);
      log.info(
          "Sincronización completada: obtained={}, created={}, updated={}, markedInactive={}, discardedInvalid={}",
          result.obtained(),
          result.created(),
          result.updated(),
          result.markedInactive(),
          result.discardedInvalid());
      return result;
    } catch (RuntimeException ex) {
      log.warn("Sincronización fallida: no se pudo obtener o aplicar la foto completa");
      throw ex;
    }
  }
}
