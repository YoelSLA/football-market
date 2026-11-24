package footballmarket.features.quote.domain.enums;

/** Señal aplicada por una ejecución de recálculo de cotizaciones. */
public enum QuoteRecalculationType {
  /** Recálculo habitual con evaluación deportiva y señal de mercado. */
  SPORT_AND_MARKET,
  /** Recálculo manual que utiliza exclusivamente la demanda de mercado. */
  MARKET_DEMAND_ONLY
}
