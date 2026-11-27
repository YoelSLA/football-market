package footballmarket.features.portfolio.domain.records;

/** Proyección agregada del conjunto de transacciones que coincide con los criterios. */
public record OperationHistoryAggregate(
    long totalTransactions,
    long buyTransactions,
    long sellTransactions,
    long boughtTokens,
    long soldTokens,
    long purchasedVolume,
    long soldVolume) {}
