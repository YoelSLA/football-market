package footballmarket.features.businessmetrics.application.impl;

import footballmarket.features.businessmetrics.application.BusinessMetricsService;
import footballmarket.features.businessmetrics.domain.records.BusinessMetricsTotals;
import footballmarket.features.market.domain.enums.TokenListingStatus;
import footballmarket.features.market.infraestructure.MarketTransactionRepository;
import footballmarket.features.market.infraestructure.TokenListingRepository;
import footballmarket.features.market.infraestructure.projections.BusinessTransactionTotalsProjection;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementa las consultas históricas y de ejecución actual de métricas de negocio. */
@Service
public class BusinessMetricsServiceImpl implements BusinessMetricsService {
  static final String PURCHASES = "football_market.business.purchases";
  static final String SALES = "football_market.business.sales";
  static final String CANCELLED_LISTINGS = "football_market.business.cancelled_listings";
  static final String TOKENS_TRANSFERRED = "football_market.business.tokens_transferred";
  static final String CREDITS_TRADED_IN_CENTS = "football_market.business.credits_traded_in_cents";

  private final MarketTransactionRepository marketTransactionRepository;
  private final TokenListingRepository tokenListingRepository;
  private final Clock clock;
  private final MeterRegistry meterRegistry;

  public BusinessMetricsServiceImpl(
      MarketTransactionRepository marketTransactionRepository,
      TokenListingRepository tokenListingRepository,
      Clock marketClock,
      MeterRegistry meterRegistry) {
    this.marketTransactionRepository = marketTransactionRepository;
    this.tokenListingRepository = tokenListingRepository;
    this.clock = marketClock;
    this.meterRegistry = meterRegistry;
  }

  /** Lee cada fuente persistida y fija un único instante UTC para la respuesta. */
  @Override
  @Transactional(readOnly = true)
  public BusinessMetricsTotals getHistoricalMetrics() {
    BusinessTransactionTotalsProjection transactions =
        this.marketTransactionRepository.aggregateBusinessMetrics();
    return new BusinessMetricsTotals(
        valueOrZero(transactions.getPurchases()),
        valueOrZero(transactions.getSales()),
        this.tokenListingRepository.countByStatus(TokenListingStatus.CANCELLED),
        valueOrZero(transactions.getTokensTransferred()),
        valueOrZero(transactions.getCreditsTradedInCents()),
        Instant.now(this.clock));
  }

  /** Lee los contadores de cardinalidad fija acumulados en el proceso actual. */
  @Override
  @Transactional(readOnly = true)
  public BusinessMetricsTotals getCurrentMetrics() {
    return new BusinessMetricsTotals(
        counterValue(PURCHASES),
        counterValue(SALES),
        counterValue(CANCELLED_LISTINGS),
        counterValue(TOKENS_TRANSFERRED),
        counterValue(CREDITS_TRADED_IN_CENTS),
        Instant.now(this.clock));
  }

  private long counterValue(String name) {
    return (long) this.meterRegistry.counter(name).count();
  }

  private static long valueOrZero(Long value) {
    return value == null ? 0L : value;
  }
}
