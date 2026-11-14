package footballmarket.features.quote.application.impl;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.market.application.exceptions.MarketAlreadyInitializedException;
import footballmarket.features.market.domain.records.MarketInitializationResult;
import footballmarket.features.quote.application.QuoteMarketInitializationService;
import footballmarket.features.quote.application.internal.QuotePopulationSelector;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.models.PlayerQuote;
import footballmarket.features.quote.infraestructure.PlayerQuoteRepository;
import footballmarket.features.quote.infraestructure.config.QuoteProperties;
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
 * <p>La fecha de mercado se deriva de la primera fecha del historial: no existe estado paralelo del
 * mercado. Repetir la inicialización en la misma fecha es idempotente y cualquier otra fecha se
 * rechaza, porque escribir una segunda fila inicial por jugador violaría la unicidad del ancla.
 */
@Service
@Transactional
public class QuoteMarketInitializationServiceImpl implements QuoteMarketInitializationService {
  private final PlayerQuoteRepository quotes;
  private final QuotePopulationSelector population;
  private final QuoteProperties properties;
  private final Clock marketClock;
  private final PlayerRepository players;

  public QuoteMarketInitializationServiceImpl(
      PlayerQuoteRepository quotes,
      QuotePopulationSelector population,
      QuoteProperties properties,
      Clock marketClock,
      PlayerRepository players) {
    this.quotes = quotes;
    this.population = population;
    this.properties = properties;
    this.marketClock = marketClock;
    this.players = players;
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
          currentMarketDate,
          Math.toIntExact(this.quotes.countByEffectiveDate(currentMarketDate)),
          true);
    }
    Instant calculatedAt = Instant.now(this.marketClock);
    long anchor = this.properties.initialAnchor();
    List<Player> players = this.population.pricablePlayers();
    int inserted = 0;
    for (Player player : players) {
      this.players.lockIdentityById(player.getId()).orElseThrow();
      List<LocalDate> confirmedDates = this.quotes.findMarketDates();
      if (!confirmedDates.isEmpty() && !confirmedDates.getFirst().equals(requested)) {
        throw new MarketAlreadyInitializedException();
      }
      if (this.quotes.existsByPlayerIdAndEffectiveDate(player.getId(), requested)) {
        continue;
      }
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
      inserted++;
    }
    return new MarketInitializationResult(
        requested,
        Math.toIntExact(this.quotes.countByEffectiveDate(requested)),
        inserted == 0 && !players.isEmpty());
  }
}
