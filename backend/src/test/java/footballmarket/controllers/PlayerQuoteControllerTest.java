package footballmarket.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.config.SecurityConfig;
import footballmarket.models.League;
import footballmarket.models.Player;
import footballmarket.models.PlayerQuote;
import footballmarket.models.Team;
import footballmarket.models.User;
import footballmarket.models.enums.QuoteOrigin;
import footballmarket.models.enums.UserRole;
import footballmarket.models.enums.ValuationStrategy;
import footballmarket.models.records.PlayerRankingEntry;
import footballmarket.services.AuthenticationService;
import footballmarket.services.QuoteQueryService;
import footballmarket.services.exceptions.QuoteNotFoundException;
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
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(PlayerQuoteController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class PlayerQuoteControllerTest {
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private QuoteQueryService queryService;
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
  @DisplayName("Historial de cotizaciones")
  class History {

    @Test
    @DisplayName("Devuelve el historial del jugador con sus datos de auditoría")
    void returnsHistory() throws Exception {
      when(queryService.findHistory(7821L)).thenReturn(List.of(recomputed(), initial()));

      mvc.perform(get("/api/players/7821/quotes").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.playerId").value(7821))
          .andExpect(jsonPath("$.quotes.length()").value(2))
          .andExpect(jsonPath("$.quotes[0].effectiveDate").value("2026-10-20"))
          .andExpect(jsonPath("$.quotes[0].price").value(105))
          .andExpect(jsonPath("$.quotes[0].previousPrice").value(100))
          .andExpect(jsonPath("$.quotes[0].multiplier").value(Matchers.closeTo(1.05, 0.0001)))
          .andExpect(jsonPath("$.quotes[0].score").value(Matchers.closeTo(0.6, 0.0001)))
          .andExpect(jsonPath("$.quotes[0].strategy").value("POSITIONAL_PERFORMANCE"))
          .andExpect(jsonPath("$.quotes[0].strategyVersion").value("v1"))
          .andExpect(jsonPath("$.quotes[0].sourceValuationDate").value("2026-10-20"))
          .andExpect(jsonPath("$.quotes[0].origin").value("RECOMPUTED"))
          .andExpect(jsonPath("$.quotes[0].calculatedAt").value("2026-10-20T18:00:00Z"))
          .andExpect(jsonPath("$.quotes[1].origin").value("INITIAL"))
          .andExpect(jsonPath("$.quotes[1].previousPrice").value(Matchers.nullValue()))
          .andExpect(jsonPath("$.quotes[1].strategy").value(Matchers.nullValue()));
    }

    @Test
    @DisplayName("Devuelve un historial vacío cuando el jugador no fue cotizado")
    void returnsEmptyHistory() throws Exception {
      when(queryService.findHistory(7821L)).thenReturn(List.of());

      mvc.perform(get("/api/players/7821/quotes").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.playerId").value(7821))
          .andExpect(jsonPath("$.quotes.length()").value(0));
    }
  }

  @Nested
  @DisplayName("Cotización a una fecha dada")
  class QuoteOnDate {

    @Test
    @DisplayName("Devuelve la cotización vigente a la fecha consultada")
    void returnsQuoteOnDate() throws Exception {
      when(queryService.findOnDate(7821L, LocalDate.of(2026, 10, 20))).thenReturn(recomputed());

      mvc.perform(
              get("/api/players/7821/quotes")
                  .header("Authorization", authorization)
                  .queryParam("date", "2026-10-20"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.effectiveDate").value("2026-10-20"))
          .andExpect(jsonPath("$.price").value(105))
          .andExpect(jsonPath("$.origin").value("RECOMPUTED"))
          .andExpect(jsonPath("$.calculatedAt").value("2026-10-20T18:00:00Z"));
    }

    @Test
    @DisplayName("Traduce la ausencia de cotización a esa fecha")
    void translatesMissingQuote() throws Exception {
      when(queryService.findOnDate(any(), any())).thenThrow(new QuoteNotFoundException());

      mvc.perform(
              get("/api/players/7821/quotes")
                  .header("Authorization", authorization)
                  .queryParam("date", "2026-10-20"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("Cotización vigente")
  class Current {

    @Test
    @DisplayName("Devuelve la cotización vigente con su instante de cálculo de origen")
    void returnsCurrentQuote() throws Exception {
      when(queryService.findCurrent(7821L)).thenReturn(recomputed());

      mvc.perform(get("/api/players/7821/quotes/current").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.effectiveDate").value("2026-10-20"))
          .andExpect(jsonPath("$.price").value(105))
          .andExpect(jsonPath("$.origin").value("RECOMPUTED"))
          .andExpect(jsonPath("$.calculatedAt").value("2026-10-20T18:00:00Z"));
    }

    @Test
    @DisplayName("Traduce la ausencia de cotización vigente")
    void translatesMissingCurrentQuote() throws Exception {
      when(queryService.findCurrent(7821L)).thenThrow(new QuoteNotFoundException());

      mvc.perform(get("/api/players/7821/quotes/current").header("Authorization", authorization))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("Ranking por precio vigente")
  class Ranking {

    @Test
    @DisplayName("Devuelve los jugadores ordenados por precio con una entrada por posición")
    void returnsRanking() throws Exception {
      when(queryService.findRanking())
          .thenReturn(
              List.of(
                  new PlayerRankingEntry(
                      7821L,
                      105L,
                      LocalDate.of(2026, 10, 20),
                      QuoteOrigin.RECOMPUTED,
                      Instant.parse("2026-10-20T18:00:00Z")),
                  new PlayerRankingEntry(
                      7822L,
                      100L,
                      LocalDate.of(2026, 10, 20),
                      QuoteOrigin.CARRIED_FORWARD,
                      Instant.parse("2026-10-20T18:00:00Z"))));

      mvc.perform(get("/api/players/ranking").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ranking.length()").value(2))
          .andExpect(jsonPath("$.ranking[0].position").value(1))
          .andExpect(jsonPath("$.ranking[0].playerId").value(7821))
          .andExpect(jsonPath("$.ranking[0].price").value(105))
          .andExpect(jsonPath("$.ranking[0].effectiveDate").value("2026-10-20"))
          .andExpect(jsonPath("$.ranking[0].origin").value("RECOMPUTED"))
          .andExpect(jsonPath("$.ranking[0].calculatedAt").value("2026-10-20T18:00:00Z"))
          .andExpect(jsonPath("$.ranking[1].position").value(2))
          .andExpect(jsonPath("$.ranking[1].playerId").value(7822))
          .andExpect(jsonPath("$.ranking[1].price").value(100))
          .andExpect(jsonPath("$.ranking[1].origin").value("CARRIED_FORWARD"));
    }

    @Test
    @DisplayName("Devuelve un ranking vacío cuando todavía no hay cotizaciones")
    void returnsEmptyRanking() throws Exception {
      when(queryService.findRanking()).thenReturn(List.of());

      mvc.perform(get("/api/players/ranking").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ranking.length()").value(0));
    }
  }

  @Nested
  @DisplayName("Validación de la consulta y seguridad")
  class ValidationAndSecurity {

    @Test
    @DisplayName("Rechaza un identificador malformado")
    void rejectsMalformedIdentifier() throws Exception {
      mvc.perform(get("/api/players/abc/quotes").header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
      verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("Rechaza una fecha de consulta malformada")
    void rejectsMalformedDate() throws Exception {
      mvc.perform(
              get("/api/players/7821/quotes")
                  .header("Authorization", authorization)
                  .queryParam("date", "2026-13-45"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
      verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("Rechaza las cuatro rutas sin un JWT válido antes del caso de uso")
    void rejectsUnauthorizedRequests() throws Exception {
      mvc.perform(get("/api/players/7821/quotes")).andExpect(status().isUnauthorized());
      mvc.perform(get("/api/players/7821/quotes").queryParam("date", "2026-10-20"))
          .andExpect(status().isUnauthorized());
      mvc.perform(get("/api/players/7821/quotes/current")).andExpect(status().isUnauthorized());
      mvc.perform(get("/api/players/ranking")).andExpect(status().isUnauthorized());
      verifyNoInteractions(queryService);
    }
  }

  private PlayerQuote recomputed() {
    return new PlayerQuote(
        player(),
        LocalDate.of(2026, 10, 20),
        105L,
        100L,
        new BigDecimal("1.050000"),
        new BigDecimal("0.6"),
        ValuationStrategy.POSITIONAL_PERFORMANCE,
        "v1",
        LocalDate.of(2026, 10, 20),
        QuoteOrigin.RECOMPUTED,
        Instant.parse("2026-10-20T18:00:00Z"));
  }

  private PlayerQuote initial() {
    return new PlayerQuote(
        player(),
        LocalDate.of(2026, 10, 17),
        100L,
        null,
        BigDecimal.ONE,
        null,
        null,
        null,
        null,
        QuoteOrigin.INITIAL,
        Instant.parse("2026-10-17T18:00:00Z"));
  }

  private Player player() {
    League league = new League("League 1");
    Team team = new Team("Team 1", league, true);
    return new Player("Player 1", team, "Forward");
  }
}
