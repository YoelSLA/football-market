package footballmarket.services;

import footballmarket.models.records.PlayerStatisticsSyncRequest;
import footballmarket.models.records.PlayerStatisticsSyncSummary;

/**
 * Fachada del caso de uso de estadísticas: la ruta HTTP determina el canal.
 *
 * <p>La entrada de scraping admite fecha y máximo opcionales y ejecuta un lote acotado. La entrada
 * JSON se expone sin argumentos funcionales y su implementación pertenece a la actualización por
 * interfaz JSON del proveedor, no a este canal.
 */
public interface PlayerStatisticsSynchronizationService {
  PlayerStatisticsSyncSummary synchronizeScraping(PlayerStatisticsSyncRequest request);

  PlayerStatisticsSyncSummary synchronizeJson();
}
