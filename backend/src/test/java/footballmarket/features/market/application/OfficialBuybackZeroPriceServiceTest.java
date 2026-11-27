package footballmarket.features.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.market.domain.records.OfficialBuybackReceipt;
import footballmarket.features.market.domain.records.OfficialBuybackRequest;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.application.exceptions.InvalidQuotePriceException;
import footballmarket.features.quote.domain.records.CurrentQuote;
import footballmarket.support.MarketTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Verifica el rechazo de venta oficial cuando la comisión redondea el neto a cero. */
@SpringBootTest(properties = "market.buyback.discount-percentage=99")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
class OfficialBuybackZeroPriceServiceTest {

  @Autowired private OfficialBuybackService buybackService;
  @Autowired private BuyOrderService buyOrderService;
  @Autowired private MarketTestData market;
  @MockitoBean private QuoteSource quotes;

  private User seller;
  private User superuser;
  private Player player;

  @BeforeEach
  void arrange() {
    seller = market.registerBuyer();
    superuser = market.registerSuperuser();
    player = market.createVisiblePlayer();
    market.emitInitialTokens(player, superuser);
    market.initializeMarket(player);
    market.fund(seller, 1_000L);
    market.fund(superuser, 1_000L);
    givenQuote(1L);
    buyOrderService.buy(seller.getEmail(), new BuyOrderRequest(player.getId(), 1));
  }

  private void givenQuote(long price) {
    org.mockito.Mockito.when(quotes.currentQuote(player.getId()))
        .thenReturn(
            Optional.of(
                new CurrentQuote(
                    player.getId(), price, OffsetDateTime.now(ZoneOffset.UTC), "strategy-v1")));
  }

  @Nested
  @DisplayName("Precio neto cero")
  class ZeroNetPrice {

    @Test
    @DisplayName(
        "Rechaza el neto cero antes de liquidar y conserva los tokens para una venta válida")
    void rechazaAntesDeLiquidar() {
      // Act / Assert
      assertThatThrownBy(
              () ->
                  buybackService.sellToSuperuser(
                      seller.getEmail(), new OfficialBuybackRequest(player.getId(), 1)))
          .isInstanceOf(InvalidQuotePriceException.class);

      // Verify: una cotización mayor permite vender la misma tenencia que se rechazó.
      givenQuote(10_000L);
      OfficialBuybackReceipt receipt =
          buybackService.sellToSuperuser(
              seller.getEmail(), new OfficialBuybackRequest(player.getId(), 1));
      assertThat(receipt.quantity()).isEqualTo(1);
      assertThat(receipt.totalAmount()).isEqualTo(100L);
    }
  }
}
