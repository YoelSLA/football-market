package footballmarket.features.dashboard.infrastructure.projections;

import java.math.BigDecimal;

public interface ValuePickProjection {
  Long getPlayerId();

  String getName();

  String getTeamName();

  String getLeagueName();

  String getPosition();

  String getImageUrl();

  BigDecimal getWhoScoredScore();

  Long getCurrentPrice();

  BigDecimal getValueRatio();
}
