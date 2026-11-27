package footballmarket.features.businessmetrics.application;

/** Evento interno de una transacción financiera confirmada. */
public record BusinessMetricTransactionEvent(
    TransactionKind kind, long tokensTransferred, long creditsTradedInCents) {
  public enum TransactionKind {
    PURCHASE,
    SALE
  }
}
