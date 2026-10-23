package footballmarket.services.impl;

import footballmarket.config.QuoteProperties;
import footballmarket.models.Player;
import footballmarket.models.QuoteExecution;
import footballmarket.models.enums.QuoteOrigin;
import footballmarket.models.enums.ValuationStrategy;
import footballmarket.repositories.PlayerQuoteRepository;
import footballmarket.repositories.QuoteExecutionRepository;
import footballmarket.services.QuoteRecalculationService;
import footballmarket.services.exceptions.ExecutionAlreadyExistsException;
import footballmarket.services.exceptions.InvalidAdjustmentFactorException;
import footballmarket.services.exceptions.InvalidMinPriceFloorException;
import footballmarket.services.exceptions.InvalidVariationCapException;
import footballmarket.services.exceptions.MarketNotInitializedException;
import footballmarket.services.exceptions.UnknownQuoteStrategyException;
import footballmarket.services.internal.QuotePlayerPublisher;
import footballmarket.services.internal.QuotePopulationSelector;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación del recálculo manual con una reserva única por fecha UTC. */
@Service
@Transactional
public class QuoteRecalculationServiceImpl implements QuoteRecalculationService {
  private static final Logger LOG = LoggerFactory.getLogger(QuoteRecalculationServiceImpl.class);

  private final PlayerQuoteRepository quotes;
  private final QuoteExecutionRepository executions;
  private final QuotePopulationSelector population;
  private final QuotePlayerPublisher publisher;
  private final QuoteProperties properties;
  private final Clock marketClock;

  public QuoteRecalculationServiceImpl(
      PlayerQuoteRepository quotes,
      QuoteExecutionRepository executions,
      QuotePopulationSelector population,
      QuotePlayerPublisher publisher,
      QuoteProperties properties,
      Clock marketClock) {
    this.quotes = quotes;
    this.executions = executions;
    this.population = population;
    this.publisher = publisher;
    this.properties = properties;
    this.marketClock = marketClock;
  }

  @Override
  public QuoteExecution recalculate(
      String requestedStrategy,
      BigDecimal requestedFactor,
      BigDecimal requestedCap,
      Long requestedFloor) {
    ValuationStrategy strategy = this.parseStrategy(requestedStrategy);
    if (!this.quotes.existsByOrigin(QuoteOrigin.INITIAL)) {
      throw new MarketNotInitializedException();
    }
    BigDecimal factor =
        requestedFactor == null ? this.properties.adjustmentFactor() : requestedFactor;
    if (factor.compareTo(new BigDecimal("0.10")) < 0 || factor.compareTo(BigDecimal.ONE) > 0) {
      throw new InvalidAdjustmentFactorException();
    }
    BigDecimal cap = requestedCap == null ? this.properties.variationCap() : requestedCap;
    if (cap.compareTo(BigDecimal.ZERO) <= 0 || cap.compareTo(BigDecimal.ONE) > 0) {
      throw new InvalidVariationCapException();
    }
    long floor = requestedFloor == null ? this.properties.minPriceFloor() : requestedFloor;
    if (floor <= 0) {
      throw new InvalidMinPriceFloorException();
    }

    LocalDate quoteDate = LocalDate.now(this.marketClock);
    Instant startedAt = Instant.now(this.marketClock);
    LOG.info(
        "quote_execution_started quoteDate={} strategy={} strategyVersion=v1 adjustmentFactor={}"
            + " variationCap={} minPriceFloor={}",
        quoteDate,
        strategy,
        factor,
        cap,
        floor);
    QuoteExecution execution =
        new QuoteExecution(
            quoteDate, strategy, "v1", factor, cap, floor, 0, 0, 0, 0, 0, startedAt, startedAt);
    try {
      this.executions.saveAndFlush(execution);
    } catch (DataIntegrityViolationException exception) {
      throw new ExecutionAlreadyExistsException();
    }

    List<Player> players = this.population.pricablePlayers();
    int recomputed = 0;
    int carried = 0;
    int notRepriced = 0;
    int failed = 0;
    for (Player player : players) {
      try {
        QuotePlayerPublisher.PublicationResult result =
            this.publisher.publish(player, quoteDate, strategy, factor, cap, floor, startedAt);
        switch (result.publication()) {
          case RECOMPUTED -> recomputed++;
          case CARRIED_FORWARD -> carried++;
          case NOT_REPRICED -> notRepriced++;
        }
      } catch (RuntimeException exception) {
        failed++;
        LOG.error(
            "quote_player_recalculation_failed playerId={} stage=recalculate cause={}",
            player.getId(),
            exception.getClass().getSimpleName(),
            exception);
      }
    }
    execution.complete(
        players.size(), recomputed, carried, notRepriced, failed, Instant.now(this.marketClock));
    LOG.info(
        "quote_execution_completed quoteDate={} strategy={} strategyVersion=v1 pricedPlayers={}"
            + " recomputedRows={} carriedForwardRows={} notRepricedPlayers={} failedPlayers={}",
        quoteDate,
        strategy,
        players.size(),
        recomputed,
        carried,
        notRepriced,
        failed);
    return this.executions.save(execution);
  }

  @Override
  @Transactional(readOnly = true)
  public List<QuoteExecution> executions(LocalDate date) {
    return this.executions.findAllByQuoteDateOrderByStartedAtDesc(date);
  }

  private ValuationStrategy parseStrategy(String requestedStrategy) {
    if (requestedStrategy == null) {
      throw new UnknownQuoteStrategyException();
    }
    try {
      return ValuationStrategy.valueOf(requestedStrategy);
    } catch (IllegalArgumentException exception) {
      throw new UnknownQuoteStrategyException();
    }
  }
}
