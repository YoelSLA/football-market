package footballmarket.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.integrations.exceptions.InvalidTheSportsDbConfigurationException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TheSportsDbRequestPacerTest {
  @Nested
  @DisplayName("Intervalo global y esperas 429")
  class GlobalInterval {
    @Test
    @DisplayName("Separa al menos 2500 ms los inicios de todas las peticiones")
    void separatesStarts() {
      MutableClock clock = new MutableClock(Instant.parse("2026-10-01T00:00:00Z"));
      List<Duration> waits = new ArrayList<>();
      TheSportsDbRequestPacer pacer =
          new TheSportsDbRequestPacer(
              2500,
              clock,
              duration -> {
                waits.add(duration);
                clock.advance(duration);
              });
      pacer.beforeRequest();
      pacer.beforeRequest();
      pacer.beforeRequest();
      assertThat(waits).containsExactly(Duration.ofMillis(2500), Duration.ofMillis(2500));
    }

    @Test
    @DisplayName("Rechaza un intervalo inferior al mínimo de la cuota")
    void rejectsFastConfiguration() {
      MutableClock clock = new MutableClock(Instant.parse("2026-10-01T00:00:00Z"));
      assertThatThrownBy(() -> new TheSportsDbRequestPacer(2499, clock, duration -> {}))
          .isInstanceOf(InvalidTheSportsDbConfigurationException.class);
    }

    @Test
    @DisplayName("Respeta el máximo entre Retry-After válido y el intervalo")
    void respectsRetryAfter() {
      MutableClock clock = new MutableClock(Instant.parse("2026-10-01T00:00:00Z"));
      List<Duration> waits = new ArrayList<>();
      TheSportsDbRequestPacer pacer =
          new TheSportsDbRequestPacer(
              2500,
              clock,
              duration -> {
                waits.add(duration);
                clock.advance(duration);
              });
      pacer.beforeRequest();
      pacer.delayUntil(clock.instant().plusSeconds(10));
      pacer.beforeRequest();
      assertThat(waits).containsExactly(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("Un 429 sin cabecera utilizable retrasa al menos 60 segundos")
    void waitsConservatively() {
      MutableClock clock = new MutableClock(Instant.parse("2026-10-01T00:00:00Z"));
      List<Duration> waits = new ArrayList<>();
      TheSportsDbRequestPacer pacer =
          new TheSportsDbRequestPacer(
              2500,
              clock,
              duration -> {
                waits.add(duration);
                clock.advance(duration);
              });
      pacer.beforeRequest();
      pacer.delayUntil(clock.instant().plusSeconds(60));
      pacer.beforeRequest();
      assertThat(waits).containsExactly(Duration.ofSeconds(60));
    }
  }

  private static final class MutableClock extends Clock {
    private Instant current;

    private MutableClock(Instant initial) {
      current = initial;
    }

    private void advance(Duration duration) {
      current = current.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return current;
    }
  }
}
