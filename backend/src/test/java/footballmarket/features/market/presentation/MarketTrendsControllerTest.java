package footballmarket.features.market.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.market.application.MarketTrendsService;
import footballmarket.features.market.domain.records.MarketHotPlayer;
import footballmarket.features.market.domain.records.MarketTrends;
import footballmarket.security.PersistedIdentityValidator;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MarketTrendsController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Tag("lightweight")
class MarketTrendsControllerTest {
  @Autowired private MockMvc mvc;
  @Autowired private SecretKey key;
  @MockitoBean private MarketTrendsService marketTrendsService;
  @MockitoBean private PersistedIdentityValidator identityValidator;

  @BeforeEach
  void setUp() {
    when(identityValidator.exists("user@example.com")).thenReturn(true);
  }

  @Nested
  @DisplayName("Contrato HTTP de tendencias")
  class TrendsResponse {
    @Test
    @DisplayName("Expone ventana, ranking, sentimiento y volúmenes con JWT")
    void returnsMarketTrendsResponse() throws Exception {
      // Arrange
      OffsetDateTime startsAt = OffsetDateTime.parse("2026-11-22T12:00:00Z");
      OffsetDateTime endsAt = OffsetDateTime.parse("2026-11-23T12:00:00Z");
      MarketHotPlayer player = new MarketHotPlayer(7821L, "Jugador", 12, 1200);
      MarketTrends trends =
          new MarketTrends(startsAt, endsAt, List.of(player), 75.0, 25.0, "BULLISH", 4, 16, 1600);
      when(marketTrendsService.getMarketTrends()).thenReturn(trends);

      // Act / Assert
      String authorization =
          "Bearer "
              + Jwts.builder()
                  .subject("user@example.com")
                  .expiration(Date.from(Instant.now().plusSeconds(300)))
                  .signWith(key)
                  .compact();
      mvc.perform(get("/api/market/trends").header(HttpHeaders.AUTHORIZATION, authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.window.startsAt").value("2026-11-22T12:00:00Z"))
          .andExpect(jsonPath("$.window.endsAt").value("2026-11-23T12:00:00Z"))
          .andExpect(jsonPath("$.topHotPlayers[0].playerId").value(7821))
          .andExpect(jsonPath("$.topHotPlayers[0].playerName").value("Jugador"))
          .andExpect(jsonPath("$.topHotPlayers[0].boughtTokens").value(12))
          .andExpect(jsonPath("$.topHotPlayers[0].buyVolume").value(1200))
          .andExpect(jsonPath("$.buyTransactionsPercentage").value(75.0))
          .andExpect(jsonPath("$.sellTransactionsPercentage").value(25.0))
          .andExpect(jsonPath("$.marketBias").value("BULLISH"))
          .andExpect(jsonPath("$.totalTransactions").value(4))
          .andExpect(jsonPath("$.totalTradedTokens").value(16))
          .andExpect(jsonPath("$.totalTradedCredits").value(1600));
    }
  }
}
