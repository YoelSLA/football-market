package footballmarket.features.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.catalog.application.exceptions.PlayerNotVisibleException;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.exceptions.InsufficientCreditsException;
import footballmarket.features.market.application.exceptions.InsufficientStockException;
import footballmarket.features.market.application.exceptions.InvalidQuantityException;
import footballmarket.features.market.application.exceptions.MarketNotInitializedException;
import footballmarket.features.market.application.exceptions.SuperuserCannotSellException;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.market.domain.records.OfficialBuybackReceipt;
import footballmarket.features.market.domain.records.OfficialBuybackRequest;
import footballmarket.features.market.domain.records.SellOrderRequest;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.application.exceptions.InvalidQuotePriceException;
import footballmarket.features.quote.application.exceptions.QuoteContractViolationException;
import footballmarket.features.quote.application.exceptions.QuoteUnavailableException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Categoría Service de la venta oficial inmediata.
 *
 * <p>Afirma exclusivamente lo que devuelve o lanza la API pública de {@link
 * OfficialBuybackService}. El estado durable se comprueba en {@code
 * MarketTransactionBuybackPersistenceTest}. El aprovisionamiento previo se delega en {@link
 * MarketTestData}, conforme a {@code docs/backend/testing.md} §5 y §6.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
class OfficialBuybackServiceTest {

  private static final long PRICE = 100L;
  private static final int DISCOUNT = 10;

  @Autowired private OfficialBuybackService buybackService;
  @Autowired private BuyOrderService buyOrderService;
  @Autowired private TokenListingService listingService;
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
    givenQuote(Optional.of(price(player.getId(), PRICE)));
    acquireTokens(player, 20);
    market.fund(seller, 10_000L);

    // El superusuario financia la operación con el importe recibido de la compra previa.
    market.fund(superuser, 10_000L);
  }

  private void acquireTokens(Player ofPlayer, int quantity) {
    buyOrderService.buy(seller.getEmail(), new BuyOrderRequest(ofPlayer.getId(), quantity));
  }

  private void givenQuote(Optional<CurrentQuote> quote) {
    org.mockito.Mockito.when(quotes.currentQuote(org.mockito.ArgumentMatchers.any()))
        .thenReturn(quote);
  }

  private CurrentQuote price(long playerId, long value) {
    return new CurrentQuote(playerId, value, OffsetDateTime.now(ZoneOffset.UTC), "strategy-v1");
  }

  private OfficialBuybackReceipt sell(long playerId, int quantity) {
    return buybackService.sellToSuperuser(
        seller.getEmail(), new OfficialBuybackRequest(playerId, quantity));
  }

  @Nested
  @DisplayName("Venta confirmada")
  class SuccessfulSale {

    @Test
    @DisplayName("Acredita el importe neto y devuelve el comprobante con cotización y descuento")
    void devuelveElComprobanteConElImporteNeto() {
      // Act
      OfficialBuybackReceipt receipt = sell(player.getId(), 5);

      // Assert
      assertThat(receipt.transactionId()).isNotNull();
      assertThat(receipt.buyerId()).isEqualTo(superuser.getId());
      assertThat(receipt.sellerId()).isEqualTo(seller.getId());
      assertThat(receipt.playerId()).isEqualTo(player.getId());
      assertThat(receipt.quantity()).isEqualTo(5);
      assertThat(receipt.quotePrice()).isEqualTo(PRICE);
      assertThat(receipt.discountPercentage()).isEqualTo(DISCOUNT);
      assertThat(receipt.unitPrice()).isEqualTo(90L);
      assertThat(receipt.totalAmount()).isEqualTo(450L);
      assertThat(receipt.executedAt()).isEqualTo(OffsetDateTime.parse("2026-11-22T10:30:00Z"));
    }

    @Test
    @DisplayName("El redondeo del precio neto es half-up sobre la cotización con descuento")
    void redondeaElPrecioNetoHaciaArriba() {
      // Arrange
      givenQuote(Optional.of(price(player.getId(), 105L)));

      // Act
      OfficialBuybackReceipt receipt = sell(player.getId(), 1);

      // Assert: 105 × 90 / 100 = 94.5, redondeado half-up a 95.
      assertThat(receipt.unitPrice()).isEqualTo(95L);
      assertThat(receipt.totalAmount()).isEqualTo(95L);
    }

    @Test
    @DisplayName("El importe total multiplica la cantidad por el precio neto redondeado")
    void multiplicaCantidadPorPrecioNeto() {
      // Act
      OfficialBuybackReceipt receipt = sell(player.getId(), 7);

      // Assert
      assertThat(receipt.unitPrice()).isEqualTo(90L);
      assertThat(receipt.totalAmount()).isEqualTo(630L);
    }
  }

  @Nested
  @DisplayName("Límites de la operación")
  class OperationLimits {

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101, Integer.MIN_VALUE, Integer.MAX_VALUE})
    @DisplayName("Rechaza una cantidad fuera del rango 1–100")
    void rechazaCantidadFueraDeRango(int quantity) {
      // Act / Assert
      assertThatThrownBy(() -> sell(player.getId(), quantity))
          .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    @DisplayName("Acepta los extremos del rango cuando hay tenencia y saldo suficientes")
    void aceptaLosExtremosDelRango() {
      // Arrange
      Player other = market.createVisiblePlayer();
      market.emitInitialTokens(other, superuser);
      market.initializeMarket(other);
      givenQuote(Optional.of(price(other.getId(), PRICE)));
      acquireTokens(other, 100);

      // Act
      givenQuote(Optional.of(price(player.getId(), PRICE)));
      OfficialBuybackReceipt lowerBound = sell(player.getId(), 1);
      givenQuote(Optional.of(price(other.getId(), PRICE)));
      OfficialBuybackReceipt upperBound = sell(other.getId(), 100);

      // Assert
      assertThat(lowerBound.quantity()).isEqualTo(1);
      assertThat(lowerBound.totalAmount()).isEqualTo(90L);
      assertThat(upperBound.quantity()).isEqualTo(100);
      assertThat(upperBound.totalAmount()).isEqualTo(9_000L);
    }

    @Test
    @DisplayName("El superusuario no puede vender sus propios tokens")
    void elSuperusuarioNoPuedeVender() {
      // Act / Assert
      assertThatThrownBy(
              () ->
                  buybackService.sellToSuperuser(
                      superuser.getEmail(), new OfficialBuybackRequest(player.getId(), 1)))
          .isInstanceOf(SuperuserCannotSellException.class);
    }
  }

  @Nested
  @DisplayName("Tenencia libre")
  class FreeStock {

    @Test
    @DisplayName("Los tokens reservados en publicaciones P2P quedan fuera de la venta oficial")
    void excluyeLosTokensReservados() {
      // Arrange
      listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 8));

      // Act / Assert: solo 12 de los 20 tokens están libres.
      assertThatThrownBy(() -> sell(player.getId(), 13))
          .isInstanceOf(InsufficientStockException.class);

      // Act
      OfficialBuybackReceipt receipt = sell(player.getId(), 12);

      // Assert
      assertThat(receipt.totalAmount()).isEqualTo(12 * 90L);
    }

    @Test
    @DisplayName("Rechaza vender más tokens de los que el vendedor posee")
    void rechazaVenderMasTokensDeLosPoseidos() {
      // Act / Assert
      assertThatThrownBy(() -> sell(player.getId(), 21))
          .isInstanceOf(InsufficientStockException.class);
    }
  }

  @Nested
  @DisplayName("Saldo del superusuario")
  class SuperuserBalance {

    @Test
    @DisplayName("Rechaza la venta cuando el superusuario no puede pagar el importe neto")
    void rechazaLaVentaSinSaldoDelSuperusuario() {
      // Arrange
      Player other = market.createVisiblePlayer();
      market.emitInitialTokens(other, superuser);
      market.initializeMarket(other);
      givenQuote(Optional.of(price(other.getId(), PRICE)));
      acquireTokens(other, 100);
      givenQuote(Optional.of(price(other.getId(), 1_000_000_000L)));

      // Act / Assert
      assertThatThrownBy(() -> sell(other.getId(), 100))
          .isInstanceOf(InsufficientCreditsException.class);
    }

    @Test
    @DisplayName("Un rechazo no deja cambios parciales: la venta posterior conserva la tenencia")
    void unRechazoNoDejaCambiosParciales() {
      // Arrange
      Player other = market.createVisiblePlayer();
      market.emitInitialTokens(other, superuser);
      market.initializeMarket(other);
      givenQuote(Optional.of(price(other.getId(), PRICE)));
      acquireTokens(other, 100);

      // Act: el importe desborda el saldo disponible y se rechaza.
      givenQuote(Optional.of(price(other.getId(), 1_000_000_000L)));
      assertThatThrownBy(() -> sell(other.getId(), 100))
          .isInstanceOf(InsufficientCreditsException.class);

      // Act: con la cotización normal la tenencia completa sigue disponible.
      givenQuote(Optional.of(price(other.getId(), PRICE)));
      OfficialBuybackReceipt receipt = sell(other.getId(), 100);

      // Assert
      assertThat(receipt.quantity()).isEqualTo(100);
      assertThat(receipt.totalAmount()).isEqualTo(9_000L);
    }
  }

  @Nested
  @DisplayName("Elegibilidad del jugador")
  class PlayerEligibility {

    @Test
    @DisplayName("Rechaza un jugador inexistente sin inventar una cotización")
    void rechazaJugadorInexistente() {
      // Act / Assert
      assertThatThrownBy(() -> sell(999_999_999L, 1)).isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    @DisplayName("Rechaza un jugador existente no visible")
    void rechazaJugadorNoVisible() {
      // Arrange
      Player notVisible = market.createNotVisiblePlayer();

      // Act / Assert
      assertThatThrownBy(() -> sell(notVisible.getId(), 1))
          .isInstanceOf(PlayerNotVisibleException.class);
    }
  }

  @Nested
  @DisplayName("Validación de la cotización")
  class QuoteValidation {

    @Test
    @DisplayName("Rechaza una cotización ausente")
    void rechazaCotizacionAusente() {
      // Arrange
      givenQuote(Optional.empty());

      // Act / Assert
      assertThatThrownBy(() -> sell(player.getId(), 1))
          .isInstanceOf(QuoteUnavailableException.class);
    }

    @Test
    @DisplayName("Rechaza una cotización no positiva")
    void rechazaCotizacionNoPositiva() {
      // Arrange
      givenQuote(Optional.of(price(player.getId(), 0L)));

      // Act / Assert
      assertThatThrownBy(() -> sell(player.getId(), 1))
          .isInstanceOf(InvalidQuotePriceException.class);
    }

    @Test
    @DisplayName("Rechaza una cotización que no corresponde al jugador solicitado")
    void rechazaCotizacionAjena() {
      // Arrange
      givenQuote(Optional.of(price(player.getId() + 1, PRICE)));

      // Act / Assert
      assertThatThrownBy(() -> sell(player.getId(), 1))
          .isInstanceOf(QuoteContractViolationException.class);
    }
  }

  @Nested
  @DisplayName("Mercado sin inicializar")
  class UninitializedMarket {

    @Test
    @DisplayName("Rechaza la venta antes de leer la cotización")
    void rechazaLaVenta() {
      // Arrange
      market.clearMarketInitialization();
      org.mockito.Mockito.clearInvocations(quotes);

      // Act / Assert
      assertThatThrownBy(() -> sell(player.getId(), 1))
          .isInstanceOf(MarketNotInitializedException.class);
      org.mockito.Mockito.verify(quotes, org.mockito.Mockito.never())
          .currentQuote(org.mockito.ArgumentMatchers.any());
    }
  }
}
