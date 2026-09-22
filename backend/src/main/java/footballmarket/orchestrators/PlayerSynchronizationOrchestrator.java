package footballmarket.orchestrators;

import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.services.FootballDataPlayerService;
import footballmarket.services.PlayerCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Coordina la obtención y aplicación atómica de una foto completa de jugadores. */
@Component
@RequiredArgsConstructor
public class PlayerSynchronizationOrchestrator {

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
    var snapshot = this.footballDataPlayerService.fetchSnapshot();
    return this.playerCatalogService.applySynchronization(snapshot);
  }
}
