package footballmarket.services;

import footballmarket.models.records.PlayerIdentitySyncSummary;

/** Identificación manual actual e histórica con auditoría nueva en cada invocación. */
public interface PlayerIdentitySynchronizationService {
  PlayerIdentitySyncSummary synchronizeCurrent();

  /** Resuelve solo pendientes aún sin referencia de un actual terminado COMPLETED o PARTIAL. */
  PlayerIdentitySyncSummary synchronizeHistorical(Long sourceRunId);
}
