package footballmarket.orchestrators;

import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.services.FootballDataPlayerService;
import footballmarket.services.PlayerCatalogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
/** Coordina la obtención y aplicación atómica de una foto completa de jugadores. */
public class PlayerSynchronizationOrchestrator {

  private static final Logger LOG =
      LoggerFactory.getLogger(PlayerSynchronizationOrchestrator.class);

  private final FootballDataPlayerService footballDataPlayerService;
  private final PlayerCatalogService playerCatalogService;

  /**
   * Coordina la obtención y aplicación de una foto completa de jugadores.
   *
   * <p>La ejecución se serializa dentro de la instancia actual de la aplicación para evitar que dos
   * sincronizaciones se ejecuten simultáneamente.
   *
   * @return resultado de la sincronización con las cantidades de jugadores obtenidos, creados,
   *     actualizados, inactivados y descartados
   */
  public synchronized PlayerSynchronizationResult synchronize() {
    LOG.info("Inicio de sincronización manual de jugadores");
    var snapshot = this.footballDataPlayerService.fetchSnapshot();
    var result = this.playerCatalogService.applySynchronization(snapshot);

    LOG.info(
        "Sincronización completada: obtenidos={}, creados={}, actualizados={}, inactivados={}, descartados={}",
        result.obtained(),
        result.created(),
        result.updated(),
        result.markedInactive(),
        result.discardedInvalid());
    return result;
  }
}
