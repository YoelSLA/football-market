package footballmarket.features.quote.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.BuyOrderService;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.quote.application.QuoteMarketInitializationService;
import footballmarket.features.quote.application.QuoteRecalculationService;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.models.PlayerQuote;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.valuation.application.ValuationEvaluationService;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.MarketTestData;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import footballmarket.support.ValuationTestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.convention.TestBean;

/** Verifica que el recálculo combinado persista íntegra su auditoría deportiva y de mercado. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class QuoteSportAndMarketPersistenceTest {
  private static final LocalDate QUOTE_DATE = LocalDate.of(2026, 10, 19);
  private static final Instant NOW = Instant.parse("2026-10-19T09:00:00Z");

  @Autowired private PlayerCatalogService playerCatalog;
  @Autowired private QuoteMarketInitializationService initialization;
  @Autowired private QuoteRecalculationService recalculation;
  @Autowired private ValuationEvaluationService valuation;
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private ValuationTestData valuationData;
  @Autowired private MarketTestData market;
  @Autowired private BuyOrderService buyOrderService;
  @Autowired private PlayerQuoteRepository quotes;

  @TestBean(name = "marketClock", methodName = "quoteClock")
  private Clock marketClock;

  static Clock quoteClock() {
    return new ControlledClock();
  }

  @Nested
  @DisplayName("Auditoría persistida del recálculo combinado")
  class CombinedQuoteAudit {
    @Test
    @DisplayName("Persiste precio anterior, factores, demanda, límites y resultado")
    void persistsCombinedCalculationComponents() {
      // Arrange
      ControlledClock clock = (ControlledClock) marketClock;
      clock.time.set(NOW);
      catalogue.prepare();
      valuationData.prepareConfirmedJsonCampaign(QUOTE_DATE.minusDays(1));
      valuation.evaluate();
      initialization.initialize(QUOTE_DATE.minusDays(2));
      Player player = playerCatalog.getActivePlayers(0, 10).getContent().getFirst();
      User buyer = market.registerBuyer();
      User superuser = market.registerSuperuser();
      market.emitInitialTokens(player, superuser);
      buyOrderService.buy(buyer.getEmail(), new BuyOrderRequest(player.getId(), 10));
      clock.time.set(NOW.plusSeconds(1));
      long previousPrice = quotes.findLatestByPlayerId(player.getId()).getFirst().getPrice();

      // Act
      QuoteExecution execution =
          recalculation.recalculate("POSITIONAL_PERFORMANCE", null, null, null);

      // Assert
      PlayerQuote persisted =
          quotes.findByPlayerIdAndEffectiveDate(player.getId(), QUOTE_DATE).orElseThrow();
      assertThat(persisted.getPreviousPrice()).isEqualTo(previousPrice);
      assertThat(persisted.getOrigin()).isEqualTo(QuoteOrigin.RECOMPUTED);
      assertThat(persisted.getSportMultiplier()).isNotNull();
      assertThat(persisted.getMarketNetDemandTokens()).isEqualTo(10L);
      assertThat(persisted.getMarketDemandFactor()).isEqualByComparingTo("0.050000");
      assertThat(persisted.getMarketMultiplier()).isEqualByComparingTo("1.050000");
      assertThat(persisted.getMultiplier())
          .isEqualByComparingTo(
              persisted
                  .getSportMultiplier()
                  .multiply(persisted.getMarketMultiplier())
                  .max(BigDecimal.ONE.subtract(execution.getVariationCap()))
                  .min(BigDecimal.ONE.add(execution.getVariationCap()))
                  .setScale(6, java.math.RoundingMode.HALF_UP));
      assertThat(persisted.getPrice()).isGreaterThanOrEqualTo(execution.getMinPriceFloor());
      assertThat(persisted.getExecution().getId()).isEqualTo(execution.getId());
      assertThat(execution.getDemandWindow()).isEqualTo(java.time.Duration.ofHours(24));
    }
  }

  static final class ControlledClock extends Clock {
    private final AtomicReference<Instant> time = new AtomicReference<>(NOW);

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(time.get(), zone);
    }

    @Override
    public Instant instant() {
      return time.get();
    }
  }
}
