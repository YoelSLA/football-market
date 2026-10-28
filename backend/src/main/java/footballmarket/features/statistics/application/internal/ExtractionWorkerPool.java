package footballmarket.features.statistics.application.internal;

import footballmarket.features.statistics.application.exceptions.PlayerStatisticsSynchronizationException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Paralelismo del lote acotado por los workers configurados del canal.
 *
 * <p>Los hilos solo viven durante el lote y se liberan al cerrarlo, incluso si una tarea falla.
 */
public final class ExtractionWorkerPool implements AutoCloseable {
  private final ExecutorService executor;

  private ExtractionWorkerPool(ExecutorService executor) {
    this.executor = executor;
  }

  /** Crea un pool con el número de workers indicado y sin reutilizar hilos externos. */
  public static ExtractionWorkerPool open(int workers) {
    if (workers < 1) {
      throw new PlayerStatisticsSynchronizationException();
    }
    AtomicInteger counter = new AtomicInteger();
    ThreadFactory factory =
        runnable -> {
          Thread thread = new Thread(runnable);
          thread.setName("statistics-extraction-" + counter.incrementAndGet());
          thread.setDaemon(true);
          return thread;
        };
    return new ExtractionWorkerPool(Executors.newFixedThreadPool(workers, factory));
  }

  /** Ejecuta una tarea por cada elemento de la lista y propaga el primer fallo. */
  public void forEach(List<Long> playerIds, java.util.function.LongConsumer task) {
    if (playerIds.isEmpty()) {
      return;
    }
    List<Future<Object>> futures =
        IntStream.range(0, playerIds.size())
            .mapToObj(
                index ->
                    this.executor.submit(
                        () -> {
                          task.accept(playerIds.get(index));
                          return null;
                        }))
            .toList();
    RuntimeException failure = null;
    for (Future<Object> future : futures) {
      try {
        future.get();
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw new PlayerStatisticsSynchronizationException();
      } catch (ExecutionException error) {
        if (failure == null && error.getCause() instanceof RuntimeException cause) {
          failure = cause;
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  @Override
  public void close() {
    this.executor.shutdownNow();
  }
}
