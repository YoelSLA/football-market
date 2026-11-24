package footballmarket.features.market.infraestructure.projections;

/** Agregados globales de transacciones clasificadas dentro de una ventana de mercado. */
public interface MarketTrendProjection {
  Long getTotalTransactions();

  Long getBuyTransactions();

  Long getSellTransactions();

  Long getTotalTradedTokens();

  Long getTotalTradedCredits();
}
