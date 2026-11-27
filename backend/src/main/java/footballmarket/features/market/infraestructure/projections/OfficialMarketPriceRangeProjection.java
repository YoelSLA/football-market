package footballmarket.features.market.infraestructure.projections;

/** Proyección del mínimo y máximo global de cotizaciones vigentes elegibles. */
public interface OfficialMarketPriceRangeProjection {
  Long getMin();

  Long getMax();
}
