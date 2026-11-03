package footballmarket.features.images.application.impl;

import footballmarket.features.images.application.PlayerImageAuditService;
import footballmarket.features.images.application.exceptions.PlayerImageRunNotFoundException;
import footballmarket.features.images.domain.models.PlayerImageSyncRun;
import footballmarket.features.images.domain.models.PlayerImageSyncRunItem;
import footballmarket.features.images.domain.records.PlayerImageSyncRunItemRecord;
import footballmarket.features.images.domain.records.PlayerImageSyncSummary;
import footballmarket.features.images.infraestructure.PlayerImageSyncRunItemRepository;
import footballmarket.features.images.infraestructure.PlayerImageSyncRunRepository;
import footballmarket.integrations.thesportsdb.config.TheSportsDbProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta resúmenes y detalles separados, sin exponer entidades persistidas a HTTP. */
@Service
public class PlayerImageAuditServiceImpl implements PlayerImageAuditService {
  private final PlayerImageSyncRunRepository runs;
  private final PlayerImageSyncRunItemRepository items;
  private final TheSportsDbProperties properties;
  private final Clock clock = Clock.systemUTC();

  public PlayerImageAuditServiceImpl(
      PlayerImageSyncRunRepository runs,
      PlayerImageSyncRunItemRepository items,
      TheSportsDbProperties properties) {
    this.runs = runs;
    this.items = items;
    this.properties = properties;
  }

  /** Aplica la retención configurable al consultar el historial sin tocar datos del jugador. */
  @Override
  @Transactional
  public Page<PlayerImageSyncSummary> getRuns(int page, int size) {
    this.purgeExpired();
    return this.runs
        .findAllByOrderByStartedAtDescIdDesc(PageRequest.of(page, size))
        .map(PlayerImageSyncRun::summary);
  }

  @Override
  @Transactional(readOnly = true)
  public PlayerImageSyncSummary getRun(Long id) {
    return this.runs.findById(id).orElseThrow(PlayerImageRunNotFoundException::new).summary();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PlayerImageSyncRunItemRecord> getItems(Long id, int page, int size) {
    if (!this.runs.existsById(id)) {
      throw new PlayerImageRunNotFoundException();
    }
    return this.items
        .findByRunIdOrderByIdAsc(id, PageRequest.of(page, size))
        .map(PlayerImageSyncRunItem::detail);
  }

  private void purgeExpired() {
    Integer days = this.properties.auditRetentionDays();
    if (days == null) {
      return;
    }
    Instant before = this.clock.instant().minus(days, ChronoUnit.DAYS);
    for (PlayerImageSyncRun run : this.runs.findByFinishedAtBefore(before)) {
      this.items.deleteByRunId(run.getId());
      this.runs.delete(run);
    }
  }
}
