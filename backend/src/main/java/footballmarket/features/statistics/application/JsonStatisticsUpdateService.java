package footballmarket.features.statistics.application;

import footballmarket.integrations.whoscored.WhoScoredRequestPacer;

/**
 * Actualización JSON de un jugador: independiente del canal de scraping y sin corte de lote.
 *
 * <p>El ritmo del canal se aplica por worker, de modo que quien recorre la población entrega el
 * permiso de su propio hilo. El canal no consulta ni contrasta observaciones de ningún otro canal:
 * su resultado depende únicamente de lo publicado por el proveedor y de su propia confirmación.
 */
public interface JsonStatisticsUpdateService {
  Outcome update(Long runId, Long playerId, WhoScoredRequestPacer.StatisticsWorker worker);

  /**
   * Resultado de la actualización de un jugador.
   *
   * @param itemId resultado persistido de la extracción
   * @param unknownColumns se detectó al menos una columna ajena al catálogo cerrado
   */
  record Outcome(Long itemId, boolean unknownColumns) {}
}
