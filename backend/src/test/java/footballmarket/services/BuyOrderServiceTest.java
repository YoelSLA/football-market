package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.catalog.application.exceptions.PlayerNotVisibleException;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.BuyOrderService;
import footballmarket.features.market.application.TokenListingService;
import footballmarket.features.market.application.exceptions.ArithmeticOverflowException;
import footballmarket.features.market.application.exceptions.InsufficientStockException;
import footballmarket.features.market.application.exceptions.InvalidQuantityException;
import footballmarket.features.market.application.exceptions.MarketNotInitializedException;
import footballmarket.features.market.application.exceptions.SuperuserCannotBuyException;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.models.UserCredit;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.quote.application.QuoteRecalculationService;
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
 * Categoría Service de la compra de tokens.
 *
 * <p>Afirma exclusivamente lo que devuelve o lanza la API pública de {@link BuyOrderService}. No
 * accede a la persistencia: el estado durable se comprueba en {@code MarketDomainPersistenceTest}.
 * El aprovisionamiento previo se delega en {@link MarketTestData}, conforme a {@code
 * docs/backend/testing.md} §5 y §6.
 *
 * <p>El reloj de mercado queda congelado para que la regresión de FR-019 pueda afirmar el instante
 * de confirmación. Las expectativas heredadas se escribieron contra el reloj real: al ejecutar esta
 * suite hay que confirmar que ninguna depende del instante actual.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("integration")
class BuyOrderServiceTest {

  private static final int QUANTITY = 10;
  private static final long PRICE = 100L;

  @Autowired private BuyOrderService buyOrderService;
  @Autowired private TokenListingService listingService;
  @Autowired private MarketTestData market;
  @Autowired private QuoteRecalculationService recalculation;
  @MockitoBean private QuoteSource quotes;

  @MockitoBean
  @Qualifier("marketClock")
  private Clock marketClock;

  private User buyer;
  private User superuser;
  private Player player;

  @BeforeEach
  void arrange() {
    org.mockito.Mockito.when(marketClock.getZone()).thenReturn(ZoneOffset.UTC);
    org.mockito.Mockito.when(marketClock.instant())
        .thenReturn(Instant.parse("2026-10-20T12:00:00Z"));
    buyer = market.registerBuyer();
    superuser = market.registerSuperuser();
    player = market.createVisiblePlayer();
    market.emitInitialTokens(player, superuser);
    market.initializeMarket(player);
    givenQuote(Optional.of(price(PRICE)));
  }

  private void givenQuote(Optional<CurrentQuote> quote) {
    org.mockito.Mockito.when(quotes.currentQuote(org.mockito.ArgumentMatchers.any()))
        .thenReturn(quote);
  }

  private CurrentQuote price(long value) {
    return new CurrentQuote(
        player.getId(), value, OffsetDateTime.now(ZoneOffset.UTC), "strategy-v1");
  }

  /**
   * Hace que la fuente cotice el precio indicado para el jugador que se le solicite.
   *
   * <p>La cotización devuelve el identificador del jugador realmente consultado, de modo que una
   * misma compra puede abarcar más de un jugador sin que la respuesta encaje con el jugador del
   * campo. {@link #price(long)} y {@link #givenQuote(Optional)} fijan la cotización a un solo
   * jugador y por eso no alcanzan cuando la prueba compra a varios.
   *
   * @param value precio por token en centésimas
   */
  private void givenQuoteForRequestedPlayer(long value) {
    org.mockito.Mockito.when(quotes.currentQuote(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            invocation -> {
              Long requestedId = invocation.getArgument(0);
              return Optional.of(
                  new CurrentQuote(
                      requestedId, value, OffsetDateTime.now(ZoneOffset.UTC), "strategy-v1"));
            });
  }

  private MarketTransaction buy(int quantity) {
    return buyPlayer(player.getId(), quantity);
  }

  @Nested
  @DisplayName("Reloj compartido de confirmación")
  class ConfirmationClock {
    @Test
    @DisplayName("La compra directa registra exactamente el instante del reloj inyectado")
    void fechaCompraDirectaConRelojDeMercado() {
      // Arrange / Act
      MarketTransaction transaction = buy(1);
      // Assert
      assertThat(transaction.getExecutedAt())
          .isEqualTo(OffsetDateTime.parse("2026-10-20T12:00:00Z"));
    }

    @Test
    @DisplayName("La liquidación de una publicación conserva el mismo instante validado")
    void fechaLiquidacionConRelojValidado() {
      // Arrange
      buy(2);
      User receiver = market.registerBuyer();
      footballmarket.features.market.domain.models.TokenListing listing =
          listingService.publish(
              buyer.getEmail(),
              new footballmarket.features.market.domain.records.SellOrderRequest(
                  player.getId(), 1));
      // Act
      MarketTransaction transaction =
          buyOrderService.buyListing(
              receiver.getEmail(),
              new footballmarket.features.market.domain.records.BuyListingRequest(listing.getId()));
      // Assert
      assertThat(transaction.getExecutedAt()).isEqualTo(listing.getCreatedAt());
    }
  }

  @Nested
  @DisplayName("Compra de una publicación completa")
  class SecondaryPurchase {
    @Test
    @DisplayName(
        "La liquidación devuelve vendedor, publicación, precio congelado y estados financieros")
    void liquidaPublicacion() {
      // Arrange
      buy(20);
      footballmarket.features.market.domain.models.TokenListing listing =
          listingService.publish(
              buyer.getEmail(),
              new footballmarket.features.market.domain.records.SellOrderRequest(
                  player.getId(), 5));
      User consumer = market.registerBuyer();
      givenQuote(Optional.of(price(900L)));
      // Act
      MarketTransaction result =
          buyOrderService.buyListing(
              consumer.getEmail(),
              new footballmarket.features.market.domain.records.BuyListingRequest(listing.getId()));
      // Assert
      assertThat(result.getListing().getId()).isEqualTo(listing.getId());
      assertThat(result.getSeller().getId()).isEqualTo(buyer.getId());
      assertThat(result.getUnitPrice()).isEqualTo(PRICE);
      assertThat(result.getTotalAmount()).isEqualTo(500);
      assertThat(result.getBuyerCreditsBefore() - result.getBuyerCreditsAfter()).isEqualTo(500);
      assertThat(result.getSellerCreditsAfter() - result.getSellerCreditsBefore()).isEqualTo(500);
      assertThat(result.getBuyerTokensAfter() - result.getBuyerTokensBefore()).isEqualTo(5);
    }

    @Test
    @DisplayName("No permite que el vendedor compre su propia publicación")
    void rechazaCompraPropia() {
      // Arrange
      buy(20);
      footballmarket.features.market.domain.models.TokenListing listing =
          listingService.publish(
              buyer.getEmail(),
              new footballmarket.features.market.domain.records.SellOrderRequest(
                  player.getId(), 5));
      // Act / Assert
      assertThatThrownBy(
              () ->
                  buyOrderService.buyListing(
                      buyer.getEmail(),
                      new footballmarket.features.market.domain.records.BuyListingRequest(
                          listing.getId())))
          .isInstanceOf(
              footballmarket.features.market.application.exceptions.CannotBuyOwnListingException
                  .class);
    }

    @Test
    @DisplayName("La compra directa sigue sin referencia a una publicación")
    void conservaCompraDirecta() {
      // Act
      MarketTransaction result = buy(5);
      // Assert
      assertThat(result.getListing()).isNull();
      assertThat(result.getSeller().getId()).isEqualTo(superuser.getId());
    }
  }

  private MarketTransaction buyPlayer(long playerId, int quantity) {
    return buyOrderService.buy(buyer.getEmail(), new BuyOrderRequest(playerId, quantity));
  }

  @Nested
  @DisplayName("Compra confirmada")
  class SuccessfulPurchase {
    @Test
    @DisplayName("Devuelve el registro con los importes calculados en centésimas")
    void devuelveElRegistroConLosImportesCalculados() {
      // Act
      MarketTransaction transaction = buy(QUANTITY);

      // Assert
      assertThat(transaction).isNotNull();
      assertThat(transaction.getId()).isNotNull();
      assertThat(transaction.getQuantity()).isEqualTo(QUANTITY);
      assertThat(transaction.getUnitPrice()).isEqualTo(PRICE);
      assertThat(transaction.getTotalAmount()).isEqualTo(PRICE * QUANTITY);
      assertThat(transaction.getPlayer().getId()).isEqualTo(player.getId());
      assertThat(transaction.getBuyer().getId()).isEqualTo(buyer.getId());
      assertThat(transaction.getSeller().getId()).isEqualTo(superuser.getId());
      assertThat(transaction.getExecutedAt()).isNotNull();
    }

    @Test
    @DisplayName("La primera compra de un jugador no necesita tenencia previa del comprador")
    void laPrimeraCompraNoNecesitaTenenciaPrevia() {
      // Act
      MarketTransaction transaction = buy(QUANTITY);

      // Assert
      assertThat(transaction.getBuyerTokensBefore()).isZero();
      assertThat(transaction.getBuyerTokensAfter()).isEqualTo(QUANTITY);
    }

    @Test
    @DisplayName("Cada solicitud válida es una compra nueva aunque repita la anterior")
    void cadaSolicitudValidaEsUnaCompraNueva() {
      // Act
      MarketTransaction first = buy(QUANTITY);
      MarketTransaction second = buy(QUANTITY);

      // Assert
      assertThat(first.getId()).isNotEqualTo(second.getId());
      assertThat(first.getTotalAmount()).isEqualTo(second.getTotalAmount());
    }

    @Test
    @DisplayName("El importe total multiplica precio y cantidad sin truncamiento")
    void elImporteMultiplicaPrecioYCantidad() {
      // Arrange
      givenQuote(Optional.of(price(250L)));

      // Act
      MarketTransaction transaction = buy(7);

      // Assert
      assertThat(transaction.getTotalAmount()).isEqualTo(1_750L);
    }
  }

  @Nested
  @DisplayName("Autorización por rol persistido")
  class Authorization {
    @Test
    @DisplayName("Un usuario persistido como superusuario no puede comprar")
    void elSuperusuarioNoPuedeComprar() {
      // Act / Assert
      assertThatThrownBy(
              () ->
                  buyOrderService.buy(
                      superuser.getEmail(), new BuyOrderRequest(player.getId(), QUANTITY)))
          .isInstanceOf(SuperuserCannotBuyException.class);
    }

    @Test
    @DisplayName("Un usuario normal autenticado sí puede comprar")
    void unUsuarioNormalPuedeComprar() {
      // Act / Assert
      assertThat(buy(QUANTITY)).isNotNull();
    }
  }

  @Nested
  @DisplayName("Validación de cantidad")
  class QuantityValidation {
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101, Integer.MIN_VALUE, Integer.MAX_VALUE})
    @DisplayName("Una cantidad fuera del rango admitido se rechaza")
    void rechazaCantidadesFueraDeRango(int quantity) {
      // Act / Assert
      assertThatThrownBy(() -> buy(quantity)).isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    @DisplayName("Los extremos del rango admitido se aceptan")
    void aceptaLosExtremosDelRango() {
      // Arrange: cada extremo se compra a un jugador distinto, con inventario independiente. El
      // rango se valida contra la cantidad pedida, no contra el inventario disponible, así que un
      // único jugador no sirve para examinar los dos extremos: la primera compra consumiría parte
      // de
      // su stock y la segunda se rechazaría por inventario, que es el criterio de AC03 y no el que
      // esta prueba examina.
      Player other = market.createVisiblePlayer();
      market.emitInitialTokens(other, superuser);

      // El saldo inicial cubre una sola de las dos compras, de modo que se acredita lo que exige la
      // segunda. No se altera el saldo inicial de producción.
      market.fund(buyer, UserCredit.INITIAL_USER_CREDITS);
      givenQuoteForRequestedPlayer(PRICE);

      // Act
      MarketTransaction lowerBound = buy(1);
      MarketTransaction upperBound = buyPlayer(other.getId(), 100);

      // Assert: se afirma la cantidad efectiva de cada extremo, no solo que la compra exista.
      assertThat(lowerBound.getQuantity()).isEqualTo(1);
      assertThat(lowerBound.getTotalAmount()).isEqualTo(PRICE);
      assertThat(upperBound.getQuantity()).isEqualTo(100);
      assertThat(upperBound.getTotalAmount()).isEqualTo(PRICE * 100);
    }
  }

  @Nested
  @DisplayName("Elegibilidad del jugador")
  class Eligibility {
    @Test
    @DisplayName("Un identificador no positivo no corresponde a ningún jugador")
    void unIdentificadorNoPositivoNoEncuentraJugador() {
      // Act / Assert: lo no positivo es el identificador del jugador, no la cantidad, que es válida
      // en ambos casos. El contrato distingue `playerId` 0 o negativo de una cantidad inválida.
      assertThatThrownBy(() -> buyPlayer(0L, QUANTITY)).isInstanceOf(PlayerNotFoundException.class);
      assertThatThrownBy(() -> buyPlayer(-1L, QUANTITY))
          .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    @DisplayName("Un identificador inexistente se distingue del jugador no visible")
    void unIdentificadorInexistenteSeDistingue() {
      // Act / Assert
      assertThatThrownBy(() -> buyPlayer(999_999_999L, QUANTITY))
          .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    @DisplayName("Un jugador existente pero no visible produce un rechazo distinto")
    void unJugadorNoVisibleProduceOtroRechazo() {
      // Arrange
      Player notVisible = market.createNotVisiblePlayer();
      market.emitInitialTokens(notVisible, superuser);

      // Act / Assert
      assertThatThrownBy(
              () ->
                  buyOrderService.buy(
                      buyer.getEmail(), new BuyOrderRequest(notVisible.getId(), QUANTITY)))
          .isInstanceOf(PlayerNotVisibleException.class);
    }
  }

  @Nested
  @DisplayName("Validación de la cotización")
  class QuoteValidation {
    @Test
    @DisplayName("Una cotización de otro jugador es un incumplimiento del contrato de integración")
    void unaCotizacionAjenaIncumpleElContrato() {
      // Arrange
      givenQuote(
          Optional.of(
              new CurrentQuote(
                  player.getId() + 1, PRICE, OffsetDateTime.now(ZoneOffset.UTC), "strategy-v1")));

      // Act / Assert
      assertThatThrownBy(() -> buy(QUANTITY)).isInstanceOf(QuoteContractViolationException.class);
    }

    @Test
    @DisplayName("Una cotización ausente se rechaza sin inventar un precio")
    void unaCotizacionAusenteSeRechaza() {
      // Arrange
      givenQuote(Optional.empty());

      // Act / Assert
      assertThatThrownBy(() -> buy(QUANTITY)).isInstanceOf(QuoteUnavailableException.class);
    }

    @Test
    @DisplayName("Un precio no positivo se rechaza antes de cualquier cálculo")
    void unPrecioNoPositivoSeRechaza() {
      // Arrange
      givenQuote(Optional.of(price(0L)));

      // Act / Assert
      assertThatThrownBy(() -> buy(QUANTITY)).isInstanceOf(InvalidQuotePriceException.class);
    }

    @Test
    @DisplayName("La estrategia ausente de la fuente se conserva sin error de compra")
    void unaEstrategiaAusenteNoImpideLaCompra() {
      // Arrange
      givenQuote(
          Optional.of(
              new CurrentQuote(player.getId(), PRICE, OffsetDateTime.now(ZoneOffset.UTC), null)));

      // Act
      MarketTransaction transaction = buy(QUANTITY);

      // Assert
      assertThat(transaction.getQuoteStrategyVersion()).isNull();
    }
  }

  @Nested
  @DisplayName("Rechazos de negocio")
  class BusinessRejections {
    @Test
    @DisplayName("Una cantidad superior al inventario disponible se rechaza")
    void rechazaCantidadSuperiorAlInventario() {
      // Act / Assert
      assertThatThrownBy(() -> buy(101)).isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    @DisplayName("Comprar el inventario completo agota el stock y rechaza la siguiente compra")
    void rechazaUnaCompraSinInventarioSuficiente() {
      // Arrange
      buy(100);

      // AC03 exige «créditos suficientes y stock inferior a lo solicitado». La primera compra agota
      // el saldo inicial, así que se acredita lo mínimo para que el rechazo sea por inventario y no
      // por saldo, que es un criterio de aceptación distinto.
      market.fund(buyer, PRICE);

      // Act / Assert
      assertThatThrownBy(() -> buy(1)).isInstanceOf(InsufficientStockException.class);
    }
  }

  @Nested
  @DisplayName("Aritmética exacta")
  class Arithmetic {
    @Test
    @DisplayName("Un precio que desborda el rango monetario se rechaza como fallo interno")
    void unPrecioQueDesbordaSeRechaza() {
      // Arrange
      givenQuote(Optional.of(price(Long.MAX_VALUE)));

      // Act / Assert
      assertThatThrownBy(() -> buy(100)).isInstanceOf(ArithmeticOverflowException.class);
    }
  }

  @Nested
  @DisplayName("Mercado sin inicializar")
  class UninitializedMarket {
    @Test
    @DisplayName("La compra directa se rechaza antes de leer la cotización")
    void rechazaLaCompraDirecta() {
      // Arrange
      market.clearMarketInitialization();
      org.mockito.Mockito.clearInvocations(quotes);

      // Act / Assert
      assertThatThrownBy(() -> buy(QUANTITY)).isInstanceOf(MarketNotInitializedException.class);
      org.mockito.Mockito.verify(quotes, org.mockito.Mockito.never())
          .currentQuote(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("La compra de una publicación se rechaza antes de leer la publicación")
    void rechazaLaCompraDeUnaPublicacion() {
      // Arrange: publicar exige que el vendedor tenga tenencias, de modo que el comprador compra
      // antes de publicar sus tokens.
      buy(QUANTITY);
      footballmarket.features.market.domain.models.TokenListing listing =
          listingService.publish(
              buyer.getEmail(),
              new footballmarket.features.market.domain.records.SellOrderRequest(
                  player.getId(), 5));
      market.clearMarketInitialization();

      // Act / Assert
      assertThatThrownBy(
              () ->
                  buyOrderService.buyListing(
                      buyer.getEmail(),
                      new footballmarket.features.market.domain.records.BuyListingRequest(
                          listing.getId())))
          .isInstanceOf(MarketNotInitializedException.class);
    }
  }

  @Nested
  @DisplayName("Efectos del recálculo sobre el mercado")
  class RecalculationEffects {
    @Test
    @DisplayName("Un recálculo posterior no altera los saldos ni las tenencias del comprador")
    void elRecalculoNoAlteraSaldosNiTenencias() {
      // Arrange
      MarketTransaction first = buy(QUANTITY);

      // Act
      footballmarket.features.quote.domain.models.QuoteExecution execution =
          recalculation.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      MarketTransaction second = buy(1);

      // Assert
      assertThat(execution).isNotNull();
      assertThat(second.getBuyerCreditsBefore()).isEqualTo(first.getBuyerCreditsAfter());
      assertThat(second.getBuyerTokensBefore()).isEqualTo(first.getBuyerTokensAfter());
    }
  }
}
