package footballmarket.services;

import footballmarket.models.records.PlayerImageSyncSummary;

/** Caso de uso de sincronización manual y síncrona de imágenes del catálogo local. */
public interface PlayerImageSynchronizationService {
  PlayerImageSyncSummary synchronize(boolean force);
}
