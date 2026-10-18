package footballmarket.integrations;

import footballmarket.config.WhoScoredProperties;
import footballmarket.integrations.exceptions.WhoScoredIntegrationException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Permiso único por instancia durante cada solicitud, con espera fuera de persistencia. */
@Component
public class WhoScoredRequestPacer {
  /** Espera sustituible para comprobar tiempos sin sleeps reales en tests. */
  @FunctionalInterface
  public interface Waiter {
    void await(long milliseconds) throws InterruptedException;
  }

  private final long intervalMs;
  private final LongSupplier milliseconds;
  private final Waiter waiter;
  private final Clock clock;
  private long nextStart = Long.MIN_VALUE;
  private volatile long unavailableUntil = Long.MIN_VALUE;

  /** Estado de pacing exclusivo de un worker y un canal, con vida limitada al lote. */
  public static final class StatisticsWorker {
    private final long intervalMs;
    private long nextStart = Long.MIN_VALUE;

    private StatisticsWorker(long intervalMs) {
      this.intervalMs = intervalMs;
    }
  }

  public StatisticsWorker statisticsWorker(long intervalMs) {
    return new StatisticsWorker(intervalMs);
  }

  /** Permisos por worker: no mantiene el monitor global durante solicitudes estadísticas. */
  public <T> T executeStatistics(
      StatisticsWorker worker, java.time.Instant deadline, Supplier<T> request) {
    synchronized (worker) {
      try {
        long now = this.milliseconds.getAsLong();
        long start = Math.max(worker.nextStart, this.unavailableUntil);
        while (start > now) {
          long remaining =
              deadline == null
                  ? Long.MAX_VALUE
                  : java.time.Duration.between(this.clock.instant(), deadline).toMillis();
          if (remaining <= start - now) {
            throw new footballmarket.integrations.exceptions.WhoScoredStatisticsException(
                "EXTRACTION_INTERRUPTED", null);
          }
          this.waiter.await(start - now);
          now = this.milliseconds.getAsLong();
          start = Math.max(worker.nextStart, this.unavailableUntil);
        }
        if (Thread.currentThread().isInterrupted()) {
          throw new InterruptedException();
        }
        if (deadline != null && !this.clock.instant().isBefore(deadline)) {
          throw new footballmarket.integrations.exceptions.WhoScoredStatisticsException(
              "EXTRACTION_INTERRUPTED", null);
        }
        worker.nextStart = now + worker.intervalMs;
        return request.get();
      } catch (InterruptedException failure) {
        Thread.currentThread().interrupt();
        throw new footballmarket.integrations.exceptions.WhoScoredStatisticsException(
            "EXTRACTION_INTERRUPTED", null);
      }
    }
  }

  @Autowired
  public WhoScoredRequestPacer(WhoScoredProperties properties) {
    this(
        properties.requestIntervalMs(),
        () -> System.nanoTime() / 1_000_000,
        Thread::sleep,
        Clock.systemUTC());
  }

  public WhoScoredRequestPacer(
      long intervalMs, LongSupplier milliseconds, Waiter waiter, Clock clock) {
    this.intervalMs = intervalMs;
    this.milliseconds = milliseconds;
    this.waiter = waiter;
    this.clock = clock;
  }

  /** Adquiere un único permiso y mantiene exclusión entre solicitudes del mismo cliente. */
  public synchronized <T> T execute(Supplier<T> request) {
    try {
      long now = this.milliseconds.getAsLong();
      long deadline = Math.max(this.nextStart, this.unavailableUntil);
      while (deadline > now) {
        this.waiter.await(deadline - now);
        now = this.milliseconds.getAsLong();
      }
      if (Thread.currentThread().isInterrupted()) {
        throw new InterruptedException();
      }
      this.nextStart = now + this.intervalMs;
      return request.get();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new WhoScoredIntegrationException("EXECUTION_INTERRUPTED", null, true);
    }
  }

  /** Aplaza solicitudes posteriores; nunca repite la solicitud fallida. */
  public synchronized void defer(String retryAfter) {
    if (retryAfter == null) {
      return;
    }
    try {
      long delay;
      if (retryAfter.matches("[0-9]+")) {
        delay = Math.multiplyExact(Long.parseLong(retryAfter), 1000L);
      } else {
        delay =
            ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant()
                    .toEpochMilli()
                - this.clock.millis();
      }
      this.unavailableUntil =
          Math.max(
              this.unavailableUntil,
              Math.addExact(this.milliseconds.getAsLong(), Math.max(0, delay)));
    } catch (RuntimeException invalidHeader) {
      // Un header inválido no modifica el ritmo configurado ni dispara reintentos.
    }
  }
}
