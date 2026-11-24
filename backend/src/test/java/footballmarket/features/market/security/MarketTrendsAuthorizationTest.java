package footballmarket.features.market.security;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.market.application.MarketTrendsService;
import footballmarket.features.market.presentation.MarketTrendsController;
import footballmarket.security.PersistedIdentityValidator;
import footballmarket.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MarketTrendsController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Tag("lightweight")
class MarketTrendsAuthorizationTest {
  @Autowired private MockMvc mvc;
  @MockitoBean private MarketTrendsService marketTrendsService;
  @MockitoBean private PersistedIdentityValidator identityValidator;

  @Nested
  @DisplayName("Autenticación de tendencias")
  class Authentication {
    @Test
    @DisplayName("Rechaza la consulta sin JWT y no obtiene métricas")
    void rejectsRequestWithoutToken() throws Exception {
      // Act / Assert
      mvc.perform(get("/api/market/trends")).andExpect(status().isUnauthorized());

      // Verify
      verify(marketTrendsService, never()).getMarketTrends();
    }
  }
}
