package footballmarket.features.images.application;

import footballmarket.features.images.domain.records.PlayerImageSyncSummary;

/** Caso de uso de sincronización manual y síncrona de imágenes del catálogo local. */
public interface PlayerImageSynchronizationService {
  PlayerImageSyncSummary synchronize(boolean force);
}
