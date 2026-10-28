package footballmarket.features.valuation.application.impl;

import footballmarket.features.valuation.application.ValuationEvaluationService;
import footballmarket.features.valuation.application.exceptions.ValuationEvaluationException;
import footballmarket.features.valuation.application.internal.ValuationPopulationPreparer;
import footballmarket.features.valuation.application.internal.ValuationResultPublisher;
import footballmarket.features.valuation.application.internal.ValuationSnapshotSelector;
import footballmarket.features.valuation.domain.enums.ValuationExclusionReason;
import footballmarket.features.valuation.domain.models.AttributeValuation;
import footballmarket.features.valuation.domain.models.AttributeValuationCalculator;
import footballmarket.features.valuation.domain.models.PositionalValuation;
import footballmarket.features.valuation.domain.models.PositionalValuationCalculator;
import footballmarket.features.valuation.domain.records.ValuationEvaluationSummary;
import footballmarket.features.valuation.infraestructure.config.ValuationProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** Recorre la población sin llamadas externas y continúa ante fallos individuales. */
@Service
public class ValuationEvaluationServiceImpl implements ValuationEvaluationService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ValuationEvaluationServiceImpl.class);
  private final ValuationSnapshotSelector selector;
  private final ValuationPopulationPreparer preparer;
  private final ValuationResultPublisher publisher;
  private final ValuationProperties properties;
  private final Clock clock;

  public ValuationEvaluationServiceImpl(
      ValuationSnapshotSelector selector,
      ValuationPopulationPreparer preparer,
      ValuationResultPublisher publisher,
      ValuationProperties properties,
      @Qualifier("statisticsClock") Clock clock) {
    this.selector = selector;
    this.preparer = preparer;
    this.publisher = publisher;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public ValuationEvaluationSummary evaluate() {
    Instant startedAt = this.clock.instant();
    LocalDate valuationDate = LocalDate.ofInstant(startedAt, java.time.ZoneOffset.UTC);
    try {
      ValuationSnapshotSelector.Snapshot snapshot = this.selector.select();
      ValuationPopulationPreparer.Preparation preparation = this.preparer.prepare(snapshot);
      long evaluated = 0;
      long failed = preparation.preparationFailures();
      Map<String, Long> imputed = new HashMap<>();
      java.util.Set<Long> eligibleIds =
          preparation.players().stream()
              .map(player -> player.player().getId())
              .collect(java.util.stream.Collectors.toSet());
      for (Long playerId : preparation.snapshotPlayerIds()) {
        if (!eligibleIds.contains(playerId)) this.publisher.remove(playerId, valuationDate);
      }
      for (ValuationPopulationPreparer.PreparedPlayer prepared : preparation.players()) {
        try {
          PositionalValuation positional =
              PositionalValuationCalculator.calculate(
                  prepared.player(), valuationDate, prepared.metrics(), this.properties, startedAt);
          AttributeValuation attributes =
              AttributeValuationCalculator.calculate(
                  prepared.player(), valuationDate, prepared.metrics(), this.properties, startedAt);
          this.publisher.publish(positional, attributes);
          evaluated++;
          for (String key : prepared.metrics().imputedMetrics()) imputed.merge(key, 1L, Long::sum);
        } catch (RuntimeException individualFailure) {
          failed++;
          this.publisher.remove(prepared.player().getId(), valuationDate);
          LOGGER.error(
              "valuation_player_failed playerId={} stage=publish causeType={}",
              prepared.player().getId(),
              individualFailure.getClass().getSimpleName());
        }
      }
      Map<ValuationExclusionReason, Long> exclusions = Map.copyOf(preparation.exclusions());
      return new ValuationEvaluationSummary(
          valuationDate, evaluated, failed, exclusions, Map.copyOf(imputed));
    } catch (
        footballmarket.features.valuation.application.exceptions
                .ValuationSnapshotUnavailableException
            unavailable) {
      throw unavailable;
    } catch (RuntimeException globalFailure) {
      LOGGER.error(
          "valuation_evaluation_failed causeType={}", globalFailure.getClass().getSimpleName());
      throw new ValuationEvaluationException();
    }
  }
}
