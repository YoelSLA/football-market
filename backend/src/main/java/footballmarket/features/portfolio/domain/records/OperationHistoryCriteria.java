package footballmarket.features.portfolio.domain.records;

import java.time.OffsetDateTime;

/** Criterios validados de consulta del historial personal. */
public record OperationHistoryCriteria(
    int page,
    int size,
    String search,
    OperationHistoryEntry.OperationType type,
    OffsetDateTime executedFrom,
    OffsetDateTime executedBefore,
    Sort sort) {

  /** Órdenes públicos permitidos por el contrato. */
  public enum Sort {
    NEWEST,
    OLDEST,
    AMOUNT_DESC,
    AMOUNT_ASC
  }
}
