package footballmarket.features.quote.domain.records;

import java.time.Instant;
import java.time.LocalDate;

/** Proyección de conjunto del ranking numerado antes de aplicar filtros. */
public interface QuoteRankingRow {
  long getPosition();

  Long getPlayerId();

  long getPrice();

  LocalDate getEffectiveDate();

  String getOrigin();

  Instant getCalculatedAt();

  String getName();

  String getPlayerPosition();

  String getImageUrl();

  Long getTeamId();

  String getTeamName();

  Long getLeagueId();

  String getLeagueName();
}
