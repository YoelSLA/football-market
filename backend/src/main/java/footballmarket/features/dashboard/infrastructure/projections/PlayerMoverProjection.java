package footballmarket.features.dashboard.infrastructure.projections;

import java.math.BigDecimal;

public interface PlayerMoverProjection {
  Long getPlayerId();

  String getName();

  String getTeamName();

  String getLeagueName();

  String getPosition();

  String getImageUrl();

  Long getCurrentPrice();

  Long getPreviousPrice();

  Long getDeltaCredits();

  BigDecimal getDeltaPercentage();

  BigDecimal getScore();
}
