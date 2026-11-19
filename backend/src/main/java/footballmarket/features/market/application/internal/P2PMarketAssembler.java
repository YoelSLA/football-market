package footballmarket.features.market.application.internal;

import footballmarket.features.market.domain.records.P2PMarketPlayer;
import footballmarket.features.market.infraestructure.projections.P2PMarketPlayerProjection;

/**
 * Ensamblador de infraestructura para transformar proyecciones del Mercado P2P a modelos de
 * dominio.
 */
public final class P2PMarketAssembler {

  private P2PMarketAssembler() {}

  public static P2PMarketPlayer toDomain(P2PMarketPlayerProjection projection) {
    if (projection == null) {
      return null;
    }

    return new P2PMarketPlayer(
        projection.getId(),
        projection.getName(),
        projection.getPosition(),
        projection.getNationality(),
        projection.getTeamId(),
        projection.getTeamName(),
        projection.getTeamBadgeUrl(),
        projection.getLeagueId(),
        projection.getLeagueName(),
        projection.getImageUrl(),
        projection.getListingCount() != null ? projection.getListingCount() : 0L,
        projection.getAvailableQuantity() != null ? projection.getAvailableQuantity() : 0L,
        projection.getMinimumUnitPrice() != null ? projection.getMinimumUnitPrice() : 0L);
  }
}
