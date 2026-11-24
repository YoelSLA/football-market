package footballmarket.features.market.application.impl;

import footballmarket.features.market.application.MarketTrendsService;
import footballmarket.features.market.domain.records.MarketHotPlayer;
import footballmarket.features.market.domain.records.MarketTrends;
import footballmarket.features.market.infraestructure.MarketTransactionRepository;
import footballmarket.features.market.infraestructure.projections.MarketTrendProjection;
import footballmarket.features.market.infraestructure.projections.PlayerMarketDemandProjection;
import footballmarket.features.quote.infraestructure.config.QuoteProperties;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementa la consulta de tendencias usando agregaciones de persistencia. */
@Service
public class MarketTrendsServiceImpl implements MarketTrendsService {
  private final MarketTransactionRepository marketTransactionRepository;
  private final QuoteProperties quoteProperties;
  private final Clock clock;

  public MarketTrendsServiceImpl(
      MarketTransactionRepository marketTransactionRepository,
      QuoteProperties quoteProperties,
      Clock marketClock) {
    this.marketTransactionRepository = marketTransactionRepository;
    this.quoteProperties = quoteProperties;
    this.clock = marketClock;
  }

  /** Captura un solo instante y calcula porcentajes por transacción, no por volumen de tokens. */
  @Override
  @Transactional(readOnly = true)
  public MarketTrends getMarketTrends() {
    OffsetDateTime endsAt = OffsetDateTime.now(this.clock).withOffsetSameInstant(ZoneOffset.UTC);
    OffsetDateTime startsAt = endsAt.minus(this.quoteProperties.demandWindow());
    MarketTrendProjection totals =
        this.marketTransactionRepository.aggregateTrends(startsAt, endsAt);
    long totalTransactions = valueOrZero(totals.getTotalTransactions());
    long buyTransactions = valueOrZero(totals.getBuyTransactions());
    long sellTransactions = valueOrZero(totals.getSellTransactions());
    Double buyPercentage =
        totalTransactions == 0 ? null : percentage(buyTransactions, totalTransactions);
    Double sellPercentage =
        totalTransactions == 0 ? null : percentage(sellTransactions, totalTransactions);
    String marketBias =
        totalTransactions == 0
            ? null
            : buyTransactions > sellTransactions
                ? "BULLISH"
                : buyTransactions < sellTransactions ? "BEARISH" : "NEUTRAL";
    List<MarketHotPlayer> topPlayers =
        this.marketTransactionRepository.findTopBoughtPlayers(startsAt, endsAt).stream()
            .map(MarketTrendsServiceImpl::toHotPlayer)
            .toList();
    return new MarketTrends(
        startsAt,
        endsAt,
        topPlayers,
        buyPercentage,
        sellPercentage,
        marketBias,
        totalTransactions,
        valueOrZero(totals.getTotalTradedTokens()),
        valueOrZero(totals.getTotalTradedCredits()));
  }

  private static MarketHotPlayer toHotPlayer(PlayerMarketDemandProjection projection) {
    return new MarketHotPlayer(
        projection.getPlayerId(),
        projection.getPlayerName(),
        valueOrZero(projection.getBoughtTokens()),
        valueOrZero(projection.getBuyVolume()));
  }

  private static long valueOrZero(Long value) {
    return value == null ? 0L : value;
  }

  private static double percentage(long numerator, long denominator) {
    return numerator * 100.0 / denominator;
  }
}
