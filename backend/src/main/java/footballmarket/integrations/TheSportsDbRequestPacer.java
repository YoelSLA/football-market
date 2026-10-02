package footballmarket.integrations;

import footballmarket.integrations.exceptions.InvalidTheSportsDbConfigurationException;
import footballmarket.integrations.exceptions.TheSportsDbUnavailableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Un solo control por instancia para el inicio de todas las solicitudes al proveedor. */
@Component
public class TheSportsDbRequestPacer {
  /** Espera reemplazable en tests sin dormir realmente. */
  @FunctionalInterface
  public interface Waiter {
    void waitFor(Duration duration) throws InterruptedException;
  }

  private final Clock clock;
  private final Waiter waiter;
  private final long intervalMs;
  private Instant nextAllowed;

  @Autowired
  public TheSportsDbRequestPacer(footballmarket.config.TheSportsDbProperties properties) {
    this(
        properties.requestIntervalMs(),
        Clock.systemUTC(),
        duration -> Thread.sleep(duration.toMillis()));
  }

  public TheSportsDbRequestPacer(long intervalMs, Clock clock, Waiter waiter) {
    if (intervalMs < 2500 || clock == null || waiter == null) {
      throw new InvalidTheSportsDbConfigurationException();
    }
    this.intervalMs = intervalMs;
    this.clock = clock;
    this.waiter = waiter;
  }

  /** Reserva el próximo inicio, manteniendo la exclusión durante la espera. */
  public synchronized void beforeRequest() {
    Instant now = this.clock.instant();
    if (this.nextAllowed != null && now.isBefore(this.nextAllowed)) {
      try {
        this.waiter.waitFor(Duration.between(now, this.nextAllowed));
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new TheSportsDbUnavailableException();
      }
    }
    Instant started = this.clock.instant();
    this.nextAllowed = started.plusMillis(this.intervalMs);
  }

  /** Extiende la espera para el próximo intento cuando el servidor responde 429. */
  public synchronized void delayUntil(Instant retryAt) {
    if (retryAt != null && (this.nextAllowed == null || retryAt.isAfter(this.nextAllowed))) {
      this.nextAllowed = retryAt;
    }
  }
}
