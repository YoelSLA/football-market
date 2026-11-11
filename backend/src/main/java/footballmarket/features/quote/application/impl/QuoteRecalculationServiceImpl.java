package footballmarket.features.quote.application.impl;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.exceptions.MarketNotInitializedException;
import footballmarket.features.quote.application.QuoteRecalculationService;
import footballmarket.features.quote.application.exceptions.InvalidAdjustmentFactorException;
import footballmarket.features.quote.application.exceptions.InvalidMinPriceFloorException;
import footballmarket.features.quote.application.exceptions.InvalidVariationCapException;
import footballmarket.features.quote.application.exceptions.UnknownQuoteStrategyException;
import footballmarket.features.quote.application.internal.QuoteExecutionRecorder;
import footballmarket.features.quote.application.internal.QuotePlayerPublisher;
import footballmarket.features.quote.application.internal.QuotePopulationSelector;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.quote.infraestructure.PlayerQuoteRepository;
import footballmarket.features.quote.infraestructure.QuoteExecutionRepository;
import footballmarket.features.quote.infraestructure.config.QuoteProperties;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Recálculo diario por jugador, compartido por las entradas manual y automática. */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class QuoteRecalculationServiceImpl implements QuoteRecalculationService {
  private static final Logger LOG = LoggerFactory.getLogger(QuoteRecalculationServiceImpl.class);

  private final PlayerQuoteRepository quotes;
  private final QuoteExecutionRepository executions;
  private final QuoteExecutionRecorder recorder;
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
      Clock marketClock,
      QuoteExecutionRecorder recorder) {
    this.quotes = quotes;
    this.executions = executions;
    this.population = population;
    this.publisher = publisher;
    this.properties = properties;
    this.marketClock = marketClock;
    this.recorder = recorder;
  }

  @Override
  public QuoteExecution recalculate(
      String requestedStrategy,
      BigDecimal requestedFactor,
      BigDecimal requestedCap,
      Long requestedFloor) {
    return this.recalculate(
        requestedStrategy,
        requestedFactor,
        requestedCap,
        requestedFloor,
        QuoteExecutionOrigin.MANUAL);
  }

  @Override
  public QuoteExecution recalculateAutomatic() {
    return this.recalculate(
        this.properties.strategy().name(),
        this.properties.adjustmentFactor(),
        this.properties.variationCap(),
        this.properties.minPriceFloor(),
        QuoteExecutionOrigin.AUTOMATIC);
  }

  private QuoteExecution recalculate(
      String requestedStrategy,
      BigDecimal requestedFactor,
      BigDecimal requestedCap,
      Long requestedFloor,
      QuoteExecutionOrigin origin) {
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

    Instant startedAt = Instant.now(this.marketClock);
    LocalDate quoteDate = startedAt.atOffset(ZoneOffset.UTC).toLocalDate();
    LOG.info(
        "quote_execution_started quoteDate={} strategy={} strategyVersion=v1 adjustmentFactor={}"
            + " variationCap={} minPriceFloor={} origin={}",
        quoteDate,
        strategy,
        factor,
        cap,
        floor,
        origin);
    QuoteExecution execution =
        new QuoteExecution(
            quoteDate, origin, strategy, "v1", factor, cap, floor, 0, 0, 0, 0, 0, startedAt, null);
    execution = this.recorder.start(execution);

    List<Player> players = this.population.pricablePlayers();
    int recomputed = 0;
    int carried = 0;
    int notRepriced = 0;
    int failed = 0;
    for (Player player : players) {
      try {
        QuotePlayerPublisher.PublicationResult result =
            this.publisher.publish(
                player, quoteDate, strategy, factor, cap, floor, startedAt, execution);
        switch (result.publication()) {
          case RECOMPUTED -> recomputed++;
          case CARRIED_FORWARD -> carried++;
          case NOT_REPRICED -> notRepriced++;
        }
      } catch (RuntimeException exception) {
        if (this.isDailyConflict(exception)) {
          notRepriced++;
          continue;
        }
        failed++;
        LOG.error(
            "quote_player_recalculation_failed playerId={} stage=recalculate cause={}",
            player.getId(),
            exception.getClass().getSimpleName());
      }
    }
    QuoteExecution completed =
        this.recorder.complete(
            execution.getId(),
            players.size(),
            recomputed,
            carried,
            notRepriced,
            failed,
            Instant.now(this.marketClock));
    LOG.info(
        "quote_execution_completed quoteDate={} strategy={} strategyVersion=v1 pricedPlayers={}"
            + " recomputedRows={} carriedForwardRows={} notRepricedPlayers={} failedPlayers={} origin={}",
        quoteDate,
        strategy,
        players.size(),
        recomputed,
        carried,
        notRepriced,
        failed,
        origin);
    return completed;
  }

  @Override
  @Transactional(readOnly = true)
  public List<QuoteExecution> executions(LocalDate date) {
    return this.executions.findAllByQuoteDateAndFinishedAtIsNotNullOrderByStartedAtDescIdDesc(date);
  }

  private boolean isDailyConflict(RuntimeException exception) {
    if (!(exception instanceof DataIntegrityViolationException)) {
      return false;
    }
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof PSQLException sql
          && "23505".equals(sql.getSQLState())
          && sql.getServerErrorMessage() != null
          && "player_quotes".equals(sql.getServerErrorMessage().getTable())
          && "uq_player_quote".equals(sql.getServerErrorMessage().getConstraint())) {
        return true;
      }
    }
    return false;
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
