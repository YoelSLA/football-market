package footballmarket.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.auth.application.AuthenticationService;
import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.catalog.application.exceptions.PlayerNotVisibleException;
import footballmarket.features.market.application.BuyOrderService;
import footballmarket.features.market.application.TokenListingService;
import footballmarket.features.market.application.exceptions.*;
import footballmarket.features.market.application.exceptions.ArithmeticOverflowException;
import footballmarket.features.market.application.exceptions.InsufficientStockException;
import footballmarket.features.market.application.exceptions.InvalidListingStatusException;
import footballmarket.features.market.application.exceptions.InvalidQuantityException;
import footballmarket.features.market.application.exceptions.ListingNotAvailableException;
import footballmarket.features.market.application.exceptions.ListingNotFoundException;
import footballmarket.features.market.application.exceptions.SuperuserCannotSellException;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.presentation.ListingController;
import footballmarket.features.market.presentation.OrderController;
import footballmarket.features.quote.application.exceptions.InvalidQuotePriceException;
import footballmarket.features.quote.application.exceptions.QuoteContractViolationException;
import footballmarket.features.quote.application.exceptions.QuoteUnavailableException;
import footballmarket.features.quote.domain.records.CurrentQuote;
import footballmarket.models.TestFixtures;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Contrato HTTP de publicación y consultas con la cadena JWT productiva. */
@WebMvcTest({OrderController.class, ListingController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class ListingControllerTest {
  @Nested
  @DisplayName("Cancelación HTTP")
  class Cancellation {
    @Test
    @DisplayName("Devuelve la publicación cancelada con closedAt informado")
    void cancelaPublicacion() throws Exception {
      // Arrange
      TokenListing listing = listing();
      listing.markCancelled(NOW.plusHours(1));
      when(listingService.cancel(anyString(), any()))
          .thenReturn(listing.snapshot(NOW.plusHours(1)));
      // Act / Assert
      mvc.perform(post("/api/listings/25/cancel").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(10))
          .andExpect(jsonPath("$.listingId").value(25))
          .andExpect(jsonPath("$.sellerId").value(2))
          .andExpect(jsonPath("$.playerId").value(1))
          .andExpect(jsonPath("$.quantity").value(5))
          .andExpect(jsonPath("$.unitPrice").value(100))
          .andExpect(jsonPath("$.totalAmount").value(500))
          .andExpect(jsonPath("$.createdAt").value(sameInstant(NOW)))
          .andExpect(jsonPath("$.expiresAt").value(sameInstant(NOW.plusDays(1))))
          .andExpect(jsonPath("$.status").value("CANCELLED"))
          .andExpect(jsonPath("$.closedAt").value(sameInstant(NOW.plusHours(1))));
    }

    @ParameterizedTest
    @MethodSource("footballmarket.controllers.ListingControllerTest#cancellationErrors")
    @DisplayName("La cancelación distingue autorización, ausencia y estado no disponible")
    void rechazaCancelacion(RuntimeException failure, int httpStatus, String code)
        throws Exception {
      // Arrange
      when(listingService.cancel(anyString(), any())).thenThrow(failure);
      // Act / Assert
      mvc.perform(post("/api/listings/25/cancel").header("Authorization", authorization))
          .andExpect(status().is(httpStatus))
          .andExpect(jsonPath("$.length()").value(6))
          .andExpect(jsonPath("$.status").value(httpStatus))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.error").isString())
          .andExpect(jsonPath("$.message").isString())
          .andExpect(jsonPath("$.code").value(code))
          .andExpect(jsonPath("$.path").value("/api/listings/25/cancel"));
    }

    @Test
    @DisplayName("Cancelar requiere sesión y no invoca el Service si falta")
    void exigeSesionCancelacion() throws Exception {
      // Act / Assert
      mvc.perform(post("/api/listings/25/cancel")).andExpect(status().isUnauthorized());
      // Verify
      verifyNoInteractions(listingService);
    }
  }

  static Stream<Arguments> cancellationErrors() {
    return Stream.of(
        Arguments.of(
            new org.springframework.security.access.AccessDeniedException("Ajena"),
            403,
            "ACCESS_DENIED"),
        Arguments.of(new ListingNotFoundException(), 404, "LISTING_NOT_FOUND"),
        Arguments.of(new ListingNotAvailableException(), 409, "LISTING_NOT_AVAILABLE"));
  }

  @Nested
  @DisplayName("Consulta HTTP de publicaciones")
  class Queries {
    @Test
    @DisplayName(
        "Sin filtro devuelve las publicaciones propias sin convertir la ausencia en un estado")
    void listaPropiasSinFiltro() throws Exception {
      // Arrange
      footballmarket.features.market.domain.records.TokenListingView view = listing().snapshot(NOW);
      when(listingService.mine(anyString(), org.mockito.ArgumentMatchers.isNull()))
          .thenReturn(java.util.List.of(view));
      // Act / Assert
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                      "/api/listings/mine")
                  .header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.listings.length()").value(1))
          .andExpect(jsonPath("$.listings[0].listingId").value(25));
      // Verify
      org.mockito.Mockito.verify(listingService).mine("seller@example.com", null);
    }

    @Test
    @DisplayName("El mercado devuelve el sobre y los nueve campos de cada elemento sin closedAt")
    void listaMercado() throws Exception {
      // Arrange
      footballmarket.features.market.domain.records.TokenListingView view = listing().snapshot(NOW);
      when(listingService.market(anyString())).thenReturn(java.util.List.of(view));
      // Act / Assert
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                      "/api/listings")
                  .header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$.listings.length()").value(1))
          .andExpect(jsonPath("$.listings[0].length()").value(9))
          .andExpect(jsonPath("$.listings[0].listingId").value(25))
          .andExpect(jsonPath("$.listings[0].sellerId").value(2))
          .andExpect(jsonPath("$.listings[0].playerId").value(1))
          .andExpect(jsonPath("$.listings[0].quantity").value(5))
          .andExpect(jsonPath("$.listings[0].unitPrice").value(100))
          .andExpect(jsonPath("$.listings[0].totalAmount").value(500))
          .andExpect(jsonPath("$.listings[0].status").value("ACTIVE"))
          .andExpect(jsonPath("$.listings[0].createdAt").value(sameInstant(NOW)))
          .andExpect(jsonPath("$.listings[0].expiresAt").value(sameInstant(NOW.plusDays(1))));
    }

    @Test
    @DisplayName(
        "El detalle devuelve estado efectivo y closedAt nulo mientras no haya cierre explícito")
    void consultaDetalle() throws Exception {
      // Arrange
      when(listingService.detail(anyString(), any()))
          .thenReturn(listing().snapshot(NOW.plusDays(1)));
      // Act / Assert
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                      "/api/listings/25")
                  .header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(10))
          .andExpect(jsonPath("$.listingId").value(25))
          .andExpect(jsonPath("$.sellerId").value(2))
          .andExpect(jsonPath("$.playerId").value(1))
          .andExpect(jsonPath("$.quantity").value(5))
          .andExpect(jsonPath("$.unitPrice").value(100))
          .andExpect(jsonPath("$.totalAmount").value(500))
          .andExpect(jsonPath("$.createdAt").value(sameInstant(NOW)))
          .andExpect(jsonPath("$.expiresAt").value(sameInstant(NOW.plusDays(1))))
          .andExpect(jsonPath("$.status").value("EXPIRED"))
          .andExpect(jsonPath("$.closedAt").value(org.hamcrest.Matchers.nullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "SOLD", "CANCELLED", "EXPIRED"})
    @DisplayName("Las propias transportan al Service el filtro exacto recibido")
    void filtraPropias(String filter) throws Exception {
      // Arrange
      when(listingService.mine(anyString(), org.mockito.ArgumentMatchers.eq(filter)))
          .thenReturn(java.util.List.of());
      // Act / Assert
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                      "/api/listings/mine")
                  .param("status", filter)
                  .header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.listings.length()").value(0));
      // Verify
      org.mockito.Mockito.verify(listingService).mine("seller@example.com", filter);
    }

    @ParameterizedTest
    @ValueSource(strings = {"active", "Active", "BAD", ""})
    @DisplayName("Un estado no reconocido devuelve INVALID_PARAMETER_TYPE sin resultados")
    void rechazaFiltro(String filter) throws Exception {
      // Arrange
      when(listingService.mine(anyString(), org.mockito.ArgumentMatchers.eq(filter)))
          .thenThrow(new InvalidListingStatusException());
      // Act / Assert
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                      "/api/listings/mine")
                  .param("status", filter)
                  .header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
    }

    @Test
    @DisplayName("El detalle no accesible devuelve LISTING_NOT_FOUND sin distinguir el motivo")
    void ocultaDetalle() throws Exception {
      // Arrange
      when(listingService.detail(anyString(), any())).thenThrow(new ListingNotFoundException());
      // Act / Assert
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                      "/api/listings/25")
                  .header("Authorization", authorization))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("LISTING_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/listings", "/api/listings/mine", "/api/listings/25"})
    @DisplayName("Las tres consultas requieren una sesión válida")
    void exigeSesionConsulta(String path) throws Exception {
      // Act / Assert
      mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path))
          .andExpect(status().isUnauthorized());
      // Verify
      verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("POST listings no es una vía alternativa de creación")
    void noExponePublicacionAlternativa() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/listings")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"playerId\":1,\"quantity\":5}"))
          .andExpect(status().isMethodNotAllowed());
      // Verify
      verifyNoInteractions(listingService);
    }
  }

  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-17T12:00:00Z");

  /** Exige ISO-8601 con offset y compara el instante exacto, sin depender de la zona local. */
  private static org.hamcrest.Matcher<String> sameInstant(OffsetDateTime expected) {
    return new org.hamcrest.FeatureMatcher<String, Instant>(
        org.hamcrest.Matchers.equalTo(expected.toInstant()),
        "instante ISO-8601 con offset",
        "instante") {
      @Override
      protected Instant featureValueOf(String actual) {
        return OffsetDateTime.parse(actual).toInstant();
      }
    };
  }

  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private TokenListingService listingService;
  @MockitoBean private BuyOrderService buyService;
  @MockitoBean private AuthenticationService authenticationService;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void arrange() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("seller@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  private TokenListing listing() {
    CurrentQuote quote = new CurrentQuote(1L, 100L, NOW, "v1");
    TokenListing result =
        TokenListing.publish(
            TestFixtures.user(2L), TestFixtures.player(1L), 5, quote, NOW, NOW.plusDays(1));
    ReflectionTestUtils.setField(result, "id", 25L);
    return result;
  }

  static Stream<Arguments> publicationErrors() {
    return Stream.of(
        Arguments.of(new InvalidQuantityException(), 400, "INVALID_QUANTITY"),
        Arguments.of(new SuperuserCannotSellException(), 403, "SUPERUSER_CANNOT_SELL"),
        Arguments.of(
            new PlayerNotFoundException("No encontramos el jugador solicitado"),
            404,
            "PLAYER_NOT_FOUND"),
        Arguments.of(new PlayerNotVisibleException(), 404, "PLAYER_NOT_VISIBLE"),
        Arguments.of(new InsufficientStockException(), 409, "INSUFFICIENT_STOCK"),
        Arguments.of(new QuoteUnavailableException(), 409, "QUOTE_UNAVAILABLE"),
        Arguments.of(new InvalidQuotePriceException(), 409, "INVALID_QUOTE_PRICE"),
        Arguments.of(new QuoteContractViolationException(), 500, "INTERNAL_ERROR"),
        Arguments.of(
            new org.springframework.dao.DataIntegrityViolationException("Detalle SQL privado"),
            500,
            "INTERNAL_ERROR"),
        Arguments.of(
            new org.springframework.transaction.TransactionSystemException("Detalle privado"),
            500,
            "INTERNAL_ERROR"),
        Arguments.of(new ArithmeticOverflowException(), 500, "INTERNAL_ERROR"));
  }

  @Nested
  @DisplayName("Publicación HTTP")
  class Publication {
    @Test
    @DisplayName(
        "Devuelve exactamente la publicación creada con 201 y las unidades monetarias acordadas")
    void devuelvePublicacion() throws Exception {
      // Arrange
      when(listingService.publish(anyString(), any())).thenReturn(listing());
      // Act / Assert
      mvc.perform(
              post("/api/orders/sell")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"playerId\":1,\"quantity\":5}"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.length()").value(8))
          .andExpect(jsonPath("$.listingId").value(25))
          .andExpect(jsonPath("$.playerId").value(1))
          .andExpect(jsonPath("$.quantity").value(5))
          .andExpect(jsonPath("$.unitPrice").value(100))
          .andExpect(jsonPath("$.totalAmount").value(500))
          .andExpect(jsonPath("$.status").value("ACTIVE"))
          .andExpect(jsonPath("$.createdAt").value(sameInstant(NOW)))
          .andExpect(jsonPath("$.expiresAt").value(sameInstant(NOW.plusDays(1))));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "{}",
          "{\"playerId\":1}",
          "{\"quantity\":5}",
          "{\"playerId\":1,\"quantity\":null}",
          "{\"playerId\":1,\"quantity\":5.0}",
          "{\"playerId\":1,\"quantity\":1e1}",
          "{\"playerId\":1,\"quantity\":\"5\"}",
          "{\"playerId\":1,\"quantity\":2147483648}"
        })
    @DisplayName("Rechaza estructuras inválidas antes de invocar el caso de uso")
    void rechazaEstructura(String body) throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/sell")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      // Verify
      verifyNoInteractions(listingService);
    }

    @ParameterizedTest
    @MethodSource("footballmarket.controllers.ListingControllerTest#publicationErrors")
    @DisplayName("Cada rechazo de publicación tiene su código contractual y no revela existencias")
    void traduceRechazos(RuntimeException failure, int httpStatus, String code) throws Exception {
      // Arrange
      when(listingService.publish(anyString(), any())).thenThrow(failure);
      // Act / Assert
      mvc.perform(
              post("/api/orders/sell")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"playerId\":1,\"quantity\":5}"))
          .andExpect(status().is(httpStatus))
          .andExpect(jsonPath("$.length()").value(6))
          .andExpect(jsonPath("$.status").value(httpStatus))
          .andExpect(jsonPath("$.code").value(code))
          .andExpect(jsonPath("$.path").value("/api/orders/sell"))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.error").isString())
          .andExpect(jsonPath("$.message").isString())
          .andExpect(jsonPath("$.reserved").doesNotExist())
          .andExpect(jsonPath("$.balance").doesNotExist())
          .andExpect(jsonPath("$.stock").doesNotExist());
    }

    @Test
    @DisplayName("Sin sesión la publicación responde 401 y no llega al Service")
    void exigeSesion() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/sell")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"playerId\":1,\"quantity\":5}"))
          .andExpect(status().isUnauthorized());
      // Verify
      verifyNoInteractions(listingService);
    }
  }
}
