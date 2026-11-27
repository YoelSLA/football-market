package footballmarket.features.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import footballmarket.common.config.QueryCacheConfiguration;
import footballmarket.features.auth.domain.User;
import footballmarket.features.businessmetrics.application.BusinessMetricsService;
import footballmarket.features.businessmetrics.domain.records.BusinessMetricsTotals;
import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.catalog.application.exceptions.PlayerNotVisibleException;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.exceptions.*;
import footballmarket.features.market.application.exceptions.InsufficientStockException;
import footballmarket.features.market.application.exceptions.InvalidListingStatusException;
import footballmarket.features.market.application.exceptions.InvalidQuantityException;
import footballmarket.features.market.application.exceptions.ListingNotAvailableException;
import footballmarket.features.market.application.exceptions.ListingNotFoundException;
import footballmarket.features.market.application.exceptions.MarketNotInitializedException;
import footballmarket.features.market.application.exceptions.SuperuserCannotSellException;
import footballmarket.features.market.domain.exceptions.InvalidListingPriceException;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.domain.records.BuyListingRequest;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.market.domain.records.SellOrderRequest;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.application.exceptions.InvalidQuotePriceException;
import footballmarket.features.quote.application.exceptions.QuoteContractViolationException;
import footballmarket.features.quote.application.exceptions.QuoteUnavailableException;
import footballmarket.features.quote.domain.records.CurrentQuote;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.support.MarketTestData;
import footballmarket.support.PlayerStatisticsQueryData;
import footballmarket.support.TestcontainersConfiguration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
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
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * API pública real de publicaciones; fixtures solo en Arrange según testing.md §5, enmienda 2.0.0.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("integration")
class TokenListingServiceTest {
  @Nested
  @DisplayName("Cancelación por la API pública")
  class Cancellation {
    @Test
    @DisplayName("El propietario no puede cancelar una publicación vendida")
    void rechazaCancelacionVendida() {
      // Arrange
      TokenListing listing =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 5));
      User other = market.registerBuyer();
      buyService.buyListing(
          other.getEmail(),
          new footballmarket.features.market.domain.records.BuyListingRequest(listing.getId()));
      // Act / Assert
      assertThatThrownBy(() -> listingService.cancel(seller.getEmail(), listing.getId()))
          .isInstanceOf(ListingNotAvailableException.class);
    }

    @Test
    @DisplayName("El propietario no puede cancelar una publicación en su frontera de vencimiento")
    void rechazaCancelacionVencida() {
      // Arrange
      TokenListing listing =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 5));
      when(marketClock.instant()).thenReturn(listing.getExpiresAt().toInstant());
      // Act / Assert
      assertThatThrownBy(() -> listingService.cancel(seller.getEmail(), listing.getId()))
          .isInstanceOf(ListingNotAvailableException.class);
      assertThat(listingService.detail(seller.getEmail(), listing.getId()).status().name())
          .isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("El propietario puede cancelar y no repetir la transición")
    void cancelaUnaVez() {
      // Arrange
      TokenListing listing =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 5));
      BusinessMetricsTotals before = businessMetricsService.getCurrentMetrics();
      // Act
      footballmarket.features.market.domain.records.TokenListingView result =
          listingService.cancel(seller.getEmail(), listing.getId());
      BusinessMetricsTotals after = businessMetricsService.getCurrentMetrics();
      // Assert
      assertThat(result.status().name()).isEqualTo("CANCELLED");
      assertThat(result.closedAt()).isNotNull();
      assertThat(after.cancelledListings() - before.cancelledListings()).isEqualTo(1L);
      assertThatThrownBy(() -> listingService.cancel(seller.getEmail(), listing.getId()))
          .isInstanceOf(ListingNotAvailableException.class);
    }

    @Test
    @DisplayName("Un tercero no puede cancelar una publicación ajena")
    void rechazaTercero() {
      // Arrange
      TokenListing listing =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 5));
      User other = market.registerBuyer();
      // Act / Assert
      assertThatThrownBy(() -> listingService.cancel(other.getEmail(), listing.getId()))
          .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
      assertThat(listingService.detail(seller.getEmail(), listing.getId()).status().name())
          .isEqualTo("ACTIVE");
    }
  }

  @Nested
  @DisplayName("Consultas públicas del Service")
  class Queries {
    @Test
    @DisplayName("El dueño lista su publicación y el tercero solo la encuentra en el mercado")
    void consultaPublicacion() {
      // Arrange
      TokenListing listing =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 5));
      User viewer = market.registerBuyer();
      // Act / Assert
      assertThat(listingService.mine(seller.getEmail(), "ACTIVE"))
          .extracting(footballmarket.features.market.domain.records.TokenListingView::listingId)
          .contains(listing.getId());
      assertThat(listingService.market(seller.getEmail()))
          .extracting(footballmarket.features.market.domain.records.TokenListingView::listingId)
          .doesNotContain(listing.getId());
      assertThat(listingService.market(viewer.getEmail()))
          .extracting(footballmarket.features.market.domain.records.TokenListingView::listingId)
          .contains(listing.getId());
      assertThat(listingService.detail(viewer.getEmail(), listing.getId()).totalAmount())
          .isEqualTo(500);
    }

    @ParameterizedTest
    @ValueSource(strings = {"active", "Active", "EXpired", "UNKNOWN", "", " ACTIVE"})
    @DisplayName(
        "El filtro exige las cuatro formas exactas sin tolerancia a capitalización ni espacios")
    void rechazaEstado(String status) {
      // Act / Assert
      assertThatThrownBy(() -> listingService.mine(seller.getEmail(), status))
          .isInstanceOf(InvalidListingStatusException.class);
    }

    @Test
    @DisplayName("La consulta paginada de publicaciones propias devuelve métricas, orden y página")
    void consultaPaginadaPropias() {
      // Arrange
      TokenListing listing1 =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 2));
      TokenListing listing2 =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 3));
      // Act
      footballmarket.features.market.domain.records.UserOrdersPage page =
          listingService.mine(seller.getEmail(), null, 0, 10, "NEWEST");
      // Assert
      assertThat(page.summary().activeOrders()).isGreaterThanOrEqualTo(2);
      assertThat(page.summary().activeTokens()).isGreaterThanOrEqualTo(5);
      assertThat(page.content())
          .extracting(footballmarket.features.market.domain.records.TokenListingView::listingId)
          .contains(listing2.getId(), listing1.getId());
      assertThat(page.totalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Un tercero no obtiene detalle de una publicación ya vendida")
    void ocultaPublicacionCerrada() {
      // Arrange
      TokenListing listing =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 5));
      User viewer = market.registerBuyer();
      buyService.buyListing(
          viewer.getEmail(),
          new footballmarket.features.market.domain.records.BuyListingRequest(listing.getId()));
      // Act / Assert
      assertThatThrownBy(() -> listingService.detail(viewer.getEmail(), listing.getId()))
          .isInstanceOf(ListingNotFoundException.class);
      assertThat(listingService.detail(seller.getEmail(), listing.getId()).status().name())
          .isEqualTo("SOLD");
    }
  }

  @Autowired private TokenListingService listingService;
  @Autowired private BusinessMetricsService businessMetricsService;
  @Autowired private BuyOrderService buyService;
  @Autowired private MarketTestData market;
  @Autowired private PlayerStatisticsQueryData statistics;
  @Autowired private CacheManager cacheManager;
  @MockitoBean private QuoteSource quotes;

  @MockitoBean
  @Qualifier("marketClock")
  private java.time.Clock marketClock;

  private User seller;
  private User superuser;
  private Player player;

  @Test
  @DisplayName("Rechaza publicar una venta si la última extracción no tiene métricas")
  void rejectsSaleWhenLatestMetricsAreEmpty() {
    // Arrange
    statistics.row(
        player.getId(),
        LocalDate.of(2026, 10, 18),
        MetricSource.JSON,
        "2026",
        "1",
        PlayerExtractionResult.COMPLETED,
        Map.of());
    // Act / Assert
    assertThatThrownBy(
            () ->
                listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 1)))
        .isInstanceOf(PlayerNotVisibleException.class);
  }

  @BeforeEach
  void arrange() {
    when(marketClock.getZone()).thenReturn(java.time.ZoneOffset.UTC);
    when(marketClock.instant()).thenReturn(java.time.Instant.parse("2026-10-18T12:00:00Z"));
    seller = market.registerBuyer();
    superuser = market.registerSuperuser();
    player = market.createVisiblePlayer();
    market.emitInitialTokens(player, superuser);
    market.initializeMarket(player);
    when(quotes.currentQuote(any()))
        .thenAnswer(
            invocation -> {
              Long playerId = invocation.getArgument(0);
              return Optional.of(new CurrentQuote(playerId, 100L, OffsetDateTime.now(), "fixture"));
            });
    buyService.buy(seller.getEmail(), new BuyOrderRequest(player.getId(), 20));
  }

  @Nested
  @DisplayName("Mercado sin inicializar")
  class UninitializedMarket {
    @Test
    @DisplayName("La publicación de venta se rechaza antes de leer la cotización")
    void rechazaLaPublicacion() {
      // Arrange
      market.clearMarketInitialization();
      org.mockito.Mockito.clearInvocations(quotes);

      // Act / Assert
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      seller.getEmail(), new SellOrderRequest(player.getId(), 5)))
          .isInstanceOf(MarketNotInitializedException.class);
      org.mockito.Mockito.verify(quotes, org.mockito.Mockito.never()).currentQuote(any());
    }

    @Test
    @DisplayName("El rechazo no consume la tenencia del vendedor")
    void elRechazoNoConsumeLaTenencia() {
      // Arrange
      market.clearMarketInitialization();

      // Act
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      seller.getEmail(), new SellOrderRequest(player.getId(), 5)))
          .isInstanceOf(MarketNotInitializedException.class);
      market.initializeMarket(player);
      TokenListing listing =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 20));

      // Assert
      assertThat(listing.getQuantity()).isEqualTo(20);
      assertThat(listing.getTotalAmount()).isEqualTo(2000);
    }
  }

  @Nested
  @DisplayName("Publicación de tokens propios")
  class Publication {
    @Test
    @DisplayName("Una tenencia totalmente reservada no permite otra publicación")
    void rechazaReservaTotal() {
      // Arrange
      listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 20));
      // Act / Assert
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      seller.getEmail(), new SellOrderRequest(player.getId(), 1)))
          .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("Una cotización posterior distinta no altera la publicación consultada")
    void mantienePrecioCongelado() {
      // Arrange
      TokenListing listing =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 5));
      CurrentQuote updated = new CurrentQuote(player.getId(), 900L, OffsetDateTime.now(), "v2");
      when(quotes.currentQuote(any())).thenReturn(Optional.of(updated));
      // Act
      footballmarket.features.market.domain.records.TokenListingView result =
          listingService.detail(seller.getEmail(), listing.getId());
      // Assert
      assertThat(result.unitPrice()).isEqualTo(100);
      assertThat(result.totalAmount()).isEqualTo(500);
    }

    @Test
    @DisplayName("Distingue jugador inexistente de jugador existente no visible")
    void distingueJugadorNoVisible() {
      // Arrange
      Player hidden = market.createNotVisiblePlayer();
      // Act / Assert
      assertThatThrownBy(
              () -> listingService.publish(seller.getEmail(), new SellOrderRequest(-1L, 5)))
          .isInstanceOf(PlayerNotFoundException.class);
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      seller.getEmail(), new SellOrderRequest(hidden.getId(), 5)))
          .isInstanceOf(PlayerNotVisibleException.class);
    }

    @Test
    @DisplayName("Un usuario sin tenencia no puede publicar tokens")
    void rechazaSinTenencia() {
      // Arrange
      User other = market.registerBuyer();
      // Act / Assert
      assertThatThrownBy(
              () ->
                  listingService.publish(other.getEmail(), new SellOrderRequest(player.getId(), 1)))
          .isInstanceOf(InsufficientStockException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    @DisplayName("Una cotización no positiva no se sustituye ni permite publicar")
    void rechazaPrecioInvalido(long price) {
      // Arrange
      CurrentQuote invalid = new CurrentQuote(player.getId(), price, OffsetDateTime.now(), "v1");
      when(quotes.currentQuote(any())).thenReturn(Optional.of(invalid));
      // Act / Assert
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      seller.getEmail(), new SellOrderRequest(player.getId(), 5)))
          .isInstanceOf(InvalidQuotePriceException.class);
    }

    @Test
    @DisplayName(
        "Una cotización de otro jugador se rechaza como incumplimiento del contrato externo")
    void rechazaCotizacionAjena() {
      // Arrange
      CurrentQuote invalid = new CurrentQuote(-1L, 100L, OffsetDateTime.now(), "v1");
      when(quotes.currentQuote(any())).thenReturn(Optional.of(invalid));
      // Act / Assert
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      seller.getEmail(), new SellOrderRequest(player.getId(), 5)))
          .isInstanceOf(QuoteContractViolationException.class);
    }

    @Test
    @DisplayName("Devuelve una publicación con precio congelado y vencimiento a las 24 horas")
    void publicaTokens() {
      // Act
      TokenListing result =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 5));
      // Assert
      assertThat(result.getId()).isNotNull();
      assertThat(result.getSeller().getId()).isEqualTo(seller.getId());
      assertThat(result.getTotalAmount()).isEqualTo(500);
      assertThat(result.getExpiresAt()).isEqualTo(result.getCreatedAt().plusDays(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    @DisplayName("Rechaza cantidades fuera del rango de negocio")
    void rechazaCantidad(int quantity) {
      // Act / Assert
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      seller.getEmail(), new SellOrderRequest(player.getId(), quantity)))
          .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    @DisplayName("El superusuario no puede publicar aunque posea el inventario inicial")
    void rechazaSuperusuario() {
      // Act / Assert
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      superuser.getEmail(), new SellOrderRequest(player.getId(), 5)))
          .isInstanceOf(SuperuserCannotSellException.class);
    }

    @Test
    @DisplayName("La disponibilidad descuenta publicaciones previas y rechaza el exceso")
    void descuentaReservas() {
      // Arrange
      listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 5));
      // Act / Assert
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      seller.getEmail(), new SellOrderRequest(player.getId(), 16)))
          .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("No inventa precio si la fuente de cotización no lo proporciona")
    void rechazaSinCotizacion() {
      // Arrange
      when(quotes.currentQuote(any())).thenReturn(Optional.empty());
      // Act / Assert
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      seller.getEmail(), new SellOrderRequest(player.getId(), 5)))
          .isInstanceOf(QuoteUnavailableException.class);
    }
  }

  @Nested
  @DisplayName("Precio acordado de la publicación")
  class NegotiatedPrice {
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"80, 400", "85, 425", "120, 600"})
    @DisplayName("Publica al precio solicitado y congela el importe exacto")
    void publicaPrecioDentroDeBanda(long unitPrice, long totalAmount) {
      // Act
      TokenListing listing =
          listingService.publish(
              seller.getEmail(), new SellOrderRequest(player.getId(), 5, unitPrice));

      // Assert
      assertThat(listing.getUnitPrice()).isEqualTo(unitPrice);
      assertThat(listing.getTotalAmount()).isEqualTo(totalAmount);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 79, 121})
    @DisplayName("Rechaza el precio fuera de banda sin crear ni reservar la publicación")
    void rechazaPrecioFueraDeBanda(long unitPrice) {
      // Act / Assert
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      seller.getEmail(), new SellOrderRequest(player.getId(), 5, unitPrice)))
          .isInstanceOf(InvalidListingPriceException.class);

      // Verify
      assertThat(
              listingService
                  .mine(seller.getEmail(), null, 0, 100, "NEWEST")
                  .summary()
                  .activeOrders())
          .isZero();
      TokenListing fullHolding =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 20));
      assertThat(fullHolding.getQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("La compra posterior liquida al precio acordado aunque cambie la cotización")
    void compraUsaElPrecioCongelado() {
      // Arrange
      TokenListing listing =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 5, 85L));
      User buyer = market.registerBuyer();
      when(quotes.currentQuote(any()))
          .thenReturn(
              Optional.of(
                  new CurrentQuote(player.getId(), 1000L, OffsetDateTime.now(), "updated-quote")));

      // Act
      footballmarket.features.market.domain.models.MarketTransaction transaction =
          buyService.buyListing(buyer.getEmail(), new BuyListingRequest(listing.getId()));

      // Assert
      assertThat(transaction.getUnitPrice()).isEqualTo(85);
      assertThat(transaction.getTotalAmount()).isEqualTo(425);
      assertThat(listingService.detail(seller.getEmail(), listing.getId()).unitPrice())
          .isEqualTo(85);
      assertThat(listingService.detail(seller.getEmail(), listing.getId()).totalAmount())
          .isEqualTo(425);
    }
  }

  @Nested
  @DisplayName("Invalidación de lecturas P2P")
  class CacheInvalidation {
    @Test
    @DisplayName("Publicar y cancelar ofertas invalida solo el mercado P2P")
    void invalidatesP2PReadsAfterPublicationAndCancellation() {
      // Arrange
      Object p2pKey = new Object();
      Object officialKey = new Object();
      cacheManager.getCache(QueryCacheConfiguration.P2P_MARKET).put(p2pKey, "cached");
      cacheManager.getCache(QueryCacheConfiguration.OFFICIAL_MARKET).put(officialKey, "cached");

      // Act
      TokenListing listing =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 5));

      // Assert
      assertThat(cacheManager.getCache(QueryCacheConfiguration.P2P_MARKET).get(p2pKey)).isNull();
      assertThat(cacheManager.getCache(QueryCacheConfiguration.OFFICIAL_MARKET).get(officialKey))
          .isNotNull();

      // Arrange
      Object cancellationKey = new Object();
      cacheManager.getCache(QueryCacheConfiguration.P2P_MARKET).put(cancellationKey, "cached");

      // Act
      listingService.cancel(seller.getEmail(), listing.getId());

      // Assert
      assertThat(cacheManager.getCache(QueryCacheConfiguration.P2P_MARKET).get(cancellationKey))
          .isNull();
    }

    @Test
    @DisplayName("Una publicación rechazada conserva las lecturas cacheadas")
    void preservesP2PReadsWhenPublicationFails() {
      // Arrange
      Object p2pKey = new Object();
      cacheManager.getCache(QueryCacheConfiguration.P2P_MARKET).put(p2pKey, "cached");

      // Act / Assert
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      seller.getEmail(), new SellOrderRequest(player.getId(), 0)))
          .isInstanceOf(InvalidQuantityException.class);
      assertThat(cacheManager.getCache(QueryCacheConfiguration.P2P_MARKET).get(p2pKey)).isNotNull();
    }
  }
}
