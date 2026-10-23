package footballmarket.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.config.SecurityConfig;
import footballmarket.models.QuoteExecution;
import footballmarket.models.User;
import footballmarket.models.enums.UserRole;
import footballmarket.models.enums.ValuationStrategy;
import footballmarket.models.records.MarketInitializationResult;
import footballmarket.services.AuthenticationService;
import footballmarket.services.QuoteMarketInitializationService;
import footballmarket.services.QuoteRecalculationService;
import footballmarket.services.exceptions.ExecutionAlreadyExistsException;
import footballmarket.services.exceptions.InvalidAdjustmentFactorException;
import footballmarket.services.exceptions.InvalidMinPriceFloorException;
import footballmarket.services.exceptions.InvalidVariationCapException;
import footballmarket.services.exceptions.MarketAlreadyInitializedException;
import footballmarket.services.exceptions.MarketNotInitializedException;
import footballmarket.services.exceptions.UnknownQuoteStrategyException;
import io.jsonwebtoken.Jwts;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(QuoteController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class QuoteControllerTest {
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private QuoteMarketInitializationService service;
  @MockitoBean private QuoteRecalculationService recalculationService;
  @MockitoBean private AuthenticationService authenticationService;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(authenticationService.getCurrentUser("quotes@example.com"))
        .thenReturn(
            new User(1L, "quotes.user", "quotes@example.com", "password123", UserRole.USER));
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("quotes@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Nested
  @DisplayName("Contrato de inicialización del mercado")
  class Contract {

    @Test
    @DisplayName("Devuelve la fecha de mercado, los jugadores inicializados y el estado previo")
    void returnsMarketState() throws Exception {
      when(service.initialize(any()))
          .thenReturn(new MarketInitializationResult(LocalDate.of(2026, 10, 20), 42, false));
      mvc.perform(
              post("/api/quotes/initialize")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{\"marketDate\":\"2026-10-20\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.marketDate").value("2026-10-20"))
          .andExpect(jsonPath("$.initializedPlayers").value(42))
          .andExpect(jsonPath("$.alreadyInitialized").value(false));
    }

    @Test
    @DisplayName("Usa la fecha del día cuando la solicitud no trae fecha de mercado")
    void usesDefaultMarketDateWithoutBody() throws Exception {
      when(service.initialize(any()))
          .thenReturn(new MarketInitializationResult(LocalDate.of(2026, 10, 20), 0, false));
      mvc.perform(post("/api/quotes/initialize").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.marketDate").value("2026-10-20"));
      ArgumentCaptor<LocalDate> requested = ArgumentCaptor.forClass(LocalDate.class);
      verify(service).initialize(requested.capture());
      org.assertj.core.api.Assertions.assertThat(requested.getValue()).isNull();
    }

    @Test
    @DisplayName("Traduce el rechazo del mercado ya inicializado en otra fecha")
    void translatesAlreadyInitialized() throws Exception {
      when(service.initialize(any())).thenThrow(new MarketAlreadyInitializedException());
      mvc.perform(post("/api/quotes/initialize").header("Authorization", authorization))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("MARKET_ALREADY_INITIALIZED"));
    }

    @Test
    @DisplayName("Rechaza la operación sin un JWT válido antes de invocar el caso de uso")
    void rejectsUnauthorizedRequest() throws Exception {
      mvc.perform(post("/api/quotes/initialize")).andExpect(status().isUnauthorized());
      verifyNoInteractions(service);
    }
  }

  @Nested
  @DisplayName("Contrato del recálculo manual")
  class RecalculationContract {

    @Test
    @DisplayName("Devuelve el resumen completo de la ejecución")
    void returnsExecutionSummary() throws Exception {
      when(recalculationService.recalculate(any(), any(), any(), any())).thenReturn(execution());

      mvc.perform(
              post("/api/quotes/recalculate")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{\"strategy\":\"POSITIONAL_PERFORMANCE\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.quoteDate").value("2026-10-21"))
          .andExpect(jsonPath("$.strategy").value("POSITIONAL_PERFORMANCE"))
          .andExpect(jsonPath("$.pricedPlayers").value(5))
          .andExpect(jsonPath("$.recomputedRows").value(3))
          .andExpect(jsonPath("$.carriedForwardRows").value(2))
          .andExpect(jsonPath("$.notRepricedPlayers").value(1))
          .andExpect(jsonPath("$.failedPlayers").value(0))
          .andExpect(jsonPath("$.startedAt").value("2026-10-21T10:00:00Z"))
          .andExpect(jsonPath("$.finishedAt").value("2026-10-21T10:00:05Z"));
    }

    @Test
    @DisplayName("Consulta el resumen persistido por fecha")
    void returnsExecutionsByDate() throws Exception {
      when(recalculationService.executions(LocalDate.of(2026, 10, 21)))
          .thenReturn(List.of(execution()));

      mvc.perform(
              get("/api/quotes/executions")
                  .header("Authorization", authorization)
                  .queryParam("date", "2026-10-21"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.executions.length()").value(1))
          .andExpect(jsonPath("$.executions[0].strategyVersion").value("v1"));
    }

    @Test
    @DisplayName("Rechaza una fecha de consulta ausente o malformada")
    void rejectsInvalidExecutionDate() throws Exception {
      mvc.perform(get("/api/quotes/executions").header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      mvc.perform(
              get("/api/quotes/executions")
                  .header("Authorization", authorization)
                  .queryParam("date", "invalid"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verifyNoInteractions(recalculationService);
    }

    @Test
    @DisplayName("Rechaza ambas rutas sin JWT antes del caso de uso")
    void rejectsBothRoutesWithoutJwt() throws Exception {
      mvc.perform(
              post("/api/quotes/recalculate")
                  .contentType("application/json")
                  .content("{\"strategy\":\"POSITIONAL_PERFORMANCE\"}"))
          .andExpect(status().isUnauthorized());
      mvc.perform(get("/api/quotes/executions").queryParam("date", "2026-10-21"))
          .andExpect(status().isUnauthorized());
      verifyNoInteractions(recalculationService);
    }

    @Test
    @DisplayName("Traduce todos los rechazos controlados del recálculo")
    void translatesControlledRejections() throws Exception {
      assertRejection(new UnknownQuoteStrategyException(), 400, "UNKNOWN_QUOTE_STRATEGY");
      assertRejection(new MarketNotInitializedException(), 409, "MARKET_NOT_INITIALIZED");
      assertRejection(new InvalidAdjustmentFactorException(), 400, "INVALID_ADJUSTMENT_FACTOR");
      assertRejection(new InvalidVariationCapException(), 400, "INVALID_VARIATION_CAP");
      assertRejection(new InvalidMinPriceFloorException(), 400, "INVALID_MIN_PRICE_FLOOR");
      assertRejection(new ExecutionAlreadyExistsException(), 409, "EXECUTION_ALREADY_EXISTS");
    }

    private void assertRejection(RuntimeException exception, int statusCode, String code)
        throws Exception {
      org.mockito.Mockito.reset(recalculationService);
      when(recalculationService.recalculate(any(), any(), any(), any())).thenThrow(exception);
      mvc.perform(
              post("/api/quotes/recalculate")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{\"strategy\":\"POSITIONAL_PERFORMANCE\"}"))
          .andExpect(status().is(statusCode))
          .andExpect(jsonPath("$.code").value(code));
    }

    private QuoteExecution execution() {
      return new QuoteExecution(
          LocalDate.of(2026, 10, 21),
          ValuationStrategy.POSITIONAL_PERFORMANCE,
          "v1",
          new BigDecimal("0.10"),
          new BigDecimal("0.10"),
          1L,
          5,
          3,
          2,
          1,
          0,
          Instant.parse("2026-10-21T10:00:00Z"),
          Instant.parse("2026-10-21T10:00:05Z"));
    }
  }
}
