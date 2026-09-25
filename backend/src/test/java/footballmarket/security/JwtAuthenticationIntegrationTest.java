package footballmarket.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.models.User;
import footballmarket.repositories.UserRepository;
import footballmarket.services.AuthenticationService;
import footballmarket.support.TestcontainersConfiguration;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
  JwtAuthenticationIntegrationTest.ProtectedRouteController.class,
  TestcontainersConfiguration.class
})
@Transactional
class JwtAuthenticationIntegrationTest {

  @Autowired private WebApplicationContext context;

  @Autowired private FilterChainProxy springSecurityFilterChain;

  @Autowired private JWTProvider jwtProvider;

  @Autowired private SecretKey jwtSecretKey;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private AuthenticationService authenticationService;

  @Autowired private UserRepository userRepository;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
  }

  /*
   * Controller utilizado exclusivamente por este test de integración.
   *
   * Nos permite comprobar que Spring Security protege correctamente
   * una ruta que no tiene ninguna regla especial de acceso.
   */
  @RestController
  static class ProtectedRouteController {

    @GetMapping("/test/protected")
    String protectedResource() {
      return "protected";
    }
  }

  @Nested
  @DisplayName("Documentación de seguridad HTTP")
  class ApiDocumentation {

    @Test
    void documentaUsuarioActualProtegidoSinProtegerRegistroNiLogin() throws Exception {
      mockMvc
          .perform(get("/v3/api-docs"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.paths['/api/auth/me'].get.security[0].bearerAuth").isArray())
          .andExpect(jsonPath("$.paths['/api/auth/me'].get.responses['200']").exists())
          .andExpect(
              jsonPath("$.paths['/api/auth/me'].get.responses['401'].content").doesNotExist())
          .andExpect(jsonPath("$.paths['/api/auth/register'].post.security").doesNotExist())
          .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").doesNotExist());
    }
  }

  @Nested
  @DisplayName("Validación de la identidad persistida")
  class PersistedIdentity {

    @Test
    void recuperaUsuarioActualYRechazaSuTokenTrasEliminarlo() throws Exception {
      User user = new User("persisted-jwt@test.com", "password123");

      authenticationService.register(user);

      String token = authenticationService.login(user.getEmail(), "password123");

      mockMvc
          .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(user.getId()))
          .andExpect(jsonPath("$.email").value(user.getEmail()));

      userRepository.delete(user);
      userRepository.flush();

      mockMvc
          .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "missing@test.com"})
    void rechazaJwtFirmadoSinIdentidadPersistida(String subject) throws Exception {
      String token =
          Jwts.builder()
              .subject(subject)
              .expiration(Date.from(Instant.now().plusSeconds(300)))
              .signWith(jwtSecretKey, Jwts.SIG.HS256)
              .compact();

      mockMvc
          .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
    }
  }

  @Nested
  @DisplayName("Acceso protegido mediante JWT")
  class ProtectedAccess {

    @Test
    void rechazaAccesoSinToken() throws Exception {
      mockMvc.perform(get("/test/protected")).andExpect(status().isUnauthorized());

      mockMvc
          .perform(get("/api/auth/me"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
    }

    @Test
    void permiteAccesoConTokenValido() throws Exception {
      String email = "jwt-flow@test.com";

      String credentials = "{\"email\":\"" + email + "\",\"password\":\"password123\"}";

      mockMvc
          .perform(
              post("/api/auth/register")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(credentials))
          .andExpect(status().isCreated());

      String loginResponse =
          mockMvc
              .perform(
                  post("/api/auth/login")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(credentials))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      String token = objectMapper.readTree(loginResponse).get("token").asText();

      mockMvc
          .perform(get("/test/protected").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(content().string("protected"));
    }

    @Test
    void rechazaTokenConFirmaInvalida() throws Exception {
      String token = jwtProvider.generateToken("user@test.com");

      String invalidToken = token.substring(0, token.lastIndexOf('.') + 1) + "invalid-signature";

      mockMvc
          .perform(get("/test/protected").header("Authorization", "Bearer " + invalidToken))
          .andExpect(status().isUnauthorized());

      mockMvc
          .perform(get("/api/auth/me").header("Authorization", "Bearer " + invalidToken))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
    }

    @Test
    void rechazaTokenExpirado() throws Exception {
      Instant now = Instant.now();

      String expiredToken =
          Jwts.builder()
              .subject("user@test.com")
              .issuedAt(Date.from(now.minusSeconds(120)))
              .expiration(Date.from(now.minusSeconds(60)))
              .signWith(jwtSecretKey, Jwts.SIG.HS256)
              .compact();

      mockMvc
          .perform(get("/test/protected").header("Authorization", "Bearer " + expiredToken))
          .andExpect(status().isUnauthorized());

      mockMvc
          .perform(get("/api/auth/me").header("Authorization", "Bearer " + expiredToken))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
    }
  }
}
