package footballmarket.features.market.infraestructure.projections;

/** Volumen de compras agregado por jugador para una ventana de mercado. */
public interface PlayerMarketDemandProjection {
  Long getPlayerId();

  String getPlayerName();

  Long getBoughtTokens();

  Long getBuyVolume();
}
