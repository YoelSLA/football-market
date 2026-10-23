package footballmarket.services.internal;

import footballmarket.models.enums.ValuationStrategy;
import footballmarket.repositories.AttributeValuationRepository;
import footballmarket.repositories.PositionalValuationRepository;
import footballmarket.services.exceptions.UnknownQuoteStrategyException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Score persistido de SPEC-013 que sustenta la cotización de un jugador.
 *
 * <p>No recalcula valuaciones ni consulta proveedores externos: resuelve el valor final ya
 * persistido —el score posicional para {@code POSITIONAL_PERFORMANCE} y el GRL para {@code
 * GENERAL_ATTRIBUTES}— junto con su fecha de valuación y la estrategia que lo produjo.
 */
@Component
public class QuoteScoreResolver {

  private final PositionalValuationRepository positionalValuations;
  private final AttributeValuationRepository attributeValuations;

  public QuoteScoreResolver(
      PositionalValuationRepository positionalValuations,
      AttributeValuationRepository attributeValuations) {
    this.positionalValuations = positionalValuations;
    this.attributeValuations = attributeValuations;
  }

  /**
   * Resuelve el score vigente del jugador para la estrategia elegida.
   *
   * @param playerId identificador interno del jugador
   * @param strategy estrategia de la ejecución
   * @return score con su fecha de valuación y su estrategia, o vacío si el jugador no fue valuado
   *     con esa estrategia
   * @throws UnknownQuoteStrategyException si la estrategia no pertenece al catálogo vigente
   */
  @Transactional(readOnly = true)
  public Optional<ResolvedScore> resolve(Long playerId, ValuationStrategy strategy) {
    if (strategy == null) {
      throw new UnknownQuoteStrategyException();
    }
    return switch (strategy) {
      case POSITIONAL_PERFORMANCE ->
          this.positionalValuations.findLatestByPlayerId(playerId).stream()
              .findFirst()
              .map(
                  valuation ->
                      new ResolvedScore(
                          valuation.getFinalScore(),
                          valuation.getValuationDate(),
                          valuation.getStrategyName(),
                          valuation.getStrategyVersion()));
      case GENERAL_ATTRIBUTES ->
          this.attributeValuations.findLatestByPlayerId(playerId).stream()
              .findFirst()
              .map(
                  valuation ->
                      new ResolvedScore(
                          valuation.getFinalGrl(),
                          valuation.getValuationDate(),
                          valuation.getStrategyName(),
                          valuation.getStrategyVersion()));
    };
  }

  /**
   * Score persistido de una valuación, con la fecha que lo sustenta y la estrategia que lo produjo.
   *
   * @param score valor final persistido por la estrategia
   * @param valuationDate fecha de la valuación de origen
   * @param strategy estrategia que produjo el score
   * @param strategyVersion versión de la estrategia
   */
  public record ResolvedScore(
      BigDecimal score,
      LocalDate valuationDate,
      ValuationStrategy strategy,
      String strategyVersion) {}
}
