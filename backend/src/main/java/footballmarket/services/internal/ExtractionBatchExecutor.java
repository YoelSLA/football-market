package footballmarket.services.internal;

import footballmarket.integrations.exceptions.WhoScoredStatisticsException;
import footballmarket.services.PlayerStatisticsExtractionService;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/** Recorre una instantánea completa sin máximo, estimaciones ni deadline total. */
@Component
public class ExtractionBatchExecutor {
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(ExtractionBatchExecutor.class);

  /** Intenta cada ID una vez; los fallos individuales ya auditados no detienen a los demás. */
  public Outcome execute(
      PlayerStatisticsExtractionService.Batch batch, Long runId, List<Long> selected, int workers) {
    AtomicBoolean interrupted = new AtomicBoolean();
    LOG.info(
        "Scraping run={} snapshot={} workers={} sin deadline total",
        runId,
        selected.size(),
        workers);
    java.util.concurrent.atomic.AtomicReference<String> failureCode =
        new java.util.concurrent.atomic.AtomicReference<>();
    try (ExtractionWorkerPool pool = ExtractionWorkerPool.open(workers, batch::releaseWorker)) {
      pool.forEach(
          selected,
          playerId -> {
            if (interrupted.get()) {
              return;
            }
            long started = System.nanoTime();
            try {
              batch.extract(runId, playerId);
            } catch (WhoScoredStatisticsException failure) {
              LOG.warn(
                  "Scraping run={} player={} worker={} code={} section={}",
                  runId,
                  playerId,
                  Thread.currentThread().getName(),
                  failure.getCode(),
                  failure.getSection());
              if ("EXTRACTION_INTERRUPTED".equals(failure.getCode())) {
                interrupted.set(true);
                failureCode.compareAndSet(null, "EXECUTION_INTERRUPTED");
              } else if ("HEADED_BROWSER_UNAVAILABLE".equals(failure.getCode())) {
                interrupted.set(true);
                failureCode.compareAndSet(null, "HEADED_BROWSER_UNAVAILABLE");
              }
              // Timeout, transporte, 403/404, estructura y 429 afectan al jugador.
              // El pacer aplica Retry-After antes de la siguiente navegación sin repetirlo.
            } catch (RuntimeException global) {
              interrupted.set(true);
              throw global;
            } finally {
              LOG.info(
                  "Scraping run={} player={} worker={} duraciónMs={}",
                  runId,
                  playerId,
                  Thread.currentThread().getName(),
                  java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            }
          });
    }
    return new Outcome(interrupted.get(), failureCode.get());
  }

  /** Un fallo global puede dejar IDs del snapshot sin intentar. */
  public record Outcome(boolean interrupted, String failureCode) {}
}
