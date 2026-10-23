package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.models.MarketTransaction;
import footballmarket.models.Player;
import footballmarket.models.PlayerQuote;
import footballmarket.models.QuoteExecution;
import footballmarket.models.User;
import footballmarket.models.enums.QuoteOrigin;
import footballmarket.models.records.BuyOrderRequest;
import footballmarket.services.BuyOrderService;
import footballmarket.services.PlayerCatalogService;
import footballmarket.services.QuoteMarketInitializationService;
import footballmarket.services.QuoteRecalculationService;
import footballmarket.services.ValuationEvaluationService;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.MarketTestData;
import footballmarket.support.QuoteTestData;
import footballmarket.support.TestcontainersConfiguration;
import footballmarket.support.ValuationTestData;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Categoría Repository de la operación del mercado sobre la cotización persistida.
 *
 * <p>Afirma sobre el estado durable lo que la fuente real habilita: la compra consume el precio y
 * el instante de cálculo de la cotización vigente persistida, y un recálculo posterior no altera
 * saldos, tenencias ni transacciones. Invoca los Services reales y no sustituye la fuente de
 * cotización, de modo que el puerto vigente queda resuelto contra el repositorio canónico.
 */
@SpringBootTest
@Import({
  TestcontainersConfiguration.class,
  QuoteTestData.class,
  IdentityCatalogueFixture.class,
  ValuationTestData.class
})
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("integration")
class QuoteMarketOperationPersistenceTest {

  private static final LocalDate MARKET_DATE = LocalDate.of(2026, 10, 17);
  private static final LocalDate CAMPAIGN_DATE = LocalDate.of(2026, 10, 18);

  @Autowired private BuyOrderService buyOrderService;
  @Autowired private QuoteMarketInitializationService initialization;
  @Autowired private QuoteRecalculationService recalculation;
  @Autowired private ValuationEvaluationService valuation;
  @Autowired private PlayerCatalogService playerCatalogue;
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private ValuationTestData data;
  @Autowired private MarketTestData market;
  @Autowired private UserCreditRepository credits;
  @Autowired private PlayerTokenRepository holdings;
  @Autowired private MarketTransactionRepository transactions;
  @Autowired private PlayerQuoteRepository quotes;

  @Nested
  @DisplayName("Compra sobre la cotización persistida")
  class PurchaseOverPersistedQuote {

    @Test
    @DisplayName("La compra usa el precio y el instante de cálculo de la cotización vigente")
    void consumeElPrecioYElInstantePersistidos() {
      // Arrange
      Player player = prepareInitializedMarket();
      User buyer = market.registerBuyer();
      User superuser = market.registerSuperuser();
      market.emitInitialTokens(player, superuser);
      PlayerQuote persisted = quotes.findLatestByPlayerId(player.getId()).getFirst();

      // Act
      MarketTransaction purchase =
          buyOrderService.buy(buyer.getEmail(), new BuyOrderRequest(player.getId(), 10));

      // Assert
      assertThat(purchase.getUnitPrice()).isEqualTo(persisted.getPrice());
      assertThat(purchase.getQuoteCalculatedAt())
          .isEqualTo(persisted.getCalculatedAt().atOffset(java.time.ZoneOffset.UTC));
    }
  }

  @Nested
  @DisplayName("Aislamiento del recálculo respecto del mercado")
  class RecalculationIsolation {

    @Test
    @DisplayName("El recálculo no altera saldos, tenencias ni transacciones")
    void elRecalculoNoAlteraElMercado() {
      // Arrange
      Player player = prepareInitializedMarket();
      User buyer = market.registerBuyer();
      User superuser = market.registerSuperuser();
      market.emitInitialTokens(player, superuser);
      buyOrderService.buy(buyer.getEmail(), new BuyOrderRequest(player.getId(), 10));
      long creditsBefore = credits.findById(buyer.getId()).orElseThrow().getAvailableCredits();
      int tokensBefore =
          holdings
              .findByPlayerIdAndOwnerId(player.getId(), buyer.getId())
              .orElseThrow()
              .getQuantity();
      long transactionsBefore = transactions.count();

      // Act
      QuoteExecution execution =
          recalculation.recalculate("POSITIONAL_PERFORMANCE", null, null, null);

      // Assert
      assertThat(execution.getRecomputedRows()).isPositive();
      assertThat(credits.findById(buyer.getId()).orElseThrow().getAvailableCredits())
          .isEqualTo(creditsBefore);
      assertThat(
              holdings
                  .findByPlayerIdAndOwnerId(player.getId(), buyer.getId())
                  .orElseThrow()
                  .getQuantity())
          .isEqualTo(tokensBefore);
      assertThat(transactions.count()).isEqualTo(transactionsBefore);
      assertThat(quotes.findLatestByPlayerId(player.getId()).getFirst().getOrigin())
          .isEqualTo(QuoteOrigin.RECOMPUTED);
    }
  }

  private Player prepareInitializedMarket() {
    if (!quotes.existsByOrigin(QuoteOrigin.INITIAL)) {
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(CAMPAIGN_DATE);
      valuation.evaluate();
      initialization.initialize(MARKET_DATE);
    }
    return playerCatalogue.getActivePlayers(0, 10).getContent().getFirst();
  }
}
