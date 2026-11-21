package footballmarket.features.dashboard.presentation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.dashboard.application.DashboardOverviewService;
import footballmarket.features.dashboard.application.DashboardPerformanceService;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardAllocationDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardFinancialSummaryDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardMarketPulseDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardOverviewResponseDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardPerformanceResponseDTO;
import footballmarket.security.PersistedIdentityValidator;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

@WebMvcTest({DashboardController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Tag("lightweight")
class DashboardControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;

  @MockitoBean private DashboardOverviewService overviewService;
  @MockitoBean private DashboardPerformanceService performanceService;
  @MockitoBean private PersistedIdentityValidator identityValidator;

  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(identityValidator.exists("test@example.com")).thenReturn(true);

    String token =
        Jwts.builder()
            .subject("test@example.com")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(this.key)
            .compact();

    this.authorization = "Bearer " + token;
    this.mvc = MockMvcBuilders.webAppContextSetup(this.context).addFilters(this.security).build();
  }

  @Test
  @DisplayName("GET /api/dashboard/overview sin sesión es rechazado con 401")
  void overviewUnauthorized() throws Exception {
    this.mvc.perform(get("/api/dashboard/overview")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/dashboard/overview con sesión retorna 200 y el payload consolidado")
  void overviewSuccess() throws Exception {
    DashboardFinancialSummaryDTO summary =
        new DashboardFinancialSummaryDTO(
            500000L,
            1000000L,
            1500000L,
            800000L,
            200000L,
            50000L,
            250000L,
            new BigDecimal("31.25"),
            50,
            40,
            10,
            true);

    DashboardAllocationDTO allocation =
        new DashboardAllocationDTO(List.of(), List.of(), new BigDecimal("25.00"), "Haaland");

    DashboardMarketPulseDTO pulse = new DashboardMarketPulseDTO(100000000L, 2000000L, 500L, 20);

    DashboardOverviewResponseDTO response =
        new DashboardOverviewResponseDTO(
            summary,
            allocation,
            pulse,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            OffsetDateTime.now());

    when(this.overviewService.getOverview("test@example.com")).thenReturn(response);

    this.mvc
        .perform(get("/api/dashboard/overview").header("Authorization", this.authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.financialSummary.availableCredits").value(500000))
        .andExpect(jsonPath("$.financialSummary.netWorth").value(1500000))
        .andExpect(jsonPath("$.allocation.topHoldingPlayerName").value("Haaland"));
  }

  @Test
  @DisplayName("GET /api/dashboard/performance con sesión retorna 200 y series")
  void performanceSuccess() throws Exception {
    DashboardPerformanceResponseDTO response =
        new DashboardPerformanceResponseDTO(
            List.of(LocalDate.parse("2026-11-20")),
            List.of(1000000L),
            List.of(new BigDecimal("100.00")));

    when(this.performanceService.getPerformance(anyString(), anyString())).thenReturn(response);

    this.mvc
        .perform(get("/api/dashboard/performance").header("Authorization", this.authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dates[0]").value("2026-11-20"))
        .andExpect(jsonPath("$.userPortfolioSeries[0]").value(1000000));
  }
}
