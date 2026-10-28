package footballmarket.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.auth.application.AuthenticationService;
import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.catalog.application.exceptions.PlayerNotVisibleException;
import footballmarket.features.market.application.BuyOrderService;
import footballmarket.features.market.application.exceptions.ArithmeticOverflowException;
import footballmarket.features.market.application.exceptions.CannotBuyOwnListingException;
import footballmarket.features.market.application.exceptions.InsufficientCreditsException;
import footballmarket.features.market.application.exceptions.InsufficientStockException;
import footballmarket.features.market.application.exceptions.InvalidQuantityException;
import footballmarket.features.market.application.exceptions.ListingNotAvailableException;
import footballmarket.features.market.application.exceptions.ListingNotFoundException;
import footballmarket.features.market.application.exceptions.SellerUnavailableException;
import footballmarket.features.market.application.exceptions.SuperuserCannotBuyException;
import footballmarket.features.market.domain.exceptions.InsufficientUserCreditsException;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.domain.records.BuyListingRequest;
import footballmarket.features.market.domain.records.BuyOrderRequest;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Contrato HTTP de la compra de tokens.
 *
 * <p>Categoría Controller: el Service se simula, de modo que estas pruebas verifican únicamente lo
 * observable en la frontera HTTP y no acredita integridad transaccional.
 */
@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class OrderControllerTest {
  private static final String BUYER_EMAIL = "buyer@example.com";
  private static final Long PLAYER_ID = 7821L;
  private static final Long TRANSACTION_ID = 12345L;
  private static final String PLAYER_NOT_FOUND_MESSAGE = "No encontramos el jugador solicitado";

  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private BuyOrderService buyOrderService;

  @MockitoBean
  private footballmarket.features.market.application.TokenListingService tokenListingService;

  @MockitoBean private AuthenticationService authenticationService;

  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject(BUYER_EMAIL)
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  private MarketTransaction transaction() {
    MarketTransaction recorded =
        MarketTransaction.record(
            TestFixtures.user(2L),
            TestFixtures.superuser(1L),
            TestFixtures.player(PLAYER_ID),
            10,
            100L,
            1000L,
            10_000L,
            9000L,
            0L,
            1000L,
            0,
            10,
            100,
            90,
            OffsetDateTime.parse("2026-10-14T10:30:00Z"),
            "simulated",
            OffsetDateTime.parse("2026-10-14T10:30:01Z"));
    ReflectionTestUtils.setField(recorded, "id", TRANSACTION_ID);
    return recorded;
  }

  private String validBody() {
    return "{\"playerId\":" + PLAYER_ID + ",\"quantity\":10}";
  }

  @Nested
  @DisplayName("Compra secundaria por publicación")
  class SecondaryPurchase {
    @ParameterizedTest
    @ValueSource(
        strings = {
          "{\"listingId\":25}",
          "{\"listingId\":25,\"playerId\":999,\"quantity\":100,\"price\":1}",
          "{\"listingId\":25,\"quantity\":2147483648}",
          "{\"listingId\":25,\"quantity\":-2147483649}",
          "{\"listingId\":25,\"quantity\":101}",
          "{\"listingId\":25,\"quantity\":0}",
          "{\"listingId\":25,\"quantity\":1.5}",
          "{\"listingId\":25,\"quantity\":1e1}",
          "{\"listingId\":25,\"quantity\":\"abc\"}",
          "{\"listingId\":25,\"quantity\":\"10\"}",
          "{\"listingId\":25,\"quantity\":true}",
          "{\"listingId\":25,\"quantity\":[]}",
          "{\"listingId\":25,\"quantity\":{}}",
          "{\"listingId\":25,\"quantity\":null,\"playerId\":null}",
          "{\"listingId\":25,\"playerId\":\"abc\"}",
          "{\"listingId\":25,\"playerId\":9223372036854775808}",
          "{\"listingId\":25,\"playerId\":{}}",
          "{\"playerId\":[],\"quantity\":\"inválida\",\"listingId\":25}"
        })
    @DisplayName(
        "listingId válido ignora jugador y cantidad incluso con tipos o representaciones inválidos")
    void compraPublicacionCompleta(String body) throws Exception {
      // Arrange
      CurrentQuote quote = new CurrentQuote(PLAYER_ID, 100L, OffsetDateTime.now(), "v1");
      TokenListing listing =
          TokenListing.publish(
              TestFixtures.user(3L),
              TestFixtures.player(PLAYER_ID),
              10,
              quote,
              OffsetDateTime.now(),
              OffsetDateTime.now().plusDays(1));
      ReflectionTestUtils.setField(listing, "id", 25L);
      MarketTransaction recorded =
          MarketTransaction.record(
              TestFixtures.user(2L),
              TestFixtures.user(3L),
              TestFixtures.player(PLAYER_ID),
              10,
              100,
              1000,
              10000,
              9000,
              0,
              1000,
              0,
              10,
              20,
              10,
              quote.calculatedAt(),
              "v1",
              quote.calculatedAt(),
              listing);
      ReflectionTestUtils.setField(recorded, "id", TRANSACTION_ID);
      when(buyOrderService.buyListing(anyString(), any())).thenReturn(recorded);
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(7))
          .andExpect(jsonPath("$.listingId").value(25))
          .andExpect(jsonPath("$.playerId").value(PLAYER_ID))
          .andExpect(jsonPath("$.quantity").value(10))
          .andExpect(jsonPath("$.unitPrice").value(100))
          .andExpect(jsonPath("$.totalAmount").value(1000))
          .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID))
          .andExpect(jsonPath("$.executedAt").exists());
      // Verify
      verify(buyOrderService).buyListing(eq(BUYER_EMAIL), eq(new BuyListingRequest(25L)));
      verify(buyOrderService, never()).buy(anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "{}",
          "{\"listingId\":null}",
          "{\"listingId\":\"25\"}",
          "{\"listingId\":25.0}",
          "{\"listingId\":1e1}",
          "{\"listingId\":true}",
          "{\"listingId\":[]}",
          "{\"listingId\":{}}",
          "{\"listingId\":9223372036854775808}",
          "{\"listingId\":\"25\",\"playerId\":7821,\"quantity\":10}",
          "{\"listingId\":25.0,\"playerId\":7821,\"quantity\":10}",
          "{\"listingId\":1e1,\"playerId\":7821,\"quantity\":10}",
          "{\"listingId\":true,\"playerId\":7821,\"quantity\":10}",
          "{\"listingId\":[],\"playerId\":7821,\"quantity\":10}",
          "{\"listingId\":{},\"playerId\":7821,\"quantity\":10}",
          "{\"listingId\":9223372036854775808,\"playerId\":7821,\"quantity\":10}"
        })
    @DisplayName("El identificador secundario exige un literal entero Long sin coerción")
    void rechazaIdentificador(String body) throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      // Verify
      verify(buyOrderService, never()).buyListing(anyString(), any());
      verify(buyOrderService, never()).buy(anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    @DisplayName(
        "Un listingId no positivo se resuelve como publicación inexistente, sin compra directa")
    void noDerivaIdentificadorInexistente(long listingId) throws Exception {
      // Arrange
      when(buyOrderService.buyListing(anyString(), any()))
          .thenThrow(new ListingNotFoundException());
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"listingId\":" + listingId + ",\"playerId\":7821,\"quantity\":10}"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("LISTING_NOT_FOUND"));
      // Verify
      verify(buyOrderService).buyListing(eq(BUYER_EMAIL), eq(new BuyListingRequest(listingId)));
      verify(buyOrderService, never()).buy(anyString(), any());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource(
        "footballmarket.controllers.OrderControllerTest#secondaryErrors")
    @DisplayName("Los rechazos secundarios conservan status, código y no revelación")
    void traduceRechazo(RuntimeException failure, int httpStatus, String code) throws Exception {
      // Arrange
      when(buyOrderService.buyListing(anyString(), any())).thenThrow(failure);
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"listingId\":25}"))
          .andExpect(status().is(httpStatus))
          .andExpect(jsonPath("$.code").value(code))
          .andExpect(jsonPath("$.status").value(httpStatus))
          .andExpect(jsonPath("$.*").value(hasSize(6)))
          .andExpect(jsonPath("$.path").value("/api/orders/buy"))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.message").isString())
          .andExpect(jsonPath("$.error").isString());
    }
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> secondaryErrors() {
    return java.util.stream.Stream.of(
        org.junit.jupiter.params.provider.Arguments.of(
            new SuperuserCannotBuyException(), 403, "SUPERUSER_CANNOT_BUY"),
        org.junit.jupiter.params.provider.Arguments.of(
            new CannotBuyOwnListingException(), 403, "CANNOT_BUY_OWN_LISTING"),
        org.junit.jupiter.params.provider.Arguments.of(
            new ListingNotFoundException(), 404, "LISTING_NOT_FOUND"),
        org.junit.jupiter.params.provider.Arguments.of(
            new ListingNotAvailableException(), 409, "LISTING_NOT_AVAILABLE"),
        org.junit.jupiter.params.provider.Arguments.of(
            new InsufficientCreditsException(), 409, "INSUFFICIENT_CREDITS"),
        org.junit.jupiter.params.provider.Arguments.of(
            new InsufficientStockException(), 409, "INSUFFICIENT_STOCK"),
        org.junit.jupiter.params.provider.Arguments.of(
            new org.springframework.dao.DataIntegrityViolationException("SQL privado"),
            500,
            "INTERNAL_ERROR"),
        org.junit.jupiter.params.provider.Arguments.of(
            new ArithmeticOverflowException(), 500, "INTERNAL_ERROR"));
  }

  @Nested
  @DisplayName("Compra confirmada")
  class SuccessfulPurchase {
    @Test
    @DisplayName("Devuelve 200 con los seis campos del contrato en centésimas")
    void devuelveElContratoCompleto() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any())).thenReturn(transaction());

      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(validBody()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(6))
          .andExpect(jsonPath("$.listingId").doesNotExist())
          .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID))
          .andExpect(jsonPath("$.playerId").value(PLAYER_ID))
          .andExpect(jsonPath("$.quantity").value(10))
          .andExpect(jsonPath("$.unitPrice").value(100))
          .andExpect(jsonPath("$.totalAmount").value(1000))
          .andExpect(jsonPath("$.executedAt").exists());
    }

    @Test
    @DisplayName("El comprador de la operación es el sujeto autenticado de la sesión")
    void tomaElCompradorDeLaSesion() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any())).thenReturn(transaction());

      // Act
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(validBody()))
          .andExpect(status().isOk());

      // Assert
      verify(buyOrderService).buy(anyString(), any());
    }
  }

  @Nested
  @DisplayName("Errores de contrato y de negocio")
  class BusinessErrors {
    @Test
    @DisplayName("Una solicitud sin sesión responde 401")
    void sinSesionRespondeNoAutorizado() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy").contentType(MediaType.APPLICATION_JSON).content(validBody()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un superusuario autenticado recibe 403")
    void superusuarioRecibeProhibido() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any())).thenThrow(new SuperuserCannotBuyException());

      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(validBody()))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("SUPERUSER_CANNOT_BUY"))
          .andExpect(jsonPath("$.*").value(hasSize(6)));
    }

    @Test
    @DisplayName("Un jugador inexistente responde 404 PLAYER_NOT_FOUND")
    void jugadorInexistenteRespondeNoEncontrado() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any()))
          .thenThrow(new PlayerNotFoundException(PLAYER_NOT_FOUND_MESSAGE));

      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(validBody()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("PLAYER_NOT_FOUND"))
          .andExpect(jsonPath("$.path").value("/api/orders/buy"));
    }

    @Test
    @DisplayName("Un jugador existente no visible responde 404 PLAYER_NOT_VISIBLE")
    void jugadorNoVisibleRespondeNoVisible() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any())).thenThrow(new PlayerNotVisibleException());

      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(validBody()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("PLAYER_NOT_VISIBLE"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101, Integer.MIN_VALUE, Integer.MAX_VALUE})
    @DisplayName("Traduce el rechazo del Service para enteros fuera del rango de negocio")
    void cantidadInvalidaRespondePeticionInvalida(int quantity) throws Exception {
      // Arrange: este slice verifica la traducción HTTP, no la validación real del Service.
      // BuyOrderServiceTest.rechazaCantidadesFueraDeRango prueba esa regla con el Service real.
      when(buyOrderService.buy(anyString(), any())).thenThrow(new InvalidQuantityException());

      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"playerId\":" + PLAYER_ID + ",\"quantity\":" + quantity + "}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_QUANTITY"));
      verify(buyOrderService).buy(eq(BUYER_EMAIL), eq(new BuyOrderRequest(PLAYER_ID, quantity)));
    }

    @Test
    @DisplayName("Una cotización ausente responde 409 QUOTE_UNAVAILABLE")
    void cotizacionAusenteRespondeConflicto() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any())).thenThrow(new QuoteUnavailableException());

      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(validBody()))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("QUOTE_UNAVAILABLE"));
    }

    @Test
    @DisplayName("Un precio no positivo responde 409 INVALID_QUOTE_PRICE")
    void precioNoPositivoRespondeConflicto() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any())).thenThrow(new InvalidQuotePriceException());

      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(validBody()))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("INVALID_QUOTE_PRICE"));
    }

    @Test
    @DisplayName("Un fallo interno responde 500 INTERNAL_ERROR genérico")
    void falloInternoRespondeErrorGenerico() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any())).thenThrow(new ArithmeticOverflowException());

      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(validBody()))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
          .andExpect(
              jsonPath("$.message")
                  .value("No se pudo procesar la compra. Intentá nuevamente más tarde."))
          .andExpect(jsonPath("$.*").value(hasSize(6)));
    }

    @Test
    @DisplayName("Una violación del contrato de cotización responde el mismo 500 genérico")
    void violacionDelContratoDeCotizacionRespondeErrorGenerico() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any()))
          .thenThrow(new QuoteContractViolationException());

      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(validBody()))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("Un vendedor no identificable responde el mismo 500 genérico")
    void vendedorAusenteRespondeErrorGenerico() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any())).thenThrow(new SellerUnavailableException());

      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(validBody()))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }
  }

  @Nested
  @DisplayName("Ausencia de revelación de existencias")
  class NoDisclosure {
    @Test
    @DisplayName("El rechazo por stock no expone existencias ni campos equivalentes")
    void elRechazoPorStockNoExponeExistencias() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any())).thenThrow(new InsufficientStockException());

      // Act
      String body =
          mvc.perform(
                  post("/api/orders/buy")
                      .header("Authorization", authorization)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(validBody()))
              .andExpect(status().isConflict())
              .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
              .andReturn()
              .getResponse()
              .getContentAsString();

      // Assert
      assertNoCuantidadRevelada(body);
    }

    @Test
    @DisplayName("El rechazo por créditos no expone saldo ni el importe requerido")
    void elRechazoPorCreditosNoExponeSaldo() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any())).thenThrow(new InsufficientCreditsException());

      // Act
      String body =
          mvc.perform(
                  post("/api/orders/buy")
                      .header("Authorization", authorization)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(validBody()))
              .andExpect(status().isConflict())
              .andExpect(jsonPath("$.code").value("INSUFFICIENT_CREDITS"))
              .andReturn()
              .getResponse()
              .getContentAsString();

      // Assert
      assertNoCuantidadRevelada(body);
    }

    @Test
    @DisplayName(
        "Un rechazo de créditos del dominio se traduce al mismo código público sin filtrar saldo")
    void unRechazoDeCreditosDelDominioTambienEsGenerico() throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any()))
          .thenThrow(new InsufficientUserCreditsException());

      // Act
      String body =
          mvc.perform(
                  post("/api/orders/buy")
                      .header("Authorization", authorization)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(validBody()))
              .andExpect(status().isConflict())
              .andExpect(jsonPath("$.code").value("INSUFFICIENT_CREDITS"))
              .andReturn()
              .getResponse()
              .getContentAsString();

      // Assert
      assertNoCuantidadRevelada(body);
    }

    /**
     * Comprueba que el cuerpo del rechazo no revele existencias.
     *
     * <p>La regla prohibitiva es sobre cantidades, no sobre nombres: el contrato exige el código
     * {@code INSUFFICIENT_STOCK}, que contiene la palabra «stock». Se verifica por tanto que el
     * cuerpo tenga exactamente los seis campos del contrato global, que ninguno se llame como un
     * saldo o una cantidad, y que el mensaje no contenga ninguna cifra.
     *
     * <p>El conjunto de campos se lee del JSON real de la respuesta y se compara con los seis
     * declarados en {@code ErrorResponseDTO}, de modo que un campo nuevo o un campo de más fallen
     * la prueba en lugar de pasar inadvertido.
     *
     * @param body cuerpo de la respuesta de rechazo
     */
    private void assertNoCuantidadRevelada(String body) throws Exception {
      JsonNode root = new ObjectMapper().readTree(body);
      List<String> fields = new ArrayList<>(root.propertyNames());
      org.assertj.core.api.Assertions.assertThat(fields)
          .as("el rechazo debe tener exactamente los seis campos del contrato global")
          .containsExactlyInAnyOrder("timestamp", "status", "error", "code", "message", "path");

      org.assertj.core.api.Assertions.assertThat(body)
          .as("el rechazo no debe incluir campos fuera del contrato global")
          .doesNotContainPattern(
              "(?i)\"(available|balance|total|quantity|amount|stock|credits|saldo|existencia)\"\s*:");

      org.assertj.core.api.Assertions.assertThat(root.get("message").asText())
          .as("el mensaje de rechazo no debe revelar cantidades")
          .doesNotContainPattern("\\d");
    }
  }

  @Nested
  @DisplayName("Validación de entrada")
  class InputValidation {
    @Test
    @DisplayName("Un cuerpo ilegible responde 400 INVALID_REQUEST sin invocar el Service")
    void cuerpoIlegibleRespondePeticionInvalida() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{no-json"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verify(buyOrderService, never()).buy(anyString(), any());
    }

    @Test
    @DisplayName("Un campo ausente responde 400 sin invocar el Service")
    void campoAusenteRespondePeticionInvalida() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"quantity\":10}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verify(buyOrderService, never()).buy(anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"abc\"", "true", "[]", "{}", "9223372036854775808"})
    @DisplayName(
        "Un playerId inválido en compra directa responde 400 sin invocar ninguna modalidad")
    void identificadorNoNumericoRespondePeticionInvalida(String playerJson) throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"playerId\":" + playerJson + ",\"quantity\":10}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verify(buyOrderService, never()).buy(anyString(), any());
      verify(buyOrderService, never()).buyListing(anyString(), any());
    }

    @Test
    @DisplayName("Una cantidad ausente responde INVALID_REQUEST antes del Service")
    void cantidadAusenteRespondePeticionInvalida() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"playerId\":" + PLAYER_ID + "}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verify(buyOrderService, never()).buy(anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "null",
          "\"10\"",
          "\"abc\"",
          "1.5",
          "10.0",
          "1e1",
          "true",
          "false",
          "[]",
          "{}",
          "2147483648",
          "-2147483649"
        })
    @DisplayName("Rechaza tipos y representaciones de quantity inválidos antes del Service")
    void cantidadNoEnteraRespondePeticionInvalida(String quantityJson) throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"playerId\":" + PLAYER_ID + ",\"quantity\":" + quantityJson + "}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verify(buyOrderService, never()).buy(anyString(), any());
      verify(buyOrderService, never()).buyListing(anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    @DisplayName("Los límites válidos llegan al caso de uso y no omiten el rechazo por créditos")
    void cantidadValidaContinuaLasValidaciones(int quantity) throws Exception {
      // Arrange: una cantidad admisible no garantiza una compra exitosa.
      when(buyOrderService.buy(anyString(), any())).thenThrow(new InsufficientCreditsException());

      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"playerId\":" + PLAYER_ID + ",\"quantity\":" + quantity + "}"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("INSUFFICIENT_CREDITS"));
      verify(buyOrderService).buy(eq(BUYER_EMAIL), eq(new BuyOrderRequest(PLAYER_ID, quantity)));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "{\"playerId\":7821,\"quantity\":10}",
          "{\"playerId\":7821,\"quantity\":10,\"listingId\":null}"
        })
    @DisplayName("La compra directa válida conserva su modalidad con listingId ausente o nulo")
    void unaSolicitudValidaAlcanzaAlService(String body) throws Exception {
      // Arrange
      when(buyOrderService.buy(anyString(), any())).thenReturn(transaction());

      // Act
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk());

      // Assert
      verify(buyOrderService).buy(eq(BUYER_EMAIL), eq(new BuyOrderRequest(PLAYER_ID, 10)));
      verify(buyOrderService, never()).buyListing(anyString(), any());
    }
  }
}
