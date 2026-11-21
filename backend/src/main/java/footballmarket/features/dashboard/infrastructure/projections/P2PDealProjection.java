package footballmarket.features.dashboard.infrastructure.projections;

import java.math.BigDecimal;

public interface P2PDealProjection {
  Long getListingId();

  Long getPlayerId();

  String getPlayerName();

  String getTeamName();

  String getLeagueName();

  String getImageUrl();

  Integer getAvailableTokens();

  Long getP2pUnitPrice();

  Long getOfficialQuotePrice();

  BigDecimal getDiscountPercentage();
}
