package footballmarket.features.quote.application;

import footballmarket.features.market.domain.records.MarketInitializationResult;
import java.time.LocalDate;

/** Inicialización del mercado: fija la primera fecha de mercado y el ancla de 1 crédito. */
public interface QuoteMarketInitializationService {

  /**
   * Inicializa el mercado con una fila inicial por jugador cotizable.
   *
   * @param marketDate fecha de mercado solicitada; si es nula se usa la fecha UTC del día
   * @return resultado observable de la inicialización
   */
  MarketInitializationResult initialize(LocalDate marketDate);
}
