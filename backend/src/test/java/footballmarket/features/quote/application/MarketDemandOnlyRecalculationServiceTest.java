package footballmarket.features.quote.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.BuyOrderService;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.quote.application.internal.QuotePlayerPublisher;
import footballmarket.features.quote.application.internal.QuoteScoreResolver;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.enums.QuoteRecalculationType;
import footballmarket.features.quote.domain.exceptions.InvalidQuoteException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@ActiveProfiles("test")
@Import({
  TestcontainersConfiguration.class,
  IdentityCatalogueFixture.class,
  MarketTestData.class,
  ValuationTestData.class,
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class MarketDemandOnlyRecalculationServiceTest {
  private static final LocalDate QUOTE_DATE = LocalDate.of(2026, 10, 19);
  private static final Instant NOW = Instant.parse("2026-10-19T09:00:00Z");

  @Autowired private QuoteRecalculationService service;
  @Autowired private QuoteMarketInitializationService initialization;
  @Autowired private QuoteQueryService quoteQuery;
  @Autowired private BuyOrderService buyOrderService;
  @Autowired private ValuationEvaluationService valuation;
  @Autowired private PlayerCatalogService players;
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private ValuationTestData valuationData;
  @Autowired private MarketTestData market;
  @MockitoSpyBean private QuoteScoreResolver scoreResolver;
  @MockitoSpyBean private QuotePlayerPublisher publisher;

  @TestBean(name = "statisticsClock", methodName = "valuationClock")
  private Clock statisticsClock;

  @TestBean(name = "marketClock", methodName = "quoteClock")
  private Clock marketClock;

  static Clock valuationClock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }

  static Clock quoteClock() {
    return new ControlledClock();
  }

  @BeforeEach
  void resetClock() {
    ((ControlledClock) marketClock).time.set(NOW);
  }

  @Nested
  @DisplayName("Recálculo manual solo por mercado")
  class MarketOnlyRecalculation {
    @Test
    @DisplayName("Procesa toda la población sin consultar scores y audita solo mercado")
    void recalculatesPopulationWithoutResolvingSportsScores() {
      // Arrange
      prepareMarket(true);
      Player player = players.getActivePlayers(0, 10).getContent().getFirst();
      User buyer = market.registerBuyer();
      User superuser = market.registerSuperuser();
      market.emitInitialTokens(player, superuser);
      buyOrderService.buy(buyer.getEmail(), new BuyOrderRequest(player.getId(), 10));
      ((ControlledClock) marketClock).time.set(NOW.plusSeconds(1));

      // Act
      QuoteExecution execution = service.recalculateMarketDemand();

      // Assert
      PlayerQuote quote = quoteQuery.findOnDate(player.getId(), QUOTE_DATE);
      assertThat(execution.getOrigin()).isEqualTo(QuoteExecutionOrigin.MANUAL);
      assertThat(execution.getRecalculationType())
          .isEqualTo(QuoteRecalculationType.MARKET_DEMAND_ONLY);
      assertThat(execution.getStrategyName()).isNull();
      assertThat(execution.getStrategyVersion()).isNull();
      assertThat(execution.getAdjustmentFactor()).isNull();
      assertThat(execution.getPricedPlayers()).isEqualTo(5);
      assertThat(execution.getRecomputedRows()).isEqualTo(5);
      assertThat(execution.getFailedPlayers()).isZero();
      assertThat(quote.getOrigin()).isEqualTo(QuoteOrigin.MARKET_DEMAND);
      assertThat(quote.getMarketNetDemandTokens()).isEqualTo(10L);
      assertThat(quote.getMarketDemandFactor()).isEqualByComparingTo("0.050000");
      assertThat(quote.getSportMultiplier()).isNull();
      assertThat(quote.getScore()).isNull();
      verifyNoInteractions(scoreResolver);
    }

    @Test
    @DisplayName("Registra cotizaciones sin variación cuando no hay demanda")
    void recordsUnchangedQuotesWithoutMarketActivity() {
      // Arrange
      prepareMarket(false);

      // Act
      QuoteExecution execution = service.recalculateMarketDemand();
      Player player = players.getActivePlayers(0, 10).getContent().getFirst();
      PlayerQuote quote = quoteQuery.findOnDate(player.getId(), QUOTE_DATE);

      // Assert
      assertThat(execution.getRecomputedRows()).isEqualTo(5);
      assertThat(execution.getFailedPlayers()).isZero();
      assertThat(quote.getPrice()).isEqualTo(100L);
      assertThat(quote.getMarketNetDemandTokens()).isZero();
      assertThat(quote.getMarketDemandFactor()).isEqualByComparingTo("0.000000");
      verifyNoInteractions(scoreResolver);
    }

    @Test
    @DisplayName("Registra un fallo individual y continúa con los demás jugadores")
    void continuesAfterIndividualPublicationFailure() {
      // Arrange
      prepareMarket(false);
      Player failedPlayer = players.getActivePlayers(0, 10).getContent().getFirst();
      doThrow(new InvalidQuoteException("Fallo individual controlado"))
          .when(publisher)
          .publishMarketDemand(
              argThat(player -> player.getId().equals(failedPlayer.getId())),
              any(LocalDate.class),
              any(BigDecimal.class),
              anyLong(),
              any(Instant.class),
              any(QuoteExecution.class));

      // Act
      QuoteExecution execution = service.recalculateMarketDemand();

      // Assert
      assertThat(execution.getPricedPlayers()).isEqualTo(5);
      assertThat(execution.getRecomputedRows()).isEqualTo(4);
      assertThat(execution.getFailedPlayers()).isEqualTo(1);
      verifyNoInteractions(scoreResolver);
    }
  }

  private void prepareMarket(boolean valued) {
    catalogue.prepare();
    valuationData.prepareConfirmedJsonCampaign(QUOTE_DATE.minusDays(1));
    if (valued) {
      valuation.evaluate();
    }
    initialization.initialize(QUOTE_DATE.minusDays(2));
  }

  static class ControlledClock extends Clock {
    final AtomicReference<Instant> time = new AtomicReference<>(NOW);

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
