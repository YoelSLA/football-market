package footballmarket.features.market.application;

import footballmarket.features.market.domain.records.MarketTrends;

/** Caso de uso de consulta agregada de tendencias del mercado. */
public interface MarketTrendsService {
  /** Consulta los agregados de mercado para la ventana configurada. */
  MarketTrends getMarketTrends();
}
