package footballmarket.features.quote.application.internal;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.market.infraestructure.MarketTransactionRepository;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.models.PlayerQuote;
import footballmarket.features.quote.domain.models.QuoteAdjustment;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.quote.infraestructure.PlayerQuoteRepository;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Publica atómicamente la cotización diaria de un jugador en una corrida manual o automática. */
@Component
public class QuotePlayerPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuotePlayerPublisher.class);
  private final PlayerQuoteRepository quotes;
  private final QuoteScoreResolver scores;
  private final PlayerRepository players;
  private final MarketTransactionRepository transactions;

  public QuotePlayerPublisher(
      PlayerQuoteRepository quotes,
      QuoteScoreResolver scores,
      PlayerRepository players,
      MarketTransactionRepository transactions) {
    this.quotes = quotes;
    this.scores = scores;
    this.players = players;
    this.transactions = transactions;
  }

  /** Inserta o actualiza la única fila diaria bajo el bloqueo compartido con inicialización. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PublicationResult publish(
      Player player,
      LocalDate quoteDate,
      ValuationStrategy strategy,
      BigDecimal adjustmentFactor,
      BigDecimal variationCap,
      long minPriceFloor,
      Instant calculatedAt,
      QuoteExecution execution) {
    try {
      this.players.lockIdentityById(player.getId()).orElseThrow();
      PlayerQuote current = this.quotes.findLatestByPlayerId(player.getId()).getFirst();
      if (current.getEffectiveDate().isAfter(quoteDate)) {
        return new PublicationResult(Publication.NOT_REPRICED, Optional.empty());
      }
      Optional<QuoteScoreResolver.ResolvedScore> resolved =
          this.scores.resolve(player.getId(), strategy);
      PlayerQuote next =
          resolved
              .map(
                  score ->
                      this.recomputed(
                          player,
                          current,
                          score,
                          adjustmentFactor,
                          variationCap,
                          minPriceFloor,
                          calculatedAt,
                          quoteDate,
                          execution))
              .orElseGet(
                  () -> this.carriedForward(player, current, quoteDate, calculatedAt, execution));
      Optional<PlayerQuote> existing =
          this.quotes.findByPlayerIdAndEffectiveDate(player.getId(), quoteDate);
      if (existing.isPresent()) {
        existing.get().replaceCalculation(next);
        next = existing.get();
      }
      this.quotes.saveAndFlush(next);
      Publication publication =
          next.getOrigin() == QuoteOrigin.RECOMPUTED
              ? Publication.RECOMPUTED
              : Publication.CARRIED_FORWARD;
      return new PublicationResult(publication, Optional.of(next));
    } catch (RuntimeException exception) {
      LOGGER.error(
          "quote_player_publication_failed playerId={} stage=publish cause={}",
          player.getId(),
          exception.getClass().getSimpleName());
      throw exception;
    }
  }

  /** Publica una cotización individual sin consultar score deportivo. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PublicationResult publishMarketDemand(
      Player player,
      LocalDate quoteDate,
      BigDecimal variationCap,
      long minPriceFloor,
      Instant calculatedAt,
      QuoteExecution execution) {
    try {
      this.players.lockIdentityById(player.getId()).orElseThrow();
      PlayerQuote current = this.quotes.findLatestByPlayerId(player.getId()).getFirst();
      if (current.getEffectiveDate().isAfter(quoteDate)) {
        return new PublicationResult(Publication.NOT_REPRICED, Optional.empty());
      }
      OffsetDateTime endsAt = execution.getStartedAt().atOffset(ZoneOffset.UTC);
      OffsetDateTime startsAt = endsAt.minus(execution.getDemandWindow());
      long netDemandTokens =
          this.transactions.netDemandTokensForPlayer(player.getId(), startsAt, endsAt);
      QuoteAdjustment adjustment =
          QuoteAdjustment.calculateMarketDemand(
              current.getPrice(), netDemandTokens, variationCap, minPriceFloor);
      PlayerQuote next =
          new PlayerQuote(
              player,
              quoteDate,
              adjustment.price(),
              current.getPrice(),
              adjustment.multiplier(),
              null,
              null,
              null,
              null,
              QuoteOrigin.MARKET_DEMAND,
              calculatedAt,
              execution,
              null,
              adjustment.marketDemandFactor(),
              adjustment.marketMultiplier(),
              netDemandTokens);
      Optional<PlayerQuote> existing =
          this.quotes.findByPlayerIdAndEffectiveDate(player.getId(), quoteDate);
      if (existing.isPresent()) {
        existing.get().replaceCalculation(next);
        next = existing.get();
      }
      this.quotes.saveAndFlush(next);
      return new PublicationResult(Publication.MARKET_DEMAND, Optional.of(next));
    } catch (RuntimeException exception) {
      LOGGER.error(
          "quote_player_market_demand_publication_failed playerId={} stage=publish cause={}",
          player.getId(),
          exception.getClass().getSimpleName());
      throw exception;
    }
  }

  private PlayerQuote recomputed(
      Player player,
      PlayerQuote current,
      QuoteScoreResolver.ResolvedScore resolved,
      BigDecimal adjustmentFactor,
      BigDecimal variationCap,
      long minPriceFloor,
      Instant calculatedAt,
      LocalDate quoteDate,
      QuoteExecution execution) {
    OffsetDateTime endsAt = execution.getStartedAt().atOffset(ZoneOffset.UTC);
    OffsetDateTime startsAt = endsAt.minus(execution.getDemandWindow());
    long netDemandTokens =
        this.transactions.netDemandTokensForPlayer(player.getId(), startsAt, endsAt);
    QuoteAdjustment adjustment =
        QuoteAdjustment.calculateWithMarketDemand(
            current.getPrice(),
            resolved.score(),
            adjustmentFactor,
            netDemandTokens,
            variationCap,
            minPriceFloor);
    return new PlayerQuote(
        player,
        quoteDate,
        adjustment.price(),
        current.getPrice(),
        adjustment.multiplier(),
        resolved.score(),
        resolved.strategy(),
        resolved.strategyVersion(),
        resolved.valuationDate(),
        QuoteOrigin.RECOMPUTED,
        calculatedAt,
        execution,
        adjustment.sportMultiplier(),
        adjustment.marketDemandFactor(),
        adjustment.marketMultiplier(),
        netDemandTokens);
  }

  private PlayerQuote carriedForward(
      Player player,
      PlayerQuote current,
      LocalDate quoteDate,
      Instant calculatedAt,
      QuoteExecution execution) {
    return new PlayerQuote(
        player,
        quoteDate,
        current.getPrice(),
        current.getPrice(),
        BigDecimal.ONE,
        null,
        current.getStrategyName(),
        current.getStrategyVersion(),
        null,
        QuoteOrigin.CARRIED_FORWARD,
        calculatedAt,
        execution);
  }

  /** Resultado observable de la publicación individual. */
  public enum Publication {
    RECOMPUTED,
    MARKET_DEMAND,
    CARRIED_FORWARD,
    NOT_REPRICED
  }

  /** Resultado de la publicación, sin fila cuando no correspondía repreciar. */
  public record PublicationResult(Publication publication, Optional<PlayerQuote> quote) {}
}
