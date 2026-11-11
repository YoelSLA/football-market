package footballmarket.features.quote.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.market.application.exceptions.MarketAlreadyInitializedException;
import footballmarket.features.market.application.exceptions.MarketNotInitializedException;
import footballmarket.features.market.domain.records.MarketInitializationResult;
import footballmarket.features.quote.application.QuoteMarketInitializationService;
import footballmarket.features.quote.application.QuoteRecalculationService;
import footballmarket.features.quote.application.exceptions.InvalidAdjustmentFactorException;
import footballmarket.features.quote.application.exceptions.InvalidMinPriceFloorException;
import footballmarket.features.quote.application.exceptions.InvalidVariationCapException;
import footballmarket.features.quote.application.exceptions.UnknownQuoteStrategyException;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import footballmarket.security.PersistedIdentityValidator;
import footballmarket.security.SecurityConfig;
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
  @MockitoBean private PersistedIdentityValidator identityValidator;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(identityValidator.exists("quotes@example.com")).thenReturn(true);
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
    @DisplayName("Consulta todas las corridas manuales y automática de una misma fecha")
    void returnsMultipleDailyExecutions() throws Exception {
      // Arrange
      QuoteExecution firstManual = execution();
      QuoteExecution secondManual = execution();
      QuoteExecution automatic =
          new QuoteExecution(
              LocalDate.of(2026, 10, 21),
              QuoteExecutionOrigin.AUTOMATIC,
              ValuationStrategy.GENERAL_ATTRIBUTES,
              "v1",
              new BigDecimal("0.10"),
              new BigDecimal("0.10"),
              1L,
              5,
              2,
              2,
              0,
              1,
              Instant.parse("2026-10-21T12:00:00Z"),
              Instant.parse("2026-10-21T12:00:05Z"));
      when(recalculationService.executions(LocalDate.of(2026, 10, 21)))
          .thenReturn(List.of(automatic, secondManual, firstManual));
      // Act / Assert
      mvc.perform(
              get("/api/quotes/executions")
                  .header("Authorization", authorization)
                  .queryParam("date", "2026-10-21"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.executions.length()").value(3))
          .andExpect(jsonPath("$.executions[0].origin").value("AUTOMATIC"))
          .andExpect(jsonPath("$.executions[0].result").value("FAILED"))
          .andExpect(jsonPath("$.executions[0].failureReason").value("INDIVIDUAL_PLAYER_FAILURES"))
          .andExpect(jsonPath("$.executions[0].startedAt").value("2026-10-21T12:00:00Z"))
          .andExpect(jsonPath("$.executions[1].origin").value("MANUAL"))
          .andExpect(jsonPath("$.executions[1].result").value("SUCCESS"))
          .andExpect(jsonPath("$.executions[2].origin").value("MANUAL"))
          .andExpect(jsonPath("$.executions[2].result").value("SUCCESS"))
          .andExpect(
              jsonPath(
                  "$.executions[*].quoteDate",
                  org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("2026-10-21"))))
          .andExpect(jsonPath("$.executions[*].id").isEmpty());
    }

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
          .andExpect(jsonPath("$.origin").value("MANUAL"))
          .andExpect(jsonPath("$.result").value("SUCCESS"))
          .andExpect(jsonPath("$.failureReason").value(org.hamcrest.Matchers.nullValue()))
          .andExpect(jsonPath("$.strategy").value("POSITIONAL_PERFORMANCE"))
          .andExpect(jsonPath("$.strategyVersion").value("v1"))
          .andExpect(jsonPath("$.adjustmentFactor").value(0.10))
          .andExpect(jsonPath("$.variationCap").value(0.10))
          .andExpect(jsonPath("$.minPriceFloor").value(1))
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
          .andExpect(jsonPath("$.executions[0].quoteDate").value("2026-10-21"))
          .andExpect(jsonPath("$.executions[0].strategy").value("POSITIONAL_PERFORMANCE"))
          .andExpect(jsonPath("$.executions[0].adjustmentFactor").value(0.10))
          .andExpect(jsonPath("$.executions[0].variationCap").value(0.10))
          .andExpect(jsonPath("$.executions[0].minPriceFloor").value(1))
          .andExpect(jsonPath("$.executions[0].pricedPlayers").value(5))
          .andExpect(jsonPath("$.executions[0].recomputedRows").value(3))
          .andExpect(jsonPath("$.executions[0].carriedForwardRows").value(2))
          .andExpect(jsonPath("$.executions[0].notRepricedPlayers").value(1))
          .andExpect(jsonPath("$.executions[0].failedPlayers").value(0))
          .andExpect(jsonPath("$.executions[0].startedAt").value("2026-10-21T10:00:00Z"))
          .andExpect(jsonPath("$.executions[0].finishedAt").value("2026-10-21T10:00:05Z"))
          .andExpect(jsonPath("$.executions[0].origin").value("MANUAL"))
          .andExpect(jsonPath("$.executions[0].result").value("SUCCESS"))
          .andExpect(
              jsonPath("$.executions[0].failureReason").value(org.hamcrest.Matchers.nullValue()))
          .andExpect(jsonPath("$.executions[0].strategyVersion").value("v1"));
    }

    @Test
    @DisplayName("Expone un motivo cerrado para fallos individuales sin detalles internos")
    void returnsFailedExecutionWithoutInternalDetails() throws Exception {
      // Arrange
      QuoteExecution failed = execution();
      failed.complete(5, 2, 2, 0, 1, Instant.parse("2026-10-21T10:00:05Z"));
      when(recalculationService.recalculate(any(), any(), any(), any())).thenReturn(failed);
      // Act / Assert
      mvc.perform(
              post("/api/quotes/recalculate")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{\"strategy\":\"POSITIONAL_PERFORMANCE\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.origin").value("MANUAL"))
          .andExpect(jsonPath("$.result").value("FAILED"))
          .andExpect(jsonPath("$.failureReason").value("INDIVIDUAL_PLAYER_FAILURES"))
          .andExpect(jsonPath("$.failedPlayers").value(1))
          .andExpect(jsonPath("$.id").doesNotExist())
          .andExpect(jsonPath("$.cause").doesNotExist())
          .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName(
        "Consulta una ejecución automática con fallos y devuelve vacío para una fecha sin registro")
    void returnsAutomaticFailureAndEmptyDate() throws Exception {
      // Arrange
      QuoteExecution automatic =
          new QuoteExecution(
              LocalDate.of(2026, 10, 21),
              QuoteExecutionOrigin.AUTOMATIC,
              ValuationStrategy.POSITIONAL_PERFORMANCE,
              "v1",
              new BigDecimal("0.10"),
              new BigDecimal("0.10"),
              1L,
              5,
              2,
              2,
              0,
              1,
              Instant.parse("2026-10-21T10:00:00Z"),
              Instant.parse("2026-10-21T10:00:05Z"));
      when(recalculationService.executions(LocalDate.of(2026, 10, 21)))
          .thenReturn(List.of(automatic));
      when(recalculationService.executions(LocalDate.of(2026, 10, 22))).thenReturn(List.of());
      // Act / Assert
      mvc.perform(
              get("/api/quotes/executions")
                  .header("Authorization", authorization)
                  .queryParam("date", "2026-10-21"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.executions.length()").value(1))
          .andExpect(jsonPath("$.executions[0].origin").value("AUTOMATIC"))
          .andExpect(jsonPath("$.executions[0].result").value("FAILED"))
          .andExpect(jsonPath("$.executions[0].failureReason").value("INDIVIDUAL_PLAYER_FAILURES"))
          .andExpect(jsonPath("$.executions[0].id").doesNotExist());
      mvc.perform(
              get("/api/quotes/executions")
                  .header("Authorization", authorization)
                  .queryParam("date", "2026-10-22"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.executions").isEmpty());
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
          QuoteExecutionOrigin.MANUAL,
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
