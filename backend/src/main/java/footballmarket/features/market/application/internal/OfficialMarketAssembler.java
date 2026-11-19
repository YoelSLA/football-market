package footballmarket.features.market.application.internal;

import footballmarket.features.market.domain.records.OfficialMarketPlayer;
import footballmarket.features.market.domain.records.OfficialMarketQuote;
import footballmarket.features.market.infraestructure.projections.OfficialMarketPlayerProjection;

/**
 * Ensamblador interno de aplicación para transformar proyecciones de persistencia hacia el modelo
 * de dominio del Mercado Oficial.
 */
public final class OfficialMarketAssembler {

  private OfficialMarketAssembler() {}

  /**
   * Convierte una proyección SQL nativa en el modelo inmutable de dominio {@link
   * OfficialMarketPlayer}.
   *
   * @param projection fila proyectada desde la base de datos
   * @return jugador del mercado oficial
   */
  public static OfficialMarketPlayer toDomain(OfficialMarketPlayerProjection projection) {
    if (projection == null) {
      return null;
    }

    OfficialMarketQuote quote = null;
    if (projection.getQuoteId() != null) {
      Double variation = null;
      if (projection.getPreviousPrice() != null && projection.getPreviousPrice() > 0) {
        if (projection.getPreviousPrice().equals(projection.getPrice())) {
          variation = 0.0;
        } else if (projection.getVariationPercentage() != null) {
          variation = projection.getVariationPercentage();
        } else {
          double calc =
              ((double) (projection.getPrice() - projection.getPreviousPrice())
                      / projection.getPreviousPrice())
                  * 100.0;
          variation = Math.round(calc * 100.0) / 100.0;
        }
      }

      double score = projection.getScore() != null ? projection.getScore() : 0.0;
      double multiplier = projection.getMultiplier() != null ? projection.getMultiplier() : 1.0;
      String origin = projection.getOrigin() != null ? projection.getOrigin() : "INITIAL";

      quote =
          new OfficialMarketQuote(
              projection.getPrice(),
              projection.getPreviousPrice(),
              variation,
              score,
              multiplier,
              origin,
              projection.getEffectiveDate(),
              projection.getCalculatedAt());
    }

    return new OfficialMarketPlayer(
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
        quote);
  }
}
