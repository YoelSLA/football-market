package footballmarket.features.identity.application;

import footballmarket.features.identity.domain.records.PlayerIdentitySyncSummary;

/** Identificación manual actual e histórica en una sola operación, sin auditoría persistida. */
public interface PlayerIdentitySynchronizationService {
  PlayerIdentitySyncSummary synchronize();
}
