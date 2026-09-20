package footballmarket.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.support.TestcontainersConfiguration;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@Import({
  JwtAuthenticationIntegrationTest.ProtectedRouteConfig.class,
  TestcontainersConfiguration.class
})
@Transactional
class JwtAuthenticationIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy springSecurityFilterChain;
  @Autowired private JWTProvider jwtProvider;
  @Autowired private SecretKey jwtSecretKey;
  @Autowired private ObjectMapper objectMapper;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(this.context)
            .addFilters(this.springSecurityFilterChain)
            .build();
  }

  @Test
  void rechazaAccesoSinToken() throws Exception {
    this.mockMvc.perform(get("/test/protected")).andExpect(status().isUnauthorized());
  }

  @Test
  void permiteAccesoConTokenValido() throws Exception {
    String email = "jwt-flow@test.com";
    String credentials = "{\"email\":\"" + email + "\",\"password\":\"password123\"}";

    this.mockMvc
        .perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
        .andExpect(status().isCreated());

    String loginResponse =
        this.mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(credentials))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = this.objectMapper.readTree(loginResponse).get("token").asText();

    this.mockMvc
        .perform(get("/test/protected").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().string("protected"));
  }

  @Test
  void rechazaTokenConFirmaInvalida() throws Exception {
    String token = this.jwtProvider.generateToken("user@test.com");
    String invalidToken = token.substring(0, token.lastIndexOf('.') + 1) + "invalid-signature";

    this.mockMvc
        .perform(get("/test/protected").header("Authorization", "Bearer " + invalidToken))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rechazaTokenExpirado() throws Exception {
    Instant now = Instant.now();
    String expiredToken =
        Jwts.builder()
            .subject("user@test.com")
            .issuedAt(Date.from(now.minusSeconds(120)))
            .expiration(Date.from(now.minusSeconds(60)))
            .signWith(this.jwtSecretKey, Jwts.SIG.HS256)
            .compact();

    this.mockMvc
        .perform(get("/test/protected").header("Authorization", "Bearer " + expiredToken))
        .andExpect(status().isUnauthorized());
  }

  @TestConfiguration
  static class ProtectedRouteConfig {

    @Bean
    ProtectedRouteController protectedRouteController() {
      return new ProtectedRouteController();
    }
  }

  @RestController
  static class ProtectedRouteController {

    @GetMapping("/test/protected")
    String protectedResource() {
      return "protected";
    }
  }
}
