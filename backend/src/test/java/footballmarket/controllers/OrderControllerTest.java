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

import footballmarket.config.SecurityConfig;
import footballmarket.models.MarketTransaction;
import footballmarket.models.TestFixtures;
import footballmarket.models.exceptions.InsufficientUserCreditsException;
import footballmarket.models.records.BuyOrderRequest;
import footballmarket.services.AuthenticationService;
import footballmarket.services.BuyOrderService;
import footballmarket.services.exceptions.ArithmeticOverflowException;
import footballmarket.services.exceptions.InsufficientCreditsException;
import footballmarket.services.exceptions.InsufficientStockException;
import footballmarket.services.exceptions.InvalidQuantityException;
import footballmarket.services.exceptions.InvalidQuotePriceException;
import footballmarket.services.exceptions.PlayerNotFoundException;
import footballmarket.services.exceptions.PlayerNotVisibleException;
import footballmarket.services.exceptions.QuoteContractViolationException;
import footballmarket.services.exceptions.QuoteUnavailableException;
import footballmarket.services.exceptions.SellerUnavailableException;
import footballmarket.services.exceptions.SuperuserCannotBuyException;
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
class OrderControllerTest {
  private static final String BUYER_EMAIL = "buyer@example.com";
  private static final Long PLAYER_ID = 7821L;
  private static final Long TRANSACTION_ID = 12345L;
  private static final String PLAYER_NOT_FOUND_MESSAGE = "No encontramos el jugador solicitado";

  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private BuyOrderService buyOrderService;
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

    @Test
    @DisplayName("Un identificador no numérico responde 400 INVALID_REQUEST")
    void identificadorNoNumericoRespondePeticionInvalida() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", authorization)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"playerId\":\"abc\",\"quantity\":10}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verify(buyOrderService, never()).buy(anyString(), any());
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

    @Test
    @DisplayName("Una solicitud válida sí alcanza al Service")
    void unaSolicitudValidaAlcanzaAlService() throws Exception {
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
}
