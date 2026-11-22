package footballmarket.features.images.application.impl;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.images.application.PlayerImageSynchronizationService;
import footballmarket.features.images.application.exceptions.PlayerImageSyncInProgressException;
import footballmarket.features.images.application.exceptions.PlayerImageSynchronizationException;
import footballmarket.features.images.domain.models.PlayerImageSyncRun;
import footballmarket.features.images.domain.records.PlayerImageSyncSummary;
import footballmarket.features.images.infraestructure.PlayerImageSyncRunRepository;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Orquesta el ciclo de vida y la ejecución por lotes de la sincronización de imágenes. */
@Service
public class PlayerImageSynchronizationServiceImpl implements PlayerImageSynchronizationService {

  private static final Logger LOG =
      LoggerFactory.getLogger(PlayerImageSynchronizationServiceImpl.class);

  private final PlayerRepository players;
  private final PlayerImageSyncRunRepository runs;
  private final PlayerImageProcessor processor;
  private final TransactionTemplate transaction;
  private final AtomicBoolean executing = new AtomicBoolean();
  private final Clock clock = Clock.systemUTC();

  public PlayerImageSynchronizationServiceImpl(
      PlayerRepository players,
      PlayerImageSyncRunRepository runs,
      PlayerImageProcessor processor,
      PlatformTransactionManager manager) {
    this.players = players;
    this.runs = runs;
    this.processor = processor;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Override
  public PlayerImageSyncSummary synchronize(boolean force) {
    return this.synchronize(force, false, null, null);
  }

  @Override
  public PlayerImageSyncSummary synchronize(
      boolean force, boolean missingOnly, Integer limit, Long teamId) {
    LOG.info(
        "Inicia sincronización: force={}, missingOnly={}, limit={}, teamId={}",
        force,
        missingOnly,
        limit,
        teamId);

    if (!this.executing.compareAndSet(false, true)) {
      LOG.warn("Rechazado: ya existe otra sincronización activa");
      throw new PlayerImageSyncInProgressException();
    }

    Long runId = null;
    try {
      runId =
          this.transaction.execute(
              status ->
                  this.runs
                      .saveAndFlush(new PlayerImageSyncRun(force, this.clock.instant()))
                      .getId());

      this.processBatch(runId, force, missingOnly, limit, teamId);

      Long completedId = runId;
      PlayerImageSyncSummary summary =
          this.transaction.execute(
              status -> {
                PlayerImageSyncRun run = this.runs.findById(completedId).orElseThrow();
                run.complete(this.clock.instant());
                return run.summary();
              });

      LOG.info("Sincronización concluida: run={}, estado={}", runId, summary.status());
      return summary;

    } catch (RuntimeException exception) {
      LOG.error("Fallo global en sincronización: run={}", runId, exception);
      if (runId != null) {
        Long failedId = runId;
        try {
          this.transaction.executeWithoutResult(
              status ->
                  this.runs
                      .findById(failedId)
                      .ifPresent(run -> run.fail(this.clock.instant(), "GLOBAL_FAILURE")));
        } catch (RuntimeException persistenceFailure) {
          LOG.error("No se pudo cerrar el run fallido: run={}", failedId, persistenceFailure);
        }
      }
      throw new PlayerImageSynchronizationException();
    } finally {
      this.executing.set(false);
    }
  }

  private void processBatch(
      Long runId, boolean force, boolean missingOnly, Integer limit, Long teamId) {
    long cursor = 0;
    int processedCount = 0;
    final int batchSize = 50;

    while (true) {
      if (limit != null && processedCount >= limit) {
        LOG.info("Límite alcanzado: procesados={}/{}", processedCount, limit);
        break;
      }

      int currentFetchSize =
          limit != null ? Math.min(batchSize, limit - processedCount) : batchSize;
      long fromId = cursor;

      List<Long> playerIds =
          this.transaction.execute(
              status ->
                  missingOnly || teamId != null
                      ? this.players.findEligiblePlayerIds(
                          fromId, missingOnly, teamId, PageRequest.of(0, currentFetchSize))
                      : this.players
                          .findFirstByIdGreaterThanOrderByIdAsc(fromId)
                          .map(Player::getId)
                          .map(List::of)
                          .orElse(List.of()));

      if (playerIds == null || playerIds.isEmpty()) {
        break;
      }

      for (Long playerId : playerIds) {
        if (limit != null && processedCount >= limit) {
          break;
        }
        cursor = playerId;
        boolean remoteProcessed = this.processor.processPlayer(runId, playerId, force);
        if (remoteProcessed) {
          processedCount++;
        }
      }
    }
  }
}
