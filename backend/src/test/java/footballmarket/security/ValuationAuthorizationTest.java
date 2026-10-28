package footballmarket.security;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.auth.application.AuthenticationService;
import footballmarket.features.auth.application.exceptions.CurrentUserNotFoundException;
import footballmarket.features.valuation.application.ValuationEvaluationService;
import footballmarket.features.valuation.presentation.ValuationController;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
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
class ValuationAuthorizationTest {
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private ValuationEvaluationService service;
  @MockitoBean private AuthenticationService authenticationService;

  @Nested
  @DisplayName("Protección Bearer de valoración")
  class Authorization {
    @Test
    @DisplayName("Rechaza la evaluación sin JWT")
    void rejectsMissingJwt() throws Exception {
      MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
      mvc.perform(post("/api/valuations/evaluate")).andExpect(status().isUnauthorized());
      verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Rechaza un JWT con firma inválida")
    void rejectsInvalidJwt() throws Exception {
      MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
      mvc.perform(post("/api/valuations/evaluate").header("Authorization", "Bearer invalid"))
          .andExpect(status().isUnauthorized());
      verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Rechaza un JWT cuyo usuario ya no está persistido")
    void rejectsMissingPersistedUser() throws Exception {
      when(authenticationService.getCurrentUser("missing@example.com"))
          .thenThrow(new CurrentUserNotFoundException());
      String token =
          "Bearer "
              + Jwts.builder()
                  .subject("missing@example.com")
                  .expiration(Date.from(Instant.now().plusSeconds(300)))
                  .signWith(key)
                  .compact();
      MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
      mvc.perform(post("/api/valuations/evaluate").header("Authorization", token))
          .andExpect(status().isUnauthorized());
      verifyNoInteractions(service);
    }
  }
}
