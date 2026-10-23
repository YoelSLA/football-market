package footballmarket.services.impl;

import footballmarket.models.PlayerImageSyncRun;
import footballmarket.models.PlayerImageSyncRunItem;
import footballmarket.models.enums.PlayerImageResolutionStatus;
import footballmarket.models.enums.PlayerImageSyncRunStatus;
import footballmarket.repositories.PlayerImageResolutionRepository;
import footballmarket.repositories.PlayerImageSyncRunItemRepository;
import footballmarket.repositories.PlayerImageSyncRunRepository;
import footballmarket.services.PlayerImageRunRecoveryService;
import footballmarket.services.exceptions.PlayerImageResolutionPersistenceException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Cierra atómicamente cada run huérfano sin perder los items ya terminados. */
@Service
public class PlayerImageRunRecoveryServiceImpl implements PlayerImageRunRecoveryService {
  private final PlayerImageSyncRunRepository runs;
  private final PlayerImageSyncRunItemRepository items;
  private final PlayerImageResolutionRepository resolutions;
  private final TransactionTemplate transaction;
  private final Clock clock = Clock.systemUTC();

  public PlayerImageRunRecoveryServiceImpl(
      PlayerImageSyncRunRepository runs,
      PlayerImageSyncRunItemRepository items,
      PlayerImageResolutionRepository resolutions,
      PlatformTransactionManager manager) {
    this.runs = runs;
    this.items = items;
    this.resolutions = resolutions;
    this.transaction = new TransactionTemplate(manager);
  }

  @Override
  public void recover() {
    try {
      for (PlayerImageSyncRun orphan : this.runs.findByStatus(PlayerImageSyncRunStatus.RUNNING)) {
        this.transaction.executeWithoutResult(status -> this.recoverRun(orphan.getId()));
      }
    } catch (RuntimeException failure) {
      throw new PlayerImageResolutionPersistenceException();
    }
  }

  private void recoverRun(Long id) {
    PlayerImageSyncRun run = this.runs.findById(id).orElseThrow();
    if (run.getStatus() != PlayerImageSyncRunStatus.RUNNING) {
      return;
    }
    Instant now = this.clock.instant();
    for (PlayerImageSyncRunItem item : this.items.findByRunIdAndFinishedAtIsNull(id)) {
      PlayerImageResolutionStatus durableState =
          this.resolutions.findById(item.getPlayer().getId()).orElseThrow().getStatus();
      item.interrupt(durableState, now);
      run.record(run.counters().interrupt());
    }
    run.fail(now, "INTERRUPTED_BY_RESTART");
    this.items.flush();
    this.runs.flush();
  }
}
