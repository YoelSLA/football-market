package footballmarket.controllers;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.auth.application.AuthenticationService;
import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.domain.enums.UserRole;
import footballmarket.features.ranking.application.ScoreRankingQueryService;
import footballmarket.features.ranking.domain.records.ScoreRanking;
import footballmarket.features.ranking.domain.records.ScoreRankingEntry;
import footballmarket.features.ranking.presentation.ScoreRankingController;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(ScoreRankingController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class ScoreRankingControllerTest {
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private ScoreRankingQueryService queryService;
  @MockitoBean private AuthenticationService authenticationService;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(authenticationService.getCurrentUser("ranking@example.com"))
        .thenReturn(
            new User(1L, "ranking.user", "ranking@example.com", "password123", UserRole.USER));
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("ranking@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Nested
  @DisplayName("Contrato del ranking por score")
  class Contract {
    @Test
    @DisplayName("Devuelve exclusivamente el encabezado y las entradas declaradas")
    void returnsExactContract() throws Exception {
      when(queryService.findScoreRanking())
          .thenReturn(
              new ScoreRanking(
                  ValuationStrategy.POSITIONAL_PERFORMANCE,
                  LocalDate.of(2026, 10, 23),
                  List.of(
                      new ScoreRankingEntry(
                          1, 7821L, new BigDecimal("0.913400"), LocalDate.of(2026, 10, 21)))));

      mvc.perform(
              get("/api/players/ranking/score")
                  .header("Authorization", authorization)
                  .queryParam("strategy", "ignored"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.*").value(Matchers.hasSize(3)))
          .andExpect(jsonPath("$.strategy").value("POSITIONAL_PERFORMANCE"))
          .andExpect(jsonPath("$.quoteDate").value("2026-10-23"))
          .andExpect(jsonPath("$.ranking[0].*").value(Matchers.hasSize(4)))
          .andExpect(jsonPath("$.ranking[0].position").value(1))
          .andExpect(jsonPath("$.ranking[0].playerId").value(7821))
          .andExpect(jsonPath("$.ranking[0].score").value(Matchers.closeTo(0.9134, 0.000001)))
          .andExpect(jsonPath("$.ranking[0].valuationDate").value("2026-10-21"));
    }

    @Test
    @DisplayName("Rechaza la consulta sin JWT antes del caso de uso")
    void rejectsMissingJwt() throws Exception {
      mvc.perform(get("/api/players/ranking/score")).andExpect(status().isUnauthorized());
      verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("Rechaza la consulta con JWT inválido antes del caso de uso")
    void rejectsInvalidJwt() throws Exception {
      mvc.perform(get("/api/players/ranking/score").header("Authorization", "Bearer invalid.token"))
          .andExpect(status().isUnauthorized());
      verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("Devuelve el contrato vacío cuando todavía no hay ejecuciones")
    void returnsEmptyContractWithoutExecutions() throws Exception {
      when(queryService.findScoreRanking()).thenReturn(new ScoreRanking(null, null, List.of()));

      mvc.perform(get("/api/players/ranking/score").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.*").value(Matchers.hasSize(3)))
          .andExpect(jsonPath("$.strategy").value(Matchers.nullValue()))
          .andExpect(jsonPath("$.quoteDate").value(Matchers.nullValue()))
          .andExpect(jsonPath("$.ranking").isArray())
          .andExpect(jsonPath("$.ranking").isEmpty());
    }

    @Test
    @DisplayName("Conserva el encabezado cuando la estrategia no tiene entradas")
    void returnsHeaderWithEmptyRanking() throws Exception {
      when(queryService.findScoreRanking())
          .thenReturn(
              new ScoreRanking(
                  ValuationStrategy.GENERAL_ATTRIBUTES, LocalDate.of(2026, 10, 23), List.of()));

      mvc.perform(get("/api/players/ranking/score").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.*").value(Matchers.hasSize(3)))
          .andExpect(jsonPath("$.strategy").value("GENERAL_ATTRIBUTES"))
          .andExpect(jsonPath("$.quoteDate").value("2026-10-23"))
          .andExpect(jsonPath("$.ranking").isEmpty());
    }

    @Test
    @DisplayName("Comunica un fallo técnico sin devolver una clasificación parcial")
    void returnsInternalErrorWithoutPartialRanking() throws Exception {
      when(queryService.findScoreRanking())
          .thenThrow(new DataAccessResourceFailureException("database unavailable"));

      mvc.perform(get("/api/players/ranking/score").header("Authorization", authorization))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.*").value(Matchers.hasSize(6)))
          .andExpect(jsonPath("$.status").value(500))
          .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
          .andExpect(jsonPath("$.path").value("/api/player/ranking/score"))
          .andExpect(jsonPath("$.ranking").doesNotExist())
          .andExpect(jsonPath("$.strategyVersion").doesNotExist());
    }
  }
}
