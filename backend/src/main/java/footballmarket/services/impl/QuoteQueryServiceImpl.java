package footballmarket.services.impl;

import footballmarket.models.Player;
import footballmarket.models.PlayerQuote;
import footballmarket.models.records.PlayerRankingEntry;
import footballmarket.repositories.PlayerQuoteRepository;
import footballmarket.services.QuoteQueryService;
import footballmarket.services.exceptions.QuoteNotFoundException;
import footballmarket.services.internal.QuotePopulationSelector;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta del historial, de la cotización vigente y del ranking de un jugador.
 *
 * <p>El historial es la propia tabla de cotizaciones: la vigente es la fila de fecha de vigencia
 * máxima y, a igualdad de fecha, la fila {@code RECOMPUTED}, escrita después que la arrastrada. El
 * ranking reúne esa misma fila vigente de cada jugador cotizado y la ordena por precio descendente.
 * Ninguna consulta recalcula precios.
 */
@Service
@Transactional(readOnly = true)
public class QuoteQueryServiceImpl implements QuoteQueryService {
  private static final Logger LOG = LoggerFactory.getLogger(QuoteQueryServiceImpl.class);

  private final PlayerQuoteRepository quotes;
  private final QuotePopulationSelector population;

  public QuoteQueryServiceImpl(PlayerQuoteRepository quotes, QuotePopulationSelector population) {
    this.quotes = quotes;
    this.population = population;
  }

  @Override
  public List<PlayerQuote> findHistory(Long playerId) {
    List<PlayerQuote> history = this.quotes.findHistoryByPlayerId(playerId);
    LOG.debug("quote_history_read playerId={} rows={}", playerId, history.size());
    return history;
  }

  @Override
  public PlayerQuote findCurrent(Long playerId) {
    PlayerQuote current =
        this.quotes.findLatestByPlayerId(playerId).stream()
            .findFirst()
            .orElseThrow(QuoteNotFoundException::new);
    LOG.debug(
        "quote_current_read playerId={} effectiveDate={} origin={} calculatedAt={}",
        playerId,
        current.getEffectiveDate(),
        current.getOrigin(),
        current.getCalculatedAt());
    return current;
  }

  @Override
  public PlayerQuote findOnDate(Long playerId, LocalDate date) {
    return this.quotes.findLatestByPlayerIdOnOrBefore(playerId, date).stream()
        .findFirst()
        .orElseThrow(QuoteNotFoundException::new);
  }

  @Override
  public List<PlayerRankingEntry> findRanking() {
    List<Long> pricable = this.population.pricablePlayers().stream().map(Player::getId).toList();
    if (pricable.isEmpty()) {
      return List.of();
    }
    List<PlayerRankingEntry> ranking =
        this.quotes.findRanking(pricable).stream()
            .map(
                quote ->
                    new PlayerRankingEntry(
                        quote.getPlayer().getId(),
                        quote.getPrice(),
                        quote.getEffectiveDate(),
                        quote.getOrigin(),
                        quote.getCalculatedAt()))
            .toList();
    LOG.debug("quote_ranking_read players={} entries={}", pricable.size(), ranking.size());
    return ranking;
  }
}
