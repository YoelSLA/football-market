package footballmarket.orchestrators;

import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.services.FootballDataPlayerService;
import footballmarket.services.PlayerCatalogService;
import footballmarket.services.TeamEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Coordina la obtención y aplicación atómica de una foto completa de jugadores. */
@Component
@RequiredArgsConstructor
public class PlayerSynchronizationOrchestrator {
  private static final Logger LOG =
      LoggerFactory.getLogger(PlayerSynchronizationOrchestrator.class);

  private final FootballDataPlayerService footballDataPlayerService;
  private final PlayerCatalogService playerCatalogService;
  private final TeamEnrichmentService teamEnrichmentService;

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
    PlayerSnapshot snapshot = this.footballDataPlayerService.fetchSnapshot();
    PlayerSynchronizationResult result = this.playerCatalogService.applySynchronization(snapshot);
    try {
      this.teamEnrichmentService.enrichCurrentTeams();
    } catch (RuntimeException exception) {
      LOG.warn("Enriquecimiento independiente fallido; sincronización principal confirmada");
    }
    return result;
  }
}
