package footballmarket.features.ranking.domain.records;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Proyección del ranking global por score y de la identidad persistida del jugador. */
public interface ScoreRankingRow {
  long getPosition();

  Long getPlayerId();

  BigDecimal getScore();

  LocalDate getValuationDate();

  String getName();

  String getPlayerPosition();

  String getImageUrl();

  Long getTeamId();

  String getTeamName();

  Long getLeagueId();

  String getLeagueName();
}
