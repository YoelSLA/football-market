package footballmarket.services.impl;

import footballmarket.config.QuoteProperties;
import footballmarket.models.Player;
import footballmarket.models.PlayerQuote;
import footballmarket.models.enums.QuoteOrigin;
import footballmarket.models.records.MarketInitializationResult;
import footballmarket.repositories.PlayerQuoteRepository;
import footballmarket.services.QuoteMarketInitializationService;
import footballmarket.services.exceptions.MarketAlreadyInitializedException;
import footballmarket.services.internal.QuotePopulationSelector;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicialización del mercado.
 *
 * <p>La fecha de mercado se deriva de las propias filas {@code INITIAL}: no existe estado paralelo
 * del mercado. Repetir la inicialización en la misma fecha es idempotente y cualquier otra fecha se
 * rechaza, porque escribir una segunda fila inicial por jugador violaría la unicidad del ancla.
 */
@Service
@Transactional
public class QuoteMarketInitializationServiceImpl implements QuoteMarketInitializationService {
  private final PlayerQuoteRepository quotes;
  private final QuotePopulationSelector population;
  private final QuoteProperties properties;
  private final Clock marketClock;

  public QuoteMarketInitializationServiceImpl(
      PlayerQuoteRepository quotes,
      QuotePopulationSelector population,
      QuoteProperties properties,
      Clock marketClock) {
    this.quotes = quotes;
    this.population = population;
    this.properties = properties;
    this.marketClock = marketClock;
  }

  @Override
  public MarketInitializationResult initialize(LocalDate marketDate) {
    LocalDate requested = marketDate == null ? LocalDate.now(this.marketClock) : marketDate;
    List<LocalDate> existingMarketDates = this.quotes.findMarketDates();
    if (!existingMarketDates.isEmpty()) {
      LocalDate currentMarketDate = existingMarketDates.getFirst();
      if (!currentMarketDate.equals(requested)) {
        throw new MarketAlreadyInitializedException();
      }
      return new MarketInitializationResult(
          currentMarketDate, Math.toIntExact(this.quotes.countByOrigin(QuoteOrigin.INITIAL)), true);
    }
    Instant calculatedAt = Instant.now(this.marketClock);
    long anchor = this.properties.initialAnchor();
    List<Player> players = this.population.pricablePlayers();
    for (Player player : players) {
      this.quotes.save(
          new PlayerQuote(
              player,
              requested,
              anchor,
              null,
              BigDecimal.ONE,
              null,
              null,
              null,
              null,
              QuoteOrigin.INITIAL,
              calculatedAt));
    }
    return new MarketInitializationResult(requested, players.size(), false);
  }
}
