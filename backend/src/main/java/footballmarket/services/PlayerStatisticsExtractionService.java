package footballmarket.services;

import java.time.Instant;

/** Extracción síncrona de jugadores; el lote delimita la vida del navegador compartido. */
public interface PlayerStatisticsExtractionService {
  Batch openBatch();

  /** Un lote abierto únicamente por una petición explícita de scraping. */
  interface Batch extends AutoCloseable {
    Long extract(Long runId, Long playerId, Instant deadline);

    @Override
    void close();
  }
}
