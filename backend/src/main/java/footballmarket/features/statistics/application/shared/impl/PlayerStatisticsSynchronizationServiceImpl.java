package footballmarket.features.statistics.application.shared.impl;

import footballmarket.features.statistics.application.JsonStatisticsUpdateService;
import footballmarket.features.statistics.application.PlayerStatisticsExtractionService;
import footballmarket.features.statistics.application.PlayerStatisticsSynchronizationService;
import footballmarket.features.statistics.application.exceptions.PlayerStatisticsSynchronizationException;
import footballmarket.features.statistics.application.exceptions.ScheduledStatisticsSyncException;
import footballmarket.features.statistics.application.exceptions.ScheduledStatisticsSyncException.Reason;
import footballmarket.features.statistics.application.scraping.internal.ExtractionBatchExecutor;
import footballmarket.features.statistics.application.shared.internal.ExtractionRunManager;
import footballmarket.features.statistics.application.shared.internal.ExtractionWorkerPool;
import footballmarket.features.statistics.application.shared.internal.PlayerStatisticsEligibilitySelector;
import footballmarket.features.statistics.domain.enums.ExtractionRunStatus;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncRequest;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncSummary;
import footballmarket.features.statistics.infraestructure.config.PlayerStatisticsProperties;
import footballmarket.integrations.whoscored.WhoScoredRequestPacer;
import footballmarket.integrations.whoscored.WhoScoredStatisticsIntegration;
import footballmarket.integrations.whoscored.exceptions.HeadedBrowserUnavailableException;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.whoscored.records.WhoScoredJsonQuery;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Recorrido síncrono de todos los pendientes de una fecha y canal.
 *
 * <p>El navegador solo se abre cuando existe al menos un pendiente real, de modo que cero trabajo
 * se resuelve sin acceso al proveedor. Ninguna transacción permanece abierta durante el recorrido.
 */
@Service
public class PlayerStatisticsSynchronizationServiceImpl
    implements PlayerStatisticsSynchronizationService {
  /**
   * JSON no corta por tiempo: el presupuesto solo delimita la recuperación de una ejecución activa.
   */
  private static final long JSON_RUN_BUDGET_MS = 86_400_000L;

  /**
   * Códigos que hacen abandonar la campaña completa y no solo el jugador que los sufrió.
   *
   * <p>El bloqueo del proveedor no se corrige reintentando otros jugadores. Un timeout, un corte de
   * red o un error 5xx puntual son de un jugador: se registran como fallo suyo y el recorrido
   * sigue, porque la siguiente invocación los vuelve a seleccionar como pendientes.
   *
   * <p>El límite de peticiones se reconoce por el estado 429 y no por su código porque la excepción
   * de límite se declara como {@code PROVIDER_TRANSPORT_ERROR}, indistinguible de un error de red
   * por código.
   */
  private static final Set<String> GLOBAL_STOP_CODES = Set.of("PROVIDER_BLOCKED");

  private static final int RATE_LIMITED_STATUS = 429;

  private final PlayerStatisticsProperties properties;
  private final PlayerStatisticsEligibilitySelector selector;
  private final PlayerStatisticsExtractionService extraction;
  private final ExtractionBatchExecutor batches;
  private final ExtractionRunManager runs;
  private final Clock clock;
  private final JsonStatisticsUpdateService jsonUpdates;
  private final WhoScoredStatisticsIntegration jsonIntegration;
  private final java.util.concurrent.locks.ReentrantLock scrapingLock =
      new java.util.concurrent.locks.ReentrantLock();

  public PlayerStatisticsSynchronizationServiceImpl(
      PlayerStatisticsProperties properties,
      PlayerStatisticsEligibilitySelector selector,
      PlayerStatisticsExtractionService extraction,
      ExtractionBatchExecutor batches,
      ExtractionRunManager runs,
      @Qualifier("statisticsClock") Clock clock,
      JsonStatisticsUpdateService jsonUpdates,
      WhoScoredStatisticsIntegration jsonIntegration) {
    this.properties = properties;
    this.selector = selector;
    this.extraction = extraction;
    this.batches = batches;
    this.runs = runs;
    this.clock = clock;
    this.jsonUpdates = jsonUpdates;
    this.jsonIntegration = jsonIntegration;
  }

  @Override
  public PlayerStatisticsSyncSummary synchronizeScraping(PlayerStatisticsSyncRequest request) {
    return this.synchronizeScraping(request, false);
  }

  @Override
  public boolean synchronizeScrapingScheduled(PlayerStatisticsSyncRequest request) {
    return this.synchronizeScraping(request, true).status() == ExtractionRunStatus.PARTIAL;
  }

  private PlayerStatisticsSyncSummary synchronizeScraping(
      PlayerStatisticsSyncRequest request, boolean scheduled) {
    if (request == null) {
      throw new PlayerStatisticsSynchronizationException();
    }
    LocalDate today = LocalDate.now(this.clock.withZone(ZoneOffset.UTC));
    LocalDate extractionDate = request.extractionDate() == null ? today : request.extractionDate();
    if (extractionDate.isAfter(today)) {
      throw new PlayerStatisticsSynchronizationException();
    }
    if (!this.scrapingLock.tryLock()) {
      throw new footballmarket.features.statistics.application.exceptions
          .PlayerStatisticsSyncInProgressException();
    }
    try {
      Long runId = this.runs.open(extractionDate, MetricSource.SCRAPING, 0).getId();
      PlayerStatisticsExtractionService.Batch batch = null;
      RuntimeException failure = null;
      ExtractionBatchExecutor.Outcome outcome = null;
      try {
        List<Long> pending =
            this.selector.allPendingPlayerIds(extractionDate, MetricSource.SCRAPING);
        if (!pending.isEmpty()) {
          batch = this.extraction.openBatch();
          try (PlayerStatisticsExtractionService.Batch resources = batch) {
            outcome =
                this.batches.execute(resources, runId, pending, this.properties.scrapingWorkers());
          }
        }
      } catch (RuntimeException error) {
        if (error instanceof HeadedBrowserUnavailableException
            || error instanceof PlayerStatisticsSynchronizationException) {
          failure = error;
        } else {
          failure = new PlayerStatisticsSynchronizationException();
          failure.initCause(error);
        }
      }
      String code =
          failure instanceof HeadedBrowserUnavailableException
              ? "HEADED_BROWSER_UNAVAILABLE"
              : failure != null
                  ? "EXECUTION_INTERRUPTED"
                  : outcome == null ? null : outcome.failureCode();
      PlayerStatisticsSyncSummary summary;
      try {
        summary =
            this.closeRun(
                runId,
                failure != null || (outcome != null && outcome.interrupted())
                    ? ExtractionRunStatus.INTERRUPTED
                    : ExtractionRunStatus.COMPLETED,
                code,
                batch == null ? 0 : batch.providerCalls(),
                scheduled && failure == null);
      } catch (RuntimeException auditFailure) {
        if (failure != null) {
          if (scheduled) {
            Reason reason =
                failure instanceof HeadedBrowserUnavailableException
                    ? Reason.HEADED_BROWSER_UNAVAILABLE
                    : Thread.currentThread().isInterrupted()
                        ? Reason.EXECUTION_INTERRUPTED
                        : Reason.EXECUTION_FAILED;
            ScheduledStatisticsSyncException signal = new ScheduledStatisticsSyncException(reason);
            signal.initCause(failure);
            signal.addSuppressed(auditFailure);
            throw signal;
          }
          failure.addSuppressed(auditFailure);
          throw failure;
        }
        throw auditFailure;
      }
      if (failure != null) {
        throw failure;
      }
      return summary;
    } finally {
      this.scrapingLock.unlock();
    }
  }

  /**
   * Entrada JSON sin argumentos funcionales.
   *
   * <p>La fecha UTC se fija al inicio y se conserva aunque el lote cruce medianoche. Recorre todos
   * los pendientes de la instantánea de elegibles en una sola pasada, por ID local ascendente, sin
   * {@code maxPlayers}, sin corte temporal de lote y sin navegador. Cero pendientes significa
   * ejecución completada sin tocar al proveedor.
   */
  @Override
  public PlayerStatisticsSyncSummary synchronizeJson() {
    return this.synchronizeJson(false);
  }

  @Override
  public boolean synchronizeJsonScheduled() {
    return this.synchronizeJson(true).status() == ExtractionRunStatus.PARTIAL;
  }

  private PlayerStatisticsSyncSummary synchronizeJson(boolean scheduled) {
    LocalDate extractionDate = LocalDate.now(this.clock.withZone(ZoneOffset.UTC));
    Long runId = this.runs.open(extractionDate, MetricSource.JSON, JSON_RUN_BUDGET_MS).getId();
    AtomicBoolean unavailable = new AtomicBoolean();
    AtomicBoolean interrupted = new AtomicBoolean();
    AtomicLong providerCalls = new AtomicLong();
    try {
      List<Long> pending = this.selector.allPendingPlayerIds(extractionDate, MetricSource.JSON);
      if (!pending.isEmpty()) {
        this.updateJsonPlayers(runId, pending, scheduled, unavailable, interrupted, providerCalls);
      }
    } catch (RuntimeException failure) {
      if (!scheduled) {
        throw failure;
      }
      throw this.abortJsonRun(runId, failure, providerCalls.get());
    }
    return this.closeRun(
        runId,
        unavailable.get() ? ExtractionRunStatus.INTERRUPTED : ExtractionRunStatus.COMPLETED,
        interrupted.get()
            ? "EXECUTION_INTERRUPTED"
            : unavailable.get() ? "PROVIDER_UNAVAILABLE" : null,
        providerCalls.get(),
        scheduled);
  }

  private void updateJsonPlayers(
      Long runId,
      List<Long> pending,
      boolean scheduled,
      AtomicBoolean unavailable,
      AtomicBoolean interrupted,
      AtomicLong providerCalls) {
    try (ExtractionWorkerPool pool = ExtractionWorkerPool.open(this.properties.jsonWorkers())) {
      java.util.concurrent.ConcurrentHashMap<Long, WhoScoredRequestPacer.StatisticsWorker> workers =
          new java.util.concurrent.ConcurrentHashMap<>();
      pool.forEach(
          pending,
          playerId -> {
            if (unavailable.get()) {
              return;
            }
            providerCalls.addAndGet(WhoScoredJsonQuery.COMBINATIONS.size());
            WhoScoredRequestPacer.StatisticsWorker worker =
                workers.computeIfAbsent(
                    Thread.currentThread().getId(), ignored -> this.jsonIntegration.newWorker());
            try {
              this.jsonUpdates.update(runId, playerId, worker);
            } catch (WhoScoredStatisticsException failure) {
              if (scheduled && "EXTRACTION_INTERRUPTED".equals(failure.getCode())) {
                interrupted.set(true);
                unavailable.set(true);
              }
              if (haltsCampaign(failure)) {
                unavailable.set(true);
              }
            }
          });
    }
  }

  /** El try-with-resources del recorrido ya terminó antes de intentar el cierre transaccional. */
  private ScheduledStatisticsSyncException abortJsonRun(
      Long runId, RuntimeException failure, long providerCalls) {
    boolean interrupted = Thread.interrupted();
    Reason reason = interrupted ? Reason.EXECUTION_INTERRUPTED : Reason.EXECUTION_FAILED;
    ScheduledStatisticsSyncException signal = new ScheduledStatisticsSyncException(reason);
    signal.initCause(failure);
    try {
      this.runs.abortScheduled(runId, reason.name(), providerCalls);
    } catch (RuntimeException closeFailure) {
      signal.addSuppressed(closeFailure);
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
    return signal;
  }

  private PlayerStatisticsSyncSummary closeRun(
      Long runId,
      ExtractionRunStatus requested,
      String failureCode,
      long providerCalls,
      boolean scheduled) {
    if (!scheduled) {
      return this.runs.close(runId, requested, failureCode, providerCalls);
    }
    try {
      return this.runs.closeScheduled(runId, requested, failureCode, providerCalls);
    } catch (ScheduledStatisticsSyncException signal) {
      throw signal;
    } catch (RuntimeException closeFailure) {
      ScheduledStatisticsSyncException signal =
          new ScheduledStatisticsSyncException(Reason.CLOSE_FAILED);
      signal.initCause(closeFailure);
      throw signal;
    }
  }

  /** Solo una indisponibilidad que afecta al proveedor entero abandona la campaña. */
  private static boolean haltsCampaign(WhoScoredStatisticsException failure) {
    return GLOBAL_STOP_CODES.contains(failure.getCode())
        || Integer.valueOf(RATE_LIMITED_STATUS).equals(failure.getProviderStatus());
  }
}
