package footballmarket.features.market.infraestructure.projections;

/** Proyección de infraestructura para el rango de precios disponible en el mercado P2P. */
public interface P2PMarketPriceRangeProjection {

  Long getMin();

  Long getMax();
}
