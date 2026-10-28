package footballmarket.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.auth.application.AuthenticationService;
import footballmarket.features.portfolio.application.PortfolioService;
import footballmarket.features.portfolio.domain.records.PortfolioView;
import footballmarket.features.portfolio.presentation.PortfolioController;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

/** Contrato HTTP de la consulta del portfolio con la cadena JWT productiva. */
@WebMvcTest({PortfolioController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class PortfolioControllerTest {

  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-20T12:00:00Z");

  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;

  @MockitoBean private PortfolioService portfolioService;
  @MockitoBean private AuthenticationService authenticationService;

  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void arrange() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("buyer@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Nested
  @DisplayName("Consulta HTTP del portfolio")
  class Consulta {

    @Test
    @DisplayName("Devuelve resumen, posiciones y paginación con los valores del portfolio")
    void devuelveElPortfolio() throws Exception {
      // Arrange
      when(portfolioService.portfolio(anyString(), anyInt(), anyInt(), isNull()))
          .thenReturn(view());
      // Act / Assert
      mvc.perform(get("/api/portfolio/me").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.summary.totalPositions").value(1))
          .andExpect(jsonPath("$.summary.totalTokens").value(10))
          .andExpect(jsonPath("$.summary.totalReservedTokens").value(0))
          .andExpect(jsonPath("$.summary.totalAvailableTokens").value(10))
          .andExpect(jsonPath("$.summary.totalInvested").value(1000))
          .andExpect(jsonPath("$.summary.totalCurrentValue").value(1000))
          .andExpect(jsonPath("$.summary.totalRealizedProfitLoss").value(0))
          .andExpect(jsonPath("$.summary.totalUnrealizedProfitLoss").value(0))
          .andExpect(jsonPath("$.summary.totalProfitLoss").value(0))
          .andExpect(jsonPath("$.summary.totalProfitLossPercentage").value(0.00))
          .andExpect(jsonPath("$.summary.totalBuyTransactions").value(1))
          .andExpect(jsonPath("$.summary.totalSellTransactions").value(0))
          .andExpect(jsonPath("$.summary.valuationComplete").value(true))
          .andExpect(jsonPath("$.positions.length()").value(1))
          .andExpect(jsonPath("$.positions[0].playerId").value(41))
          .andExpect(jsonPath("$.positions[0].playerName").value("Joel Robles"))
          .andExpect(jsonPath("$.positions[0].teamName").value("Real Betis Balompié"))
          .andExpect(jsonPath("$.positions[0].position").value("Goalkeeper"))
          .andExpect(jsonPath("$.positions[0].imageUrl").value("https://example.org/41.png"))
          .andExpect(jsonPath("$.positions[0].active").value(true))
          .andExpect(jsonPath("$.positions[0].status").value("OPEN"))
          .andExpect(jsonPath("$.positions[0].valuationStatus").value("VALUED"))
          .andExpect(jsonPath("$.positions[0].quantity").value(10))
          .andExpect(jsonPath("$.positions[0].reservedQuantity").value(0))
          .andExpect(jsonPath("$.positions[0].availableQuantity").value(10))
          .andExpect(jsonPath("$.positions[0].acquisitionCost").value(1000))
          .andExpect(jsonPath("$.positions[0].averageUnitPrice").value(100))
          .andExpect(jsonPath("$.positions[0].currentUnitPrice").value(100))
          .andExpect(jsonPath("$.positions[0].priceUpdatedAt").exists())
          .andExpect(jsonPath("$.positions[0].currentValue").value(1000))
          .andExpect(jsonPath("$.positions[0].realizedProfitLoss").value(0))
          .andExpect(jsonPath("$.positions[0].unrealizedProfitLoss").value(0))
          .andExpect(jsonPath("$.positions[0].totalProfitLoss").value(0))
          .andExpect(jsonPath("$.positions[0].unrealizedProfitLossPercentage").value(0.00))
          .andExpect(jsonPath("$.positions[0].portfolioWeightPercentage").value(100.00))
          .andExpect(jsonPath("$.positions[0].openedAt").exists())
          .andExpect(jsonPath("$.positions[0].closedAt").doesNotExist())
          .andExpect(jsonPath("$.positions[0].lastTransactionAt").exists())
          .andExpect(jsonPath("$.positions[0].totalBuyTransactions").value(1))
          .andExpect(jsonPath("$.positions[0].totalSellTransactions").value(0))
          .andExpect(jsonPath("$.pagination.page").value(0))
          .andExpect(jsonPath("$.pagination.size").value(20))
          .andExpect(jsonPath("$.pagination.totalElements").value(1))
          .andExpect(jsonPath("$.pagination.totalPages").value(1));
    }

    @Test
    @DisplayName("La consulta exige sesión y no invoca el Service sin ella")
    void exigeSesion() throws Exception {
      // Act / Assert
      mvc.perform(get("/api/portfolio/me")).andExpect(status().isUnauthorized());
      // Verify
      verifyNoInteractions(portfolioService);
    }

    @Test
    @DisplayName("Los parámetros de consulta se trasladan al caso de uso")
    void trasladaParametros() throws Exception {
      // Arrange
      when(portfolioService.portfolio(anyString(), anyInt(), anyInt(), any())).thenReturn(view());
      // Act
      mvc.perform(
              get("/api/portfolio/me")
                  .param("page", "2")
                  .param("size", "5")
                  .param("status", "OPEN")
                  .header("Authorization", authorization))
          .andExpect(status().isOk());
      // Verify
      org.mockito.Mockito.verify(portfolioService)
          .portfolio(eq("buyer@example.com"), eq(2), eq(5), eq("OPEN"));
    }

    @Test
    @DisplayName("Un parámetro inválido se rechaza con 400 y el cuerpo de error vigente")
    void rechazaParametroInvalido() throws Exception {
      // Arrange
      when(portfolioService.portfolio(anyString(), anyInt(), anyInt(), any()))
          .thenThrow(
              new footballmarket.features.portfolio.presentation.exceptions
                  .InvalidPortfolioStatusException());
      // Act / Assert
      mvc.perform(
              get("/api/portfolio/me")
                  .param("status", "open")
                  .header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.error").isString())
          .andExpect(jsonPath("$.message").isString())
          .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"))
          .andExpect(jsonPath("$.path").value("/api/portfolio/me"));
    }

    @Test
    @DisplayName("Una inconsistencia del mercado se traduce en un error de integridad")
    void traduceErrorDeIntegridad() throws Exception {
      // Arrange
      when(portfolioService.portfolio(anyString(), anyInt(), anyInt(), any()))
          .thenThrow(
              new footballmarket.features.portfolio.domain.exceptions
                  .PortfolioIntegrityException());
      // Act / Assert
      mvc.perform(get("/api/portfolio/me").header("Authorization", authorization))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
          .andExpect(jsonPath("$.path").value("/api/portfolio/me"));
    }

    @Test
    @DisplayName("La indisponibilidad de cotizaciones se traduce en 503 sin datos parciales")
    void traduceIndisponibilidadDeCotizaciones() throws Exception {
      // Arrange
      when(portfolioService.portfolio(anyString(), anyInt(), anyInt(), any()))
          .thenThrow(
              new footballmarket.features.quote.application.exceptions
                  .QuoteBatchUnavailableException());
      // Act / Assert
      mvc.perform(get("/api/portfolio/me").header("Authorization", authorization))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("QUOTE_SOURCE_UNAVAILABLE"))
          .andExpect(jsonPath("$.path").value("/api/portfolio/me"));
    }
  }

  private PortfolioView view() {
    PortfolioView.PortfolioPosition position =
        new PortfolioView.PortfolioPosition(
            41L,
            "Joel Robles",
            "Real Betis Balompié",
            "Goalkeeper",
            "https://example.org/41.png",
            true,
            "OPEN",
            "VALUED",
            10,
            0,
            10,
            1000L,
            100L,
            100L,
            NOW,
            1000L,
            0L,
            0L,
            0L,
            new BigDecimal("0.00"),
            new BigDecimal("100.00"),
            NOW,
            null,
            NOW,
            1L,
            0L);
    PortfolioView.PortfolioSummary summary =
        new PortfolioView.PortfolioSummary(
            1, 10, 0, 10, 1000L, 1000L, 0L, 0L, 0L, new BigDecimal("0.00"), 1L, 0L, true);
    return new PortfolioView(
        summary, List.of(position), new PortfolioView.PortfolioPagination(0, 20, 1, 1));
  }
}
