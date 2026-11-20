package footballmarket.features.market.infraestructure.projections;

import java.time.Instant;
import java.time.LocalDate;

/** Interfaz de proyección SQL para la consulta paginada consolidada del Mercado Oficial. */
public interface OfficialMarketPlayerProjection {
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

  // Campos de cotización embebida (nulos si el jugador no registra cotización)
  Long getQuoteId();

  Long getPrice();

  Long getPreviousPrice();

  Double getVariationPercentage();

  Double getScore();

  Double getMultiplier();

  String getOrigin();

  LocalDate getEffectiveDate();

  Instant getCalculatedAt();
}
