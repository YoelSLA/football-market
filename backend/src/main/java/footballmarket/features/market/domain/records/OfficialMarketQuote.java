package footballmarket.features.market.domain.records;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Modelo inmutable de cotización de mercado oficial en la capa de dominio.
 *
 * @param price precio unitario vigente en centésimas de crédito
 * @param previousPrice precio anterior en centésimas de crédito, o nulo si no existe
 * @param variationPercentage variación porcentual, o nulo si no existe precio previo válido
 * @param performanceScore score técnico de rendimiento
 * @param multiplier multiplicador de valuación
 * @param calculationOrigin origen o disparador del cálculo
 * @param effectiveDate fecha de vigencia
 * @param calculatedAt instante exacto UTC de persistencia
 */
public record OfficialMarketQuote(
    long price,
    Long previousPrice,
    Double variationPercentage,
    double performanceScore,
    double multiplier,
    String calculationOrigin,
    LocalDate effectiveDate,
    Instant calculatedAt) {}
