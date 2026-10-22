package footballmarket.services.internal;

import footballmarket.models.Player;
import footballmarket.models.PlayerQuote;
import footballmarket.models.QuoteAdjustment;
import footballmarket.models.enums.QuoteOrigin;
import footballmarket.models.enums.ValuationStrategy;
import footballmarket.repositories.PlayerQuoteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Publica atómicamente la cotización de un jugador dentro de una corrida manual. */
@Component
public class QuotePlayerPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuotePlayerPublisher.class);
  private final PlayerQuoteRepository quotes;
  private final QuoteScoreResolver scores;

  public QuotePlayerPublisher(PlayerQuoteRepository quotes, QuoteScoreResolver scores) {
    this.quotes = quotes;
    this.scores = scores;
  }

  /**
   * Publica una fila recalculada o arrastrada, o evita escribir cuando no hay valuación posterior.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public PublicationResult publish(
      Player player,
      LocalDate quoteDate,
      ValuationStrategy strategy,
      BigDecimal adjustmentFactor,
      BigDecimal variationCap,
      long minPriceFloor,
      Instant calculatedAt) {
    try {
      PlayerQuote current = this.quotes.findLatestByPlayerId(player.getId()).getFirst();
      Optional<QuoteScoreResolver.ResolvedScore> resolved =
          this.scores.resolve(player.getId(), strategy);
      LocalDate referenceDate = this.quotes.findReferenceDatesByPlayerId(player.getId()).getFirst();
      if (resolved.isPresent() && !resolved.get().valuationDate().isAfter(referenceDate)) {
        return new PublicationResult(Publication.NOT_REPRICED, Optional.empty());
      }
      if (resolved.isEmpty() && !quoteDate.isAfter(referenceDate)) {
        return new PublicationResult(Publication.NOT_REPRICED, Optional.empty());
      }
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
                          calculatedAt))
              .orElseGet(() -> this.carriedForward(player, current, quoteDate, calculatedAt));
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
          exception.getClass().getSimpleName(),
          exception);
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
      Instant calculatedAt) {
    QuoteAdjustment adjustment =
        QuoteAdjustment.calculate(
            current.getPrice(), resolved.score(), adjustmentFactor, variationCap, minPriceFloor);
    return new PlayerQuote(
        player,
        resolved.valuationDate(),
        adjustment.price(),
        current.getPrice(),
        adjustment.multiplier(),
        resolved.score(),
        resolved.strategy(),
        resolved.strategyVersion(),
        resolved.valuationDate(),
        QuoteOrigin.RECOMPUTED,
        calculatedAt);
  }

  private PlayerQuote carriedForward(
      Player player, PlayerQuote current, LocalDate quoteDate, Instant calculatedAt) {
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
        calculatedAt);
  }

  /** Resultado observable de la publicación individual. */
  public enum Publication {
    RECOMPUTED,
    CARRIED_FORWARD,
    NOT_REPRICED
  }

  /** Resultado de la publicación, sin fila cuando no correspondía repreciar. */
  public record PublicationResult(Publication publication, Optional<PlayerQuote> quote) {}
}
