package footballmarket.features.statistics.infraestructure.scheduling;

import footballmarket.features.statistics.application.PlayerStatisticsSynchronizationService;
import footballmarket.features.statistics.application.exceptions.ScheduledStatisticsSyncException;
import footballmarket.features.statistics.application.exceptions.ScheduledStatisticsSyncException.Reason;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncRequest;
import footballmarket.features.valuation.application.ValuationEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Entrada batch deportiva: secuencia las fachadas sin acceder a persistencia ni cotizaciones. */
@Component
public class AutomaticSportsDataScheduler {
  private static final Logger LOG = LoggerFactory.getLogger(AutomaticSportsDataScheduler.class);
  private final PlayerStatisticsSynchronizationService statistics;
  private final ValuationEvaluationService evaluation;
  private final boolean enabled;

  public AutomaticSportsDataScheduler(
      PlayerStatisticsSynchronizationService statistics,
      ValuationEvaluationService evaluation,
      @Value("${whoscored.statistics.schedule.enabled:true}") boolean enabled) {
    this.statistics = statistics;
    this.evaluation = evaluation;
    this.enabled = enabled;
  }

  /** Espera cada operación; un fallo termina el disparo sin impedir ejecuciones posteriores. */
  @Scheduled(cron = "0 0 12 * * SUN", zone = "America/Argentina/Buenos_Aires")
  public void synchronize() {
    if (!this.enabled) {
      return;
    }
    String stage = "JSON";
    LOG.info("sports_data_scheduled_started");
    try {
      checkInterruption();
      boolean jsonPartial = this.statistics.synchronizeJsonScheduled();
      checkInterruption();
      stage = "SCRAPING";
      boolean scrapingPartial =
          this.statistics.synchronizeScrapingScheduled(new PlayerStatisticsSyncRequest(null));
      checkInterruption();
      stage = "EVALUATION";
      this.evaluation.evaluate();
      checkInterruption();
      LOG.info(
          "sports_data_scheduled_finished result={}",
          jsonPartial || scrapingPartial ? "PARTIAL" : "COMPLETED");
    } catch (RuntimeException failure) {
      if (isInterruption(failure)) {
        Thread.currentThread().interrupt();
      }
      String cause =
          failure instanceof ScheduledStatisticsSyncException scheduled
              ? scheduled.getReason().name()
              : failure.getClass().getSimpleName();
      boolean closeFailed =
          failure instanceof ScheduledStatisticsSyncException scheduled
              && (scheduled.getReason() == Reason.CLOSE_FAILED
                  || failure.getSuppressed().length > 0);
      LOG.error(
          "sports_data_scheduled_failed stage={} cause={} closeFailed={}",
          stage,
          cause,
          closeFailed);
    }
  }

  private static void checkInterruption() {
    if (Thread.currentThread().isInterrupted()) {
      throw new ScheduledStatisticsSyncException(Reason.EXECUTION_INTERRUPTED);
    }
  }

  /** Solo examina tipos y códigos controlados; nunca mensajes ni trazas de errores externos. */
  private static boolean isInterruption(Throwable failure) {
    Throwable cause = failure;
    for (int depth = 0; cause != null && depth < 32; depth++) {
      if (cause instanceof InterruptedException
          || (cause instanceof ScheduledStatisticsSyncException scheduled
              && (scheduled.getReason() == Reason.EXECUTION_INTERRUPTED
                  || scheduled.getReason() == Reason.INTERRUPTED))) {
        return true;
      }
      if (cause.getCause() == cause) {
        break;
      }
      cause = cause.getCause();
    }
    return false;
  }
}
