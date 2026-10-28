package footballmarket.features.identity.application;

import footballmarket.features.identity.domain.records.PlayerIdentitySyncSummary;

/** Identificación manual actual e histórica con auditoría nueva en cada invocación. */
public interface PlayerIdentitySynchronizationService {
  PlayerIdentitySyncSummary synchronizeCurrent();

  /** Resuelve solo pendientes aún sin referencia de un actual terminado COMPLETED o PARTIAL. */
  PlayerIdentitySyncSummary synchronizeHistorical(Long sourceRunId);
}
