package footballmarket.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import footballmarket.config.SecurityConfig;
import footballmarket.controllers.exceptions.InvalidStatisticsRequestException;
import footballmarket.integrations.exceptions.HeadedBrowserUnavailableException;
import footballmarket.models.User;
import footballmarket.models.enums.UserRole;
import footballmarket.services.AuthenticationService;
import footballmarket.services.exceptions.PlayerStatisticsSyncInProgressException;
import footballmarket.services.exceptions.PlayerStatisticsSynchronizationException;
import io.jsonwebtoken.Jwts;
import java.time.*;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifica los códigos de error controlados del advice con MVC real y una ruta exclusiva de test.
 */
@WebMvcTest(PlayerStatisticsContractFoundationTest.ContractFixture.class)
@Import({SecurityConfig.class, PlayerStatisticsContractFoundationTest.ContractFixture.class})
@ActiveProfiles("test")
class PlayerStatisticsContractFoundationTest {
  private static final String BASE = "/api/players/statistics/whoscored";
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private AuthenticationService authentication;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    User user = new User(1L, "stats.user", "stats@test.com", "password123", UserRole.USER);
    when(authentication.getCurrentUser("stats@test.com")).thenReturn(user);
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("stats@test.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Nested
  @DisplayName("Códigos de error controlados del advice de estadísticas")
  class Contract {
    @ParameterizedTest
    @CsvSource({
      "busy,409,PLAYER_STATISTICS_SYNC_IN_PROGRESS",
      "failed,500,PLAYER_STATISTICS_SYNC_FAILED",
      "browser,503,HEADED_BROWSER_UNAVAILABLE",
      "invalid,400,INVALID_REQUEST"
    })
    @DisplayName("Conserva los estados y códigos de errores controlados")
    void mapsControlledFailures(String failure, int expectedStatus, String code) throws Exception {
      // Act / Assert
      mvc.perform(
              get(BASE + "/foundation-error")
                  .param("failure", failure)
                  .header("Authorization", authorization))
          .andExpect(status().is(expectedStatus))
          .andExpect(jsonPath("$.status").value(expectedStatus))
          .andExpect(jsonPath("$.code").value(code))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.error").exists())
          .andExpect(jsonPath("$.message").isNotEmpty())
          .andExpect(jsonPath("$.path").value(BASE + "/foundation-error"));
    }
  }

  /**
   * Frontera MVC exclusiva de test para los errores controlados del advice.
   *
   * <p>No replica las rutas de sincronización ni de cobertura: desde la incorporación del
   * Controller de US02 las verifica {@link PlayerStatisticsControllerTest}, y duplicarlas
   * provocaría mappings ambiguos al cargar el contexto completo.
   */
  @RestController
  static class ContractFixture {
    @GetMapping(BASE + "/foundation-error")
    void error(@RequestParam String failure) {
      switch (failure) {
        case "busy" -> throw new PlayerStatisticsSyncInProgressException();
        case "browser" -> throw new HeadedBrowserUnavailableException();
        case "invalid" ->
            throw new InvalidStatisticsRequestException("INVALID_REQUEST", "Entrada no permitida");
        default -> throw new PlayerStatisticsSynchronizationException();
      }
    }
  }
}
