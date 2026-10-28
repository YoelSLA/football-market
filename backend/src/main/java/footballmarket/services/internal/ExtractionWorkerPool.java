package footballmarket.services.internal;

import footballmarket.services.exceptions.PlayerStatisticsSynchronizationException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Paralelismo del lote acotado por los workers configurados del canal.
 *
 * <p>Los hilos solo viven durante el lote y se liberan al cerrarlo, incluso si una tarea falla.
 * Cada worker dispone de un gancho de limpieza que se ejecuta en su propio hilo, porque los
 * recursos afines al hilo que crea el worker solo pueden cerrarse ahí.
 */
public final class ExtractionWorkerPool implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(ExtractionWorkerPool.class);
  private static final long CLOSE_TIMEOUT_SECONDS = 10L;

  private final ExecutorService executor;
  private final Set<Thread> workerThreads;
  private final int workers;
  private final Runnable threadCleanup;

  private ExtractionWorkerPool(
      ExecutorService executor, Set<Thread> workerThreads, int workers, Runnable threadCleanup) {
    this.executor = executor;
    this.workerThreads = workerThreads;
    this.workers = workers;
    this.threadCleanup = threadCleanup;
  }

  /** Crea un pool con el número de workers indicado y sin reutilizar hilos externos. */
  public static ExtractionWorkerPool open(int workers) {
    return open(workers, () -> {});
  }

  /**
   * Crea un pool que libera los recursos de cada hilo de worker al terminar ese hilo.
   *
   * <p>La limpieza se ejecuta en el propio worker, que es el único hilo donde sus recursos pueden
   * cerrarse.
   */
  public static ExtractionWorkerPool open(int workers, Runnable threadCleanup) {
    if (workers < 1) {
      throw new PlayerStatisticsSynchronizationException();
    }
    Set<Thread> workerThreads = ConcurrentHashMap.newKeySet();
    AtomicInteger counter = new AtomicInteger();
    ThreadFactory factory =
        runnable -> {
          Thread thread =
              new Thread(
                  () -> {
                    try {
                      runnable.run();
                    } finally {
                      workerThreads.remove(Thread.currentThread());
                    }
                  });
          thread.setName("statistics-extraction-" + counter.incrementAndGet());
          thread.setDaemon(true);
          workerThreads.add(thread);
          return thread;
        };
    return new ExtractionWorkerPool(
        Executors.newFixedThreadPool(workers, factory), workerThreads, workers, threadCleanup);
  }

  /** Ejecuta una tarea por cada elemento de la lista y propaga el primer fallo. */
  public void forEach(List<Long> playerIds, java.util.function.LongConsumer task) {
    if (playerIds.isEmpty()) {
      return;
    }
    AtomicInteger next = new AtomicInteger();
    java.util.concurrent.atomic.AtomicBoolean stopping =
        new java.util.concurrent.atomic.AtomicBoolean();
    List<Future<Object>> futures =
        IntStream.range(0, Math.min(this.workers, playerIds.size()))
            .mapToObj(
                index ->
                    this.executor.submit(
                        () -> {
                          RuntimeException primary = null;
                          try {
                            int selected;
                            while (!stopping.get()
                                && (selected = next.getAndIncrement()) < playerIds.size()) {
                              task.accept(playerIds.get(selected));
                            }
                            return null;
                          } catch (RuntimeException failure) {
                            primary = failure;
                            stopping.set(true);
                            throw failure;
                          } finally {
                            // El driver recibe mensajes durante close: no hacerlo con el hilo
                            // interrumpido.
                            boolean interrupted = Thread.interrupted();
                            try {
                              this.threadCleanup.run();
                            } catch (RuntimeException cleanupFailure) {
                              stopping.set(true);
                              if (primary != null) {
                                primary.addSuppressed(cleanupFailure);
                              } else {
                                throw cleanupFailure;
                              }
                            } finally {
                              if (interrupted) {
                                Thread.currentThread().interrupt();
                              }
                            }
                          }
                        }))
            .toList();
    RuntimeException failure = null;
    boolean interrupted = false;
    for (Future<Object> future : futures) {
      boolean finished = false;
      while (!finished) {
        try {
          future.get();
          finished = true;
        } catch (InterruptedException interruption) {
          interrupted = true;
          stopping.set(true);
          this.workerThreads.forEach(Thread::interrupt);
        } catch (ExecutionException error) {
          finished = true;
          RuntimeException cause =
              error.getCause() instanceof RuntimeException runtime
                  ? runtime
                  : new PlayerStatisticsSynchronizationException();
          if (failure == null) {
            failure = cause;
          } else if (failure != cause) {
            failure.addSuppressed(cause);
          }
        }
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
      if (failure == null) {
        failure = new PlayerStatisticsSynchronizationException();
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  @Override
  public void close() {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CLOSE_TIMEOUT_SECONDS);
    this.executor.shutdown();
    boolean interrupted = Thread.interrupted();
    boolean terminated = this.awaitTermination(deadline);
    boolean cleaned = this.awaitWorkerCleanup(deadline);
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
    if (!terminated || !cleaned) {
      LOG.warn("Los workers de extracción no terminaron su cierre dentro del plazo");
    }
  }

  /**
   * Espera la terminación del executor hasta el plazo indicado.
   *
   * <p>{@code awaitTermination} no basta para garantizar la limpieza por hilo: el executor señala
   * su terminación antes de que el {@link ThreadFactory} ejecute el cierre posterior del hilo.
   */
  private boolean awaitTermination(long deadline) {
    long remaining = deadline - System.nanoTime();
    try {
      return remaining > 0 && this.executor.awaitTermination(remaining, TimeUnit.NANOSECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /** Espera a que los hilos creados terminen su limpieza, acotado por el mismo plazo. */
  private boolean awaitWorkerCleanup(long deadline) {
    for (Thread worker : List.copyOf(this.workerThreads)) {
      long remaining = deadline - System.nanoTime();
      if (remaining <= 0) {
        return false;
      }
      try {
        worker.join(Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remaining)));
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        return false;
      }
      if (worker.isAlive()) {
        return false;
      }
    }
    return this.workerThreads.isEmpty();
  }
}
