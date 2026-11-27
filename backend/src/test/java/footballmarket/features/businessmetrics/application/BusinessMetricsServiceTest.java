package footballmarket.features.businessmetrics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import footballmarket.features.auth.domain.User;
import footballmarket.features.businessmetrics.domain.records.BusinessMetricsTotals;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.BuyOrderService;
import footballmarket.features.market.application.OfficialBuybackService;
import footballmarket.features.market.application.TokenListingService;
import footballmarket.features.market.application.exceptions.InvalidQuantityException;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.market.domain.records.SellOrderRequest;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.domain.records.CurrentQuote;
import footballmarket.support.MarketTestData;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Comprueba las métricas históricas y de ejecución con persistencia real. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class BusinessMetricsServiceTest {
  @Autowired private BusinessMetricsService businessMetricsService;
  @Autowired private BuyOrderService buyOrderService;
  @Autowired private OfficialBuybackService buybackService;
  @Autowired private TokenListingService tokenListingService;
  @Autowired private MarketTestData market;
  @Autowired private MeterRegistry meterRegistry;
  @MockitoBean private QuoteSource quotes;

  @MockitoBean
  @Qualifier("marketClock")
  private Clock marketClock;

  @BeforeEach
  void freezeMarketClock() {
    when(marketClock.getZone()).thenReturn(ZoneOffset.UTC);
    when(marketClock.instant()).thenReturn(Instant.parse("2026-11-26T12:00:00Z"));
  }

  @Nested
  @DisplayName("Consulta histórica")
  class HistoricalMetrics {
    @Test
    @DisplayName("Devuelve agregados vacíos en cero e instante UTC")
    void returnsZeroTotalsAndUtcQueryInstantWhenHistoryIsEmpty() {
      // Act
      BusinessMetricsTotals totals = businessMetricsService.getHistoricalMetrics();

      // Assert
      assertThat(totals.purchases()).isZero();
      assertThat(totals.sales()).isZero();
      assertThat(totals.cancelledListings()).isZero();
      assertThat(totals.tokensTransferred()).isZero();
      assertThat(totals.creditsTradedInCents()).isZero();
      assertThat(totals.queriedAt()).isNotNull();
      assertThat(totals.queriedAt().atOffset(ZoneOffset.UTC).getOffset()).isEqualTo(ZoneOffset.UTC);
      assertThat(totals.queriedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("Agrega operaciones y cancelaciones confirmadas desde sus registros")
    void aggregatesConfirmedTransactionsAndCancelledListings() {
      // Arrange
      User buyer = market.registerBuyer();
      User superuser = market.registerSuperuser();
      Player player = market.createVisiblePlayer();
      market.emitInitialTokens(player, superuser);
      market.initializeMarket(player);
      OffsetDateTime now = OffsetDateTime.now(marketClock);
      when(quotes.currentQuote(any()))
          .thenReturn(Optional.of(new CurrentQuote(player.getId(), 100L, now, "strategy-v1")));
      buyOrderService.buy(buyer.getEmail(), new BuyOrderRequest(player.getId(), 10));
      TokenListing listing =
          tokenListingService.publish(buyer.getEmail(), new SellOrderRequest(player.getId(), 5));
      tokenListingService.cancel(buyer.getEmail(), listing.getId());

      // Act
      BusinessMetricsTotals totals = businessMetricsService.getHistoricalMetrics();

      // Assert
      assertThat(totals.purchases()).isEqualTo(1L);
      assertThat(totals.sales()).isZero();
      assertThat(totals.cancelledListings()).isEqualTo(1L);
      assertThat(totals.tokensTransferred()).isEqualTo(10L);
      assertThat(totals.creditsTradedInCents()).isEqualTo(1000L);
      assertThat(totals.queriedAt()).isEqualTo(Instant.parse("2026-11-26T12:00:00Z"));
    }
  }

  @Nested
  @DisplayName("Métricas de la ejecución actual")
  class CurrentMetrics {
    @Test
    @DisplayName("Una compra rechazada no incrementa los contadores")
    void doesNotCountRejectedPurchase() {
      // Arrange
      User buyer = market.registerBuyer();
      BusinessMetricsTotals before = businessMetricsService.getCurrentMetrics();

      // Act / Assert
      assertThatThrownBy(
              () -> buyOrderService.buy(buyer.getEmail(), new BuyOrderRequest(999_999L, 0)))
          .isInstanceOf(InvalidQuantityException.class);
      BusinessMetricsTotals after = businessMetricsService.getCurrentMetrics();

      assertThat(after.purchases()).isEqualTo(before.purchases());
      assertThat(after.sales()).isEqualTo(before.sales());
      assertThat(after.cancelledListings()).isEqualTo(before.cancelledListings());
      assertThat(after.tokensTransferred()).isEqualTo(before.tokensTransferred());
      assertThat(after.creditsTradedInCents()).isEqualTo(before.creditsTradedInCents());
    }

    @Test
    @DisplayName("Acumula compras, venta oficial, cancelaciones y volúmenes por operación")
    void accumulatesCompletedOperationsAndVolumes() {
      // Arrange
      User buyer = market.registerBuyer();
      User superuser = market.registerSuperuser();
      market.fund(superuser, 10_000L);
      Player player = market.createVisiblePlayer();
      market.emitInitialTokens(player, superuser);
      market.initializeMarket(player);
      OffsetDateTime now = OffsetDateTime.now(marketClock);
      when(quotes.currentQuote(any()))
          .thenReturn(Optional.of(new CurrentQuote(player.getId(), 100L, now, "strategy-v1")));
      BusinessMetricsTotals before = businessMetricsService.getCurrentMetrics();

      // Act
      buyOrderService.buy(buyer.getEmail(), new BuyOrderRequest(player.getId(), 10));
      buybackService.sellToSuperuser(
          buyer.getEmail(),
          new footballmarket.features.market.domain.records.OfficialBuybackRequest(
              player.getId(), 2));
      TokenListing listing =
          tokenListingService.publish(buyer.getEmail(), new SellOrderRequest(player.getId(), 1));
      tokenListingService.cancel(buyer.getEmail(), listing.getId());
      BusinessMetricsTotals after = businessMetricsService.getCurrentMetrics();

      // Assert
      assertThat(after.purchases() - before.purchases()).isEqualTo(1L);
      assertThat(after.sales() - before.sales()).isEqualTo(1L);
      assertThat(after.cancelledListings() - before.cancelledListings()).isEqualTo(1L);
      assertThat(after.tokensTransferred() - before.tokensTransferred()).isEqualTo(12L);
      assertThat(after.creditsTradedInCents() - before.creditsTradedInCents()).isEqualTo(1180L);
      assertMetricHasNoTags("football_market.business.purchases");
      assertMetricHasNoTags("football_market.business.sales");
      assertMetricHasNoTags("football_market.business.cancelled_listings");
      assertMetricHasNoTags("football_market.business.tokens_transferred");
      assertMetricHasNoTags("football_market.business.credits_traded_in_cents");
    }

    private void assertMetricHasNoTags(String metricName) {
      assertThat(meterRegistry.find(metricName).meter().getId().getTags()).isEmpty();
    }
  }
}
