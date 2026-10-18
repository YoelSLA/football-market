package footballmarket.services.impl;

import footballmarket.config.PlayerStatisticsProperties;
import footballmarket.integrations.WhoScoredRequestPacer;
import footballmarket.integrations.WhoScoredStatisticsIntegration;
import footballmarket.integrations.exceptions.HeadedBrowserUnavailableException;
import footballmarket.integrations.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.records.WhoScoredJsonQuery;
import footballmarket.models.enums.ExtractionRunStatus;
import footballmarket.models.enums.MetricSource;
import footballmarket.models.records.PlayerStatisticsSyncRequest;
import footballmarket.models.records.PlayerStatisticsSyncSummary;
import footballmarket.services.JsonStatisticsUpdateService;
import footballmarket.services.PlayerStatisticsExtractionService;
import footballmarket.services.PlayerStatisticsSynchronizationService;
import footballmarket.services.exceptions.PlayerStatisticsSynchronizationException;
import footballmarket.services.internal.ExtractionBatchExecutor;
import footballmarket.services.internal.ExtractionRunManager;
import footballmarket.services.internal.ExtractionWorkerPool;
import footballmarket.services.internal.PlayerStatisticsEligibilitySelector;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * Lote de scraping síncrono y reenrollable sobre la misma fecha y canal.
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

  public PlayerStatisticsSynchronizationServiceImpl(
      PlayerStatisticsProperties properties,
      PlayerStatisticsEligibilitySelector selector,
      PlayerStatisticsExtractionService extraction,
      ExtractionBatchExecutor batches,
      ExtractionRunManager runs,
      Clock clock,
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
    if (request == null) {
      throw new PlayerStatisticsSynchronizationException();
    }
    LocalDate today = LocalDate.now(this.clock.withZone(ZoneOffset.UTC));
    LocalDate extractionDate = request.extractionDate() == null ? today : request.extractionDate();
    if (extractionDate.isAfter(today)
        || (request.maxPlayers() != null && request.maxPlayers() < 1)) {
      throw new PlayerStatisticsSynchronizationException();
    }
    long budgetMillis = this.properties.scrapingBatchTimeoutMs();
    Long runId =
        this.runs
            .open(extractionDate, MetricSource.SCRAPING, request.maxPlayers(), budgetMillis)
            .getId();
    boolean pending =
        !this.selector.pendingPlayerIds(extractionDate, MetricSource.SCRAPING, 1).isEmpty();
    if (!pending) {
      return this.runs.close(runId, ExtractionRunStatus.COMPLETED, null, 0);
    }
    Instant deadline = Instant.now().truncatedTo(ChronoUnit.MICROS).plusMillis(budgetMillis);
    try (PlayerStatisticsExtractionService.Batch batch = this.extraction.openBatch()) {
      ExtractionBatchExecutor.Outcome outcome =
          this.batches.execute(
              batch,
              runId,
              extractionDate,
              MetricSource.SCRAPING,
              this.properties.scrapingWorkers(),
              request.maxPlayers(),
              deadline);
      return this.runs.close(
          runId,
          outcome.interrupted() ? ExtractionRunStatus.INTERRUPTED : ExtractionRunStatus.COMPLETED,
          outcome.failureCode(),
          outcome.providerCalls());
    } catch (HeadedBrowserUnavailableException unavailable) {
      this.runs.close(runId, ExtractionRunStatus.INTERRUPTED, "HEADED_BROWSER_UNAVAILABLE", 0);
      throw unavailable;
    } catch (RuntimeException failure) {
      // Una ejecucion que queda abierta bloquea la siguiente hasta vencer su presupuesto. Cualquier
      // fallo inesperado cierra igualmente el lote, declarando lo que si se pudo confirmar.
      this.runs.close(runId, ExtractionRunStatus.INTERRUPTED, "EXECUTION_INTERRUPTED", 0);
      throw failure;
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
    LocalDate extractionDate = LocalDate.now(this.clock.withZone(ZoneOffset.UTC));
    Long runId =
        this.runs.open(extractionDate, MetricSource.JSON, null, JSON_RUN_BUDGET_MS).getId();
    List<Long> pending = this.selector.allPendingPlayerIds(extractionDate, MetricSource.JSON);
    if (pending.isEmpty()) {
      return this.runs.close(runId, ExtractionRunStatus.COMPLETED, null, 0);
    }
    AtomicBoolean unavailable = new AtomicBoolean();
    AtomicLong providerCalls = new AtomicLong();
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
              if (haltsCampaign(failure)) {
                unavailable.set(true);
              }
            }
          });
    }
    return this.runs.close(
        runId,
        unavailable.get() ? ExtractionRunStatus.INTERRUPTED : ExtractionRunStatus.COMPLETED,
        unavailable.get() ? "PROVIDER_UNAVAILABLE" : null,
        providerCalls.get());
  }

  /** Solo una indisponibilidad que afecta al proveedor entero abandona la campaña. */
  private static boolean haltsCampaign(WhoScoredStatisticsException failure) {
    return GLOBAL_STOP_CODES.contains(failure.getCode())
        || Integer.valueOf(RATE_LIMITED_STATUS).equals(failure.getProviderStatus());
  }
}
