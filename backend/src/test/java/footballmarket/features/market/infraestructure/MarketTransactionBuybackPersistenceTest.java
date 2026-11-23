package footballmarket.features.market.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.BuyOrderService;
import footballmarket.features.market.application.OfficialBuybackService;
import footballmarket.features.market.application.exceptions.InsufficientStockException;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.market.domain.records.OfficialBuybackReceipt;
import footballmarket.features.market.domain.records.OfficialBuybackRequest;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.domain.records.CurrentQuote;
import footballmarket.support.MarketTestData;
import footballmarket.support.TestcontainersConfiguration;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Categoría Repository de la auditoría de la venta oficial.
 *
 * <p>Comprueba que las columnas nullable de cotización de referencia y descuento se persisten solo
 * en la venta oficial y que un rechazo no deja filas financieras.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
class MarketTransactionBuybackPersistenceTest {

  private static final long PRICE = 100L;

  @Autowired private OfficialBuybackService buybackService;
  @Autowired private BuyOrderService buyOrderService;
  @Autowired private MarketTransactionRepository transactions;
  @Autowired private MarketTestData market;

  @MockitoBean private QuoteSource quotes;

  @MockitoBean
  @Qualifier("marketClock")
  private Clock marketClock;

  private User seller;
  private User superuser;
  private Player player;

  @BeforeEach
  void arrange() {
    org.mockito.Mockito.when(marketClock.getZone()).thenReturn(ZoneOffset.UTC);
    org.mockito.Mockito.when(marketClock.instant())
        .thenReturn(Instant.parse("2026-11-22T10:30:00Z"));
    seller = market.registerBuyer();
    superuser = market.registerSuperuser();
    player = market.createVisiblePlayer();
    market.emitInitialTokens(player, superuser);
    market.initializeMarket(player);
    givenQuote(PRICE);
    buyOrderService.buy(seller.getEmail(), new BuyOrderRequest(player.getId(), 10));
    market.fund(superuser, 10_000L);
  }

  private void givenQuote(long value) {
    org.mockito.Mockito.when(quotes.currentQuote(org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            Optional.of(
                new CurrentQuote(
                    player.getId(), value, OffsetDateTime.now(ZoneOffset.UTC), "strategy-v1")));
  }

  @Nested
  @DisplayName("Persistencia de la venta oficial")
  class BuybackPersistence {

    @Test
    @DisplayName("Guarda la cotización de referencia y el descuento aplicados")
    void guardaCotizacionYDescuento() {
      // Act
      OfficialBuybackReceipt receipt =
          buybackService.sellToSuperuser(
              seller.getEmail(), new OfficialBuybackRequest(player.getId(), 5));

      // Assert
      MarketTransaction persisted = transactions.findById(receipt.transactionId()).orElseThrow();
      assertThat(persisted.getQuotePrice()).isEqualTo(PRICE);
      assertThat(persisted.getDiscountPercentage()).isEqualTo((short) 10);
      assertThat(persisted.getUnitPrice()).isEqualTo(90L);
      assertThat(persisted.getTotalAmount()).isEqualTo(450L);
      assertThat(persisted.getBuyer().getId()).isEqualTo(superuser.getId());
      assertThat(persisted.getSeller().getId()).isEqualTo(seller.getId());
      assertThat(persisted.getExecutedAt()).isEqualTo(OffsetDateTime.parse("2026-11-22T10:30:00Z"));
    }
  }

  @Nested
  @DisplayName("Compatibilidad de compras existentes")
  class PurchaseCompatibility {

    @Test
    @DisplayName("Una compra confirmada persiste ambas columnas nuevas como null")
    void laCompraPersisteColumnasNulas() {
      // Act
      MarketTransaction purchase =
          buyOrderService.buy(seller.getEmail(), new BuyOrderRequest(player.getId(), 1));

      // Assert
      MarketTransaction persisted = transactions.findById(purchase.getId()).orElseThrow();
      assertThat(persisted.getQuotePrice()).isNull();
      assertThat(persisted.getDiscountPercentage()).isNull();
      assertThat(persisted.getUnitPrice()).isEqualTo(PRICE);
    }
  }

  @Nested
  @DisplayName("Atomicidad del rechazo")
  class RejectionAtomicity {

    @Test
    @DisplayName("Una venta rechazada no persiste ningún registro financiero")
    void laVentaRechazadaNoPersisteRegistro() {
      // Arrange
      long before = transactions.count();

      // Act / Assert
      assertThatThrownBy(
              () ->
                  buybackService.sellToSuperuser(
                      seller.getEmail(), new OfficialBuybackRequest(player.getId(), 11)))
          .isInstanceOf(InsufficientStockException.class);

      // Assert
      assertThat(transactions.count()).isEqualTo(before);
    }
  }
}
