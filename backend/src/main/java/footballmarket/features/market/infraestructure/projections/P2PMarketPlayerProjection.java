package footballmarket.features.market.infraestructure.projections;

/** Proyección de infraestructura para el catálogo P2P con métricas agrupadas por jugador. */
public interface P2PMarketPlayerProjection {

  Long getId();

  String getName();

  String getPosition();

  String getNationality();

  Long getTeamId();

  String getTeamName();

  String getTeamBadgeUrl();

  Long getLeagueId();

  String getLeagueName();

  String getImageUrl();

  Long getListingCount();

  Long getAvailableQuantity();

  Long getMinimumUnitPrice();
}
