package footballmarket.features.market.domain.records;

/** Modelo inmutable de jugador en el Mercado P2P con datos agregados de publicaciones activas. */
public record P2PMarketPlayer(
    Long id,
    String name,
    String position,
    String nationality,
    Long teamId,
    String teamName,
    String teamBadgeUrl,
    Long leagueId,
    String leagueName,
    String imageUrl,
    long listingCount,
    long availableQuantity,
    long minimumUnitPrice) {}
