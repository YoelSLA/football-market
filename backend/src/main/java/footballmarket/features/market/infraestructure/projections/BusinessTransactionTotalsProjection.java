package footballmarket.features.market.infraestructure.projections;

/** Agregados históricos de las transacciones financieras persistidas. */
public interface BusinessTransactionTotalsProjection {
  Long getPurchases();

  Long getSales();

  Long getTokensTransferred();

  Long getCreditsTradedInCents();
}
