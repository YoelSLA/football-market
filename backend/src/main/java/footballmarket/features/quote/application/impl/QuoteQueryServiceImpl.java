package footballmarket.features.quote.application.impl;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.quote.application.QuoteQueryService;
import footballmarket.features.quote.application.exceptions.QuoteNotFoundException;
import footballmarket.features.quote.application.internal.QuotePopulationSelector;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.models.PlayerQuote;
import footballmarket.features.quote.domain.records.PlayerRankingEntry;
import footballmarket.features.quote.domain.records.QuoteRankingPage;
import footballmarket.features.quote.domain.records.QuoteRankingQuery;
import footballmarket.features.quote.infraestructure.PlayerQuoteRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta del historial, de la cotización vigente y del ranking de un jugador.
 *
 * <p>El historial es la propia tabla de cotizaciones: la vigente es la fila de fecha de vigencia
 * máxima, con una única fila diaria por jugador. El ranking reúne esa misma fila vigente de cada
 * jugador cotizado y la ordena por precio descendente. Ninguna consulta recalcula precios.
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
    List<PlayerQuote> rows = this.quotes.findRanking(pricable);
    List<PlayerRankingEntry> ranking =
        java.util.stream.IntStream.range(0, rows.size())
            .mapToObj(
                index -> {
                  PlayerQuote quote = rows.get(index);
                  return new PlayerRankingEntry(
                      quote.getPlayer().getId(),
                      quote.getPrice(),
                      quote.getEffectiveDate(),
                      quote.getOrigin(),
                      quote.getCalculatedAt(),
                      index + 1L,
                      null);
                })
            .toList();
    LOG.debug("quote_ranking_read player={} entries={}", pricable.size(), ranking.size());
    return ranking;
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public QuoteRankingPage findRanking(QuoteRankingQuery query) {
    List<PlayerRankingEntry> entries =
        this.quotes
            .findRankingPage(
                query.name(), query.leagueId(), query.position(), query.size(), query.offset())
            .stream()
            .map(
                row ->
                    new PlayerRankingEntry(
                        row.getPlayerId(),
                        row.getPrice(),
                        row.getEffectiveDate(),
                        QuoteOrigin.valueOf(row.getOrigin()),
                        row.getCalculatedAt(),
                        row.getPosition(),
                        new QuoteRankingPage.PlayerIdentity(
                            row.getName(),
                            row.getPlayerPosition(),
                            row.getImageUrl(),
                            row.getTeamId() == null
                                ? null
                                : new QuoteRankingPage.TeamIdentity(
                                    row.getTeamId(), row.getTeamName(), null),
                            row.getLeagueId() == null
                                ? null
                                : new QuoteRankingPage.LeagueOption(
                                    row.getLeagueId(), row.getLeagueName()))))
            .toList();
    long total = this.quotes.countRanking(query.name(), query.leagueId(), query.position());
    return new QuoteRankingPage(
        entries,
        query.page(),
        query.size(),
        total,
        this.quotes.findRankingLeagues(),
        this.quotes.findRankingPositions());
  }
}
