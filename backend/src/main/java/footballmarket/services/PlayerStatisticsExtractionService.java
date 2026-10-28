package footballmarket.services;

/** Extracción síncrona de jugadores; el lote delimita la vida de los recursos de cada worker. */
public interface PlayerStatisticsExtractionService {
  Batch openBatch();

  /** Un lote abierto únicamente por una petición explícita de scraping. */
  interface Batch extends AutoCloseable {
    Long extract(Long runId, Long playerId);

    /** Navegaciones realmente iniciadas, incluidas las de jugadores fallidos. */
    long providerCalls();

    /**
     * Libera los recursos atados al hilo del worker que ejecuta la extracción.
     *
     * <p>Debe invocarse en ese mismo hilo al terminar el worker; no se usa desde el hilo de la
     * petición.
     */
    default void releaseWorker() {}

    @Override
    void close();
  }
}
