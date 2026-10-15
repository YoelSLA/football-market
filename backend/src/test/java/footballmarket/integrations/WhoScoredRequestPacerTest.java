package footballmarket.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.integrations.exceptions.WhoScoredIntegrationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WhoScoredRequestPacerTest {
  @Nested
  @DisplayName("Ritmo compartido sin reintentos")
  class Pacing {
    @Test
    @DisplayName("El permiso es único entre hilos y el intervalo se comparte por instancia")
    void sharesOnePermitAcrossThreads() throws Exception {
      // Arrange
      AtomicLong time = new AtomicLong();
      List<Long> starts = java.util.Collections.synchronizedList(new ArrayList<>());
      WhoScoredRequestPacer pacer =
          new WhoScoredRequestPacer(
              2500, time::get, time::addAndGet, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
      java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
      try (java.util.concurrent.ExecutorService executor =
          java.util.concurrent.Executors.newFixedThreadPool(2)) {
        java.util.concurrent.Future<?> first =
            executor.submit(
                () -> {
                  start.await();
                  return pacer.execute(() -> starts.add(time.get()));
                });
        java.util.concurrent.Future<?> second =
            executor.submit(
                () -> {
                  start.await();
                  return pacer.execute(() -> starts.add(time.get()));
                });
        // Act
        start.countDown();
        first.get(5, java.util.concurrent.TimeUnit.SECONDS);
        second.get(5, java.util.concurrent.TimeUnit.SECONDS);
      }
      // Assert
      assertThat(starts).containsExactly(0L, 2500L);
    }

    @Test
    @DisplayName("Interpreta Retry After HTTP date sin repetir ni alterar el reloj real")
    void respectsHttpDate() {
      // Arrange
      AtomicLong time = new AtomicLong();
      WhoScoredRequestPacer pacer =
          new WhoScoredRequestPacer(
              2500, time::get, time::addAndGet, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
      pacer.defer("Thu, 1 Jan 1970 00:00:10 GMT");
      // Act
      long started = pacer.execute(time::get);
      // Assert
      assertThat(started).isEqualTo(10000);
    }

    @Test
    @DisplayName("Separa inicios y respeta Retry After en la solicitud posterior")
    void spacesRequestsAndDefers() {
      // Arrange
      AtomicLong time = new AtomicLong(0);
      List<Long> starts = new ArrayList<>();
      Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
      WhoScoredRequestPacer pacer =
          new WhoScoredRequestPacer(2500, time::get, time::addAndGet, clock);
      // Act
      pacer.execute(() -> starts.add(time.get()));
      pacer.execute(() -> starts.add(time.get()));
      pacer.defer("10");
      pacer.execute(() -> starts.add(time.get()));
      // Assert
      assertThat(starts).containsExactly(0L, 2500L, 12500L);
    }

    @Test
    @DisplayName("Interrumpir la espera impide enviar y conserva la señal")
    void preservesInterruption() {
      // Arrange
      Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
      WhoScoredRequestPacer pacer =
          new WhoScoredRequestPacer(
              2500,
              () -> 0,
              delay -> {
                throw new InterruptedException();
              },
              clock);
      pacer.execute(() -> "first");
      try {
        // Act / Assert
        assertThatThrownBy(() -> pacer.execute(() -> "must-not-send"))
            .isInstanceOf(WhoScoredIntegrationException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
      } finally {
        Thread.interrupted();
      }
    }
  }
}
