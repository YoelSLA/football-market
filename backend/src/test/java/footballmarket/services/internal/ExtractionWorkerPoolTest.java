package footballmarket.services.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.services.exceptions.PlayerStatisticsSynchronizationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Ciclo de vida de los hilos que ejecutan el lote de scraping.
 *
 * <p>Cada worker crea recursos afines a su hilo, así que el pool debe ejecutar la limpieza en ese
 * mismo hilo y esperar a que termine antes de devolver el control.
 */
@org.junit.jupiter.api.Tag("unit")
class ExtractionWorkerPoolTest {
  private void record(Map<String, AtomicInteger> cleanups) {
    cleanups
        .computeIfAbsent(Thread.currentThread().getName(), name -> new AtomicInteger())
        .incrementAndGet();
  }

  @Nested
  @DisplayName("Ejecución de tareas del lote")
  class Tasks {
    @Test
    @DisplayName("Propaga el primer fallo en el orden de la lista")
    void propagatesFirstFailure() {
      // Arrange
      try (ExtractionWorkerPool pool = ExtractionWorkerPool.open(1)) {
        // Act / Assert
        assertThatThrownBy(
                () ->
                    pool.forEach(
                        List.of(1L, 2L, 3L),
                        id -> {
                          if (id == 1L) {
                            throw new IllegalStateException("primer fallo");
                          }
                          if (id == 2L) {
                            throw new IllegalStateException("segundo fallo");
                          }
                        }))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("primer fallo");
      }
    }

    @Test
    @DisplayName("Una lista vacía no crea hilos de worker")
    void emptyListDoesNotCreateWorkers() {
      // Arrange
      Map<String, AtomicInteger> cleanups = new ConcurrentHashMap<>();
      try (ExtractionWorkerPool pool = ExtractionWorkerPool.open(2, () -> record(cleanups))) {
        // Act
        pool.forEach(
            List.of(),
            id -> {
              throw new AssertionError("No debe ejecutarse ninguna tarea vacía");
            });
        // Assert
        assertThat(cleanups).isEmpty();
      }
    }
  }

  @Nested
  @DisplayName("Cierre del pool")
  class Closing {
    @Test
    @DisplayName("La limpieza termina antes de retornar y no recibe el hilo interrumpido")
    void cleanupRunsBeforeReturnWithoutInterrupt() {
      // Arrange
      AtomicInteger cleanups = new AtomicInteger();
      try (ExtractionWorkerPool pool =
          ExtractionWorkerPool.open(
              1,
              () -> {
                if (Thread.currentThread().isInterrupted()) {
                  throw new AssertionError("El driver no puede cerrarse con el hilo interrumpido");
                }
                cleanups.incrementAndGet();
              })) {
        // Act
        pool.forEach(List.of(1L), id -> Thread.currentThread().interrupt());
        // Assert
        assertThat(cleanups).hasValue(1);
      }
    }

    @Test
    @DisplayName("El error de limpieza no oculta el fallo principal")
    void preservesPrimaryAndCleanupFailure() {
      // Arrange
      IllegalStateException primary = new IllegalStateException("operación");
      IllegalStateException cleanup = new IllegalStateException("limpieza");
      try (ExtractionWorkerPool pool =
          ExtractionWorkerPool.open(
              1,
              () -> {
                throw cleanup;
              })) {
        // Act / Assert
        assertThatThrownBy(
                () ->
                    pool.forEach(
                        List.of(1L),
                        id -> {
                          throw primary;
                        }))
            .isSameAs(primary)
            .hasSuppressedException(cleanup);
      }
    }

    @Test
    @DisplayName("Libera los recursos una vez en cada hilo de worker")
    void cleansUpOncePerWorkerThread() {
      // Arrange
      Map<String, AtomicInteger> cleanups = new ConcurrentHashMap<>();
      CyclicBarrier barrier = new CyclicBarrier(2);
      try (ExtractionWorkerPool pool = ExtractionWorkerPool.open(2, () -> record(cleanups))) {
        // Act
        pool.forEach(
            List.of(1L, 2L),
            id -> {
              try {
                barrier.await(5, TimeUnit.SECONDS);
              } catch (Exception failure) {
                throw new AssertionError("Los workers no llegaron a coincidir", failure);
              }
            });
      }
      // Assert
      assertThat(cleanups).hasSize(2);
      assertThat(cleanups.values()).allSatisfy(count -> assertThat(count).hasValue(1));
      assertThat(cleanups.keySet())
          .allSatisfy(name -> assertThat(name).startsWith("statistics-extraction-"));
    }

    @Test
    @DisplayName("El cierre espera a que el worker termine su limpieza")
    void closeWaitsForCleanup() throws Exception {
      // Arrange
      CountDownLatch started = new CountDownLatch(1);
      AtomicInteger cleanups = new AtomicInteger();
      ExtractionWorkerPool pool = ExtractionWorkerPool.open(1, cleanups::incrementAndGet);
      Thread runner =
          new Thread(
              () ->
                  pool.forEach(
                      List.of(1L),
                      id -> {
                        started.countDown();
                        try {
                          Thread.sleep(1000);
                        } catch (InterruptedException interrupted) {
                          try {
                            Thread.sleep(300);
                          } catch (InterruptedException second) {
                            Thread.currentThread().interrupt();
                          }
                        }
                      }));
      runner.setDaemon(true);
      runner.start();
      assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
      // Act
      pool.close();
      // Assert
      assertThat(cleanups).hasValue(1);
      runner.join(5000);
      assertThat(runner.isAlive()).isFalse();
    }
  }

  @Nested
  @DisplayName("Configuración del pool")
  class Configuration {
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("Rechaza un pool sin workers")
    void rejectsNonPositiveWorkers(int workers) {
      assertThatThrownBy(() -> ExtractionWorkerPool.open(workers))
          .isInstanceOf(PlayerStatisticsSynchronizationException.class);
    }
  }
}
