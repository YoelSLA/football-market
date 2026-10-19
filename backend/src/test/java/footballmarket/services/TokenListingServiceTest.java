package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import footballmarket.integrations.QuoteSource;
import footballmarket.models.Player;
import footballmarket.models.TokenListing;
import footballmarket.models.User;
import footballmarket.models.records.BuyOrderRequest;
import footballmarket.models.records.CurrentQuote;
import footballmarket.models.records.SellOrderRequest;
import footballmarket.services.exceptions.*;
import footballmarket.support.MarketTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
          other.getEmail(), new footballmarket.models.records.BuyListingRequest(listing.getId()));
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
      // Act
      footballmarket.models.records.TokenListingView result =
          listingService.cancel(seller.getEmail(), listing.getId());
      // Assert
      assertThat(result.status().name()).isEqualTo("CANCELLED");
      assertThat(result.closedAt()).isNotNull();
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
          .extracting(footballmarket.models.records.TokenListingView::listingId)
          .contains(listing.getId());
      assertThat(listingService.market(seller.getEmail()))
          .extracting(footballmarket.models.records.TokenListingView::listingId)
          .doesNotContain(listing.getId());
      assertThat(listingService.market(viewer.getEmail()))
          .extracting(footballmarket.models.records.TokenListingView::listingId)
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
    @DisplayName("Un tercero no obtiene detalle de una publicación ya vendida")
    void ocultaPublicacionCerrada() {
      // Arrange
      TokenListing listing =
          listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 5));
      User viewer = market.registerBuyer();
      buyService.buyListing(
          viewer.getEmail(), new footballmarket.models.records.BuyListingRequest(listing.getId()));
      // Act / Assert
      assertThatThrownBy(() -> listingService.detail(viewer.getEmail(), listing.getId()))
          .isInstanceOf(ListingNotFoundException.class);
      assertThat(listingService.detail(seller.getEmail(), listing.getId()).status().name())
          .isEqualTo("SOLD");
    }
  }

  @Autowired private TokenListingService listingService;
  @Autowired private BuyOrderService buyService;
  @Autowired private MarketTestData market;
  @MockitoBean private QuoteSource quotes;
  @MockitoBean private java.time.Clock marketClock;
  private User seller;
  private User superuser;
  private Player player;

  @BeforeEach
  void arrange() {
    when(marketClock.getZone()).thenReturn(java.time.ZoneOffset.UTC);
    when(marketClock.instant()).thenReturn(java.time.Instant.parse("2026-10-18T12:00:00Z"));
    seller = market.registerBuyer();
    superuser = market.registerSuperuser();
    player = market.createVisiblePlayer();
    market.emitInitialTokens(player, superuser);
    when(quotes.currentQuote(any()))
        .thenAnswer(
            invocation -> {
              Long playerId = invocation.getArgument(0);
              return Optional.of(new CurrentQuote(playerId, 100L, OffsetDateTime.now(), "fixture"));
            });
    buyService.buy(seller.getEmail(), new BuyOrderRequest(player.getId(), 20));
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
      footballmarket.models.records.TokenListingView result =
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
}
