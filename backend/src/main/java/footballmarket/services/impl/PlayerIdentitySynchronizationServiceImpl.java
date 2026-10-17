package footballmarket.services.impl;

import footballmarket.models.*;
import footballmarket.models.enums.*;
import footballmarket.models.records.*;
import footballmarket.repositories.*;
import footballmarket.services.PlayerIdentitySynchronizationService;
import footballmarket.services.exceptions.PlayerIdentitySourceNotEligibleException;
import footballmarket.services.exceptions.PlayerIdentitySynchronizationException;
import footballmarket.services.internal.*;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Fachada de ambos pases; la red y el matching no mantienen transacciones de persistencia. */
@Service
public class PlayerIdentitySynchronizationServiceImpl
    implements PlayerIdentitySynchronizationService {
  private final PlayerIdentitySnapshotCapture snapshot;
  private final PlayerIdentityLeagueDiscoveryProcessor discovery;
  private final PlayerIdentityResolutionProcessor resolution;
  private final PlayerIdentitySyncRunRepository runs;
  private final PlayerIdentitySyncItemRepository items;
  private final PlayerIdentityLeagueDiscoveryRepository leagues;
  private final WhoScoredIdentityCandidateRepository candidates;
  private final PlayerIdentityDiscardedRowRepository discarded;
  private final TransactionTemplate transaction;

  public PlayerIdentitySynchronizationServiceImpl(
      PlayerIdentitySnapshotCapture snapshot,
      PlayerIdentityLeagueDiscoveryProcessor discovery,
      PlayerIdentityResolutionProcessor resolution,
      PlayerIdentitySyncRunRepository runs,
      PlayerIdentitySyncItemRepository items,
      PlayerIdentityLeagueDiscoveryRepository leagues,
      WhoScoredIdentityCandidateRepository candidates,
      PlayerIdentityDiscardedRowRepository discarded,
      PlatformTransactionManager manager) {
    this.snapshot = snapshot;
    this.discovery = discovery;
    this.resolution = resolution;
    this.runs = runs;
    this.items = items;
    this.leagues = leagues;
    this.candidates = candidates;
    this.discarded = discarded;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Override
  public PlayerIdentitySyncSummary synchronizeCurrent() {
    return this.synchronize(this.snapshot.current());
  }

  @Override
  public PlayerIdentitySyncSummary synchronizeHistorical(Long sourceRunId) {
    if (sourceRunId == null || sourceRunId < 1) {
      throw new PlayerIdentitySourceNotEligibleException();
    }
    return this.synchronize(this.snapshot.historical(sourceRunId));
  }

  private PlayerIdentitySyncSummary synchronize(PlayerIdentitySnapshotCapture.Capture capture) {
    boolean interrupted = false;
    try {
      interrupted = this.discovery.discover(capture);
      interrupted = this.resolution.resolve(capture, interrupted);
      boolean finalWasInterrupted = interrupted;
      return this.transaction.execute(status -> this.finish(capture.runId(), finalWasInterrupted));
    } catch (RuntimeException failure) {
      throw new PlayerIdentitySynchronizationException();
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private PlayerIdentitySyncSummary finish(Long runId, boolean interrupted) {
    PlayerIdentitySyncRun run = this.runs.findById(runId).orElseThrow();
    List<PlayerIdentityLeagueDiscovery> scopes = this.leagues.findByRunIdOrderById(runId);
    Map<PlayerIdentityItemResult, Long> results = new EnumMap<>(PlayerIdentityItemResult.class);
    this.items
        .countResults(runId)
        .forEach(result -> results.put(result.getResult(), result.getAmount()));
    long failed =
        scopes.stream()
            .filter(scope -> scope.getStatus() == PlayerIdentityLeagueStatus.FAILED)
            .count();
    long completed =
        scopes.stream()
            .filter(scope -> scope.getStatus() == PlayerIdentityLeagueStatus.COMPLETED)
            .count();
    PlayerIdentitySyncCounters counters =
        PlayerIdentitySyncCounters.from(
            results,
            this.candidates.countByRunId(runId),
            failed,
            this.discarded.countByRunId(runId));
    run.finish(
        counters,
        completed,
        Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS),
        interrupted ? "EXECUTION_INTERRUPTED" : null);
    this.runs.flush();
    return new PlayerIdentitySyncSummary(
        run.getId(),
        run.getPassType(),
        run.getSourceRunId(),
        run.getStartedAt(),
        run.getFinishedAt(),
        run.getStatus(),
        run.getFailureReason(),
        counters,
        scopes);
  }
}
