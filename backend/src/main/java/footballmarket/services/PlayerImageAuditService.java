package footballmarket.services;

import footballmarket.models.records.PlayerImageSyncRunItemRecord;
import footballmarket.models.records.PlayerImageSyncSummary;
import org.springframework.data.domain.Page;

/** Lectura paginada de auditoría y retención de ejecuciones anteriores al plazo configurado. */
public interface PlayerImageAuditService {
  Page<PlayerImageSyncSummary> getRuns(int page, int size);

  PlayerImageSyncSummary getRun(Long id);

  Page<PlayerImageSyncRunItemRecord> getItems(Long id, int page, int size);
}
