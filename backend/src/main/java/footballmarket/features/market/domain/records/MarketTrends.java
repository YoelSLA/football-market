package footballmarket.features.market.domain.records;

import java.time.OffsetDateTime;
import java.util.List;

/** Métricas agregadas de actividad del mercado en una ventana temporal. */
public record MarketTrends(
    OffsetDateTime startsAt,
    OffsetDateTime endsAt,
    List<MarketHotPlayer> topHotPlayers,
    Double buyTransactionsPercentage,
    Double sellTransactionsPercentage,
    String marketBias,
    long totalTransactions,
    long totalTradedTokens,
    long totalTradedCredits) {}
