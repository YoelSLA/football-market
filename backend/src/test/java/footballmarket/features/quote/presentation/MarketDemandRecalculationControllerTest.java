package footballmarket.features.quote.presentation;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.quote.application.QuoteMarketInitializationService;
import footballmarket.features.quote.application.QuoteRecalculationService;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.enums.QuoteRecalculationType;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.security.PersistedIdentityValidator;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
@Tag("lightweight")
class MarketDemandRecalculationControllerTest {
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private QuoteMarketInitializationService initializationService;
  @MockitoBean private QuoteRecalculationService recalculationService;
  @MockitoBean private PersistedIdentityValidator identityValidator;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(identityValidator.exists("market-operator@example.com")).thenReturn(true);
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("market-operator@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Nested
  @DisplayName("Contrato del recálculo exclusivo de mercado")
  class MarketDemandContract {
    @Test
    @DisplayName("Devuelve el resumen sin atribuir estrategia ni factor deportivo")
    void returnsMarketOnlyExecution() throws Exception {
      // Arrange
      when(identityValidator.isSuperuser("market-operator@example.com")).thenReturn(true);
      when(recalculationService.recalculateMarketDemand()).thenReturn(execution());

      // Act / Assert
      mvc.perform(
              post("/api/quotes/recalculate/market-demand").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.origin").value("MANUAL"))
          .andExpect(jsonPath("$.recalculationType").value("MARKET_DEMAND_ONLY"))
          .andExpect(jsonPath("$.demandWindow").value("PT24H"))
          .andExpect(jsonPath("$.strategy").value(org.hamcrest.Matchers.nullValue()))
          .andExpect(jsonPath("$.strategyVersion").value(org.hamcrest.Matchers.nullValue()))
          .andExpect(jsonPath("$.adjustmentFactor").value(org.hamcrest.Matchers.nullValue()))
          .andExpect(jsonPath("$.variationCap").value(0.10))
          .andExpect(jsonPath("$.minPriceFloor").value(1));
      verify(recalculationService).recalculateMarketDemand();
    }

    @Test
    @DisplayName("Rechaza la solicitud sin JWT antes de iniciar el recálculo")
    void rejectsRequestWithoutJwt() throws Exception {
      mvc.perform(post("/api/quotes/recalculate/market-demand"))
          .andExpect(status().isUnauthorized());

      verifyNoInteractions(recalculationService);
    }

    @Test
    @DisplayName("Rechaza un usuario autenticado que no es superusuario")
    void rejectsAuthenticatedNonSuperuser() throws Exception {
      when(identityValidator.isSuperuser("market-operator@example.com")).thenReturn(false);

      mvc.perform(
              post("/api/quotes/recalculate/market-demand").header("Authorization", authorization))
          .andExpect(status().isUnauthorized());

      verifyNoInteractions(recalculationService);
    }
  }

  private QuoteExecution execution() {
    return new QuoteExecution(
        LocalDate.of(2026, 10, 19),
        QuoteExecutionOrigin.MANUAL,
        null,
        null,
        null,
        new BigDecimal("0.10"),
        1,
        5,
        5,
        0,
        0,
        0,
        Instant.parse("2026-10-19T09:00:00Z"),
        Instant.parse("2026-10-19T09:00:01Z"),
        QuoteRecalculationType.MARKET_DEMAND_ONLY,
        Duration.ofHours(24));
  }
}
