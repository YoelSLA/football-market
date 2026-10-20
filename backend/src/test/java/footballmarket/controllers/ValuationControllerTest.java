package footballmarket.controllers;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.config.SecurityConfig;
import footballmarket.models.User;
import footballmarket.models.enums.UserRole;
import footballmarket.models.enums.ValuationExclusionReason;
import footballmarket.models.records.ValuationEvaluationSummary;
import footballmarket.services.AuthenticationService;
import footballmarket.services.ValuationEvaluationService;
import footballmarket.services.exceptions.ValuationEvaluationException;
import footballmarket.services.exceptions.ValuationSnapshotUnavailableException;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
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

@WebMvcTest(ValuationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class ValuationControllerTest {
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private ValuationEvaluationService service;
  @MockitoBean private AuthenticationService authenticationService;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(authenticationService.getCurrentUser("valuation@example.com"))
        .thenReturn(
            new User(1L, "valuation.user", "valuation@example.com", "password123", UserRole.USER));
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("valuation@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Nested
  @DisplayName("Contrato de evaluación poblacional")
  class Contract {
    @Test
    @DisplayName("Devuelve el resumen exacto sin entrada")
    void returnsSummary() throws Exception {
      EnumMap<ValuationExclusionReason, Long> exclusions =
          new EnumMap<>(ValuationExclusionReason.class);
      for (ValuationExclusionReason reason : ValuationExclusionReason.values())
        exclusions.put(reason, 0L);
      when(service.evaluate())
          .thenReturn(
              new ValuationEvaluationSummary(
                  LocalDate.of(2026, 10, 19), 2, 1, exclusions, Map.of("goalsPer90", 1L)));
      mvc.perform(post("/api/valuations/evaluate").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.valuationDate").value("2026-10-19"))
          .andExpect(jsonPath("$.evaluatedPlayers").value(2))
          .andExpect(jsonPath("$.failedPlayers").value(1))
          .andExpect(jsonPath("$.excludedPlayers.NO_APPEARANCES").value(0));
    }

    @Test
    @DisplayName("Rechaza query antes de invocar el caso de uso")
    void rejectsInput() throws Exception {
      mvc.perform(
              post("/api/valuations/evaluate?date=2026-10-19")
                  .header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Traduce la ausencia de snapshot confirmado")
    void translatesUnavailableSnapshot() throws Exception {
      when(service.evaluate()).thenThrow(new ValuationSnapshotUnavailableException());
      mvc.perform(post("/api/valuations/evaluate").header("Authorization", authorization))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("VALUATION_SNAPSHOT_UNAVAILABLE"));
    }

    @Test
    @DisplayName("Traduce el fallo global controlado")
    void translatesGlobalFailure() throws Exception {
      when(service.evaluate()).thenThrow(new ValuationEvaluationException());
      mvc.perform(post("/api/valuations/evaluate").header("Authorization", authorization))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.code").value("VALUATION_EVALUATION_FAILED"));
    }
  }
}
