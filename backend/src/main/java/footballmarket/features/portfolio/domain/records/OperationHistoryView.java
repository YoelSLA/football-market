package footballmarket.features.portfolio.domain.records;

import java.util.List;

/** Resultado paginado del historial personal y sus agregados filtrados. */
public record OperationHistoryView(
    Summary summary, List<OperationHistoryEntry> transactions, Pagination pagination) {

  /** Indicadores de actividad calculados sobre todo el conjunto filtrado. */
  public record Summary(
      long totalTransactions,
      long buyTransactions,
      long sellTransactions,
      long boughtTokens,
      long soldTokens,
      long purchasedVolume,
      long soldVolume) {}

  /** Metadatos de la página solicitada. */
  public record Pagination(int page, int size, long totalElements, int totalPages) {}
}
