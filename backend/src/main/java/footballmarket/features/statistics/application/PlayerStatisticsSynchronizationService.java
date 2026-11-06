package footballmarket.features.statistics.application;

import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncRequest;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncSummary;

/**
 * Fachada del caso de uso de estadísticas: la ruta HTTP determina el canal.
 *
 * <p>Scraping admite fecha opcional y JSON fija su propia fecha UTC. Ambos recorren sus pendientes.
 * Las entradas programadas conservan el recorrido manual y comunican los fallos globales aunque el
 * resumen manual sea parcial o completo.
 */
public interface PlayerStatisticsSynchronizationService {
  PlayerStatisticsSyncSummary synchronizeScraping(PlayerStatisticsSyncRequest request);

  PlayerStatisticsSyncSummary synchronizeJson();

  /** Devuelve true solo para un resultado parcial sin fallo global; no expone contadores. */
  boolean synchronizeJsonScheduled();

  /**
   * Conserva la fecha solicitada y devuelve true solo para un parcial sin fallo global. Los fallos
   * globales se comunican mediante excepción controlada, sin exponer contadores.
   */
  boolean synchronizeScrapingScheduled(PlayerStatisticsSyncRequest request);
}
