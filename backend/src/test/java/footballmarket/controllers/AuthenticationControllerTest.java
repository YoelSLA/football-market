package footballmarket.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.config.SecurityConfig;
import footballmarket.controllers.dtos.requests.LoginRequestDTO;
import footballmarket.controllers.dtos.requests.RegisterRequestDTO;
import footballmarket.models.User;
import footballmarket.models.enums.UserRole;
import footballmarket.services.AuthenticationService;
import footballmarket.services.exceptions.CurrentUserNotFoundException;
import footballmarket.services.exceptions.EmailAlreadyRegisteredException;
import footballmarket.services.exceptions.InvalidCredentialsException;
import footballmarket.services.exceptions.UsernameAlreadyRegisteredException;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthenticationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AuthenticationControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey jwtSecretKey;
  @MockitoBean private AuthenticationService authenticationService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
  }

  @Nested
  @DisplayName("Consulta del usuario actual")
  class CurrentUser {
    @Test
    @DisplayName("Rechaza consultar el usuario actual sin token y sin invocar el caso de uso")
    void rechazaConsultaSinTokenConCuerpoVacio() throws Exception {
      mockMvc
          .perform(get("/api/auth/me"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      verifyNoInteractions(authenticationService);
    }

    @ParameterizedTest
    @DisplayName("Rechaza credenciales inválidas antes de consultar al usuario")
    @ValueSource(strings = {"Bearer invalid", "Bearer", "Basic invalid", "expired", "signature"})
    void rechazaCredencialesInvalidasAntesDelCasoDeUso(String authorization) throws Exception {
      if (authorization.equals("expired")) {
        authorization =
            "Bearer "
                + Jwts.builder()
                    .subject("current@test.com")
                    .issuedAt(Date.from(Instant.now().minusSeconds(600)))
                    .expiration(Date.from(Instant.now().minusSeconds(300)))
                    .signWith(jwtSecretKey, Jwts.SIG.HS256)
                    .compact();
      } else if (authorization.equals("signature")) {
        authorization =
            "Bearer "
                + Jwts.builder()
                    .subject("current@test.com")
                    .signWith(Jwts.SIG.HS256.key().build(), Jwts.SIG.HS256)
                    .compact();
      }
      mockMvc
          .perform(get("/api/auth/me").header("Authorization", authorization))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      verifyNoInteractions(authenticationService);
    }

    @ParameterizedTest
    @DisplayName("Rechaza un sujeto firmado sin identidad persistida")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "missing@test.com"})
    void rechazaIdentidadInexistenteAntesDelController(String subject) throws Exception {
      String token =
          Jwts.builder()
              .subject(subject)
              .expiration(Date.from(Instant.now().plusSeconds(300)))
              .signWith(jwtSecretKey, Jwts.SIG.HS256)
              .compact();
      // JJWT omite el claim cuando recibe una cadena vacía o en blanco.
      String expectedSubject = subject == null || subject.isBlank() ? null : subject;
      when(authenticationService.getCurrentUser(expectedSubject))
          .thenThrow(new CurrentUserNotFoundException());

      mockMvc
          .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      verify(authenticationService).getCurrentUser(expectedSubject);
      verifyNoMoreInteractions(authenticationService);
    }

    @Test
    @DisplayName("Devuelve la identidad persistida, no los claims obsoletos del token")
    void devuelveSoloLaIdentidadPersistidaUsandoElSujeto() throws Exception {
      String token =
          Jwts.builder()
              .subject("CURRENT@TEST.COM")
              .claim("id", 999)
              .claim("email", "stale@test.com")
              .claim("role", "SUPERUSER")
              .signWith(jwtSecretKey, Jwts.SIG.HS256)
              .compact();
      when(authenticationService.getCurrentUser("CURRENT@TEST.COM"))
          .thenReturn(
              new User(42L, "current.user", "current@test.com", "password123", UserRole.USER));

      mockMvc
          .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(
              content()
                  .json(
                      "{\"id\":42,\"username\":\"current.user\",\"email\":\"current@test.com\",\"role\":\"USER\"}",
                      JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("Informa el rol persistido cuando el usuario es el dueño inicial de los tokens")
    void informaElRolPersistidoDelSuperuser() throws Exception {
      String token =
          Jwts.builder()
              .subject("owner@test.com")
              .expiration(Date.from(Instant.now().plusSeconds(300)))
              .signWith(jwtSecretKey, Jwts.SIG.HS256)
              .compact();
      when(authenticationService.getCurrentUser("owner@test.com"))
          .thenReturn(
              new User(7L, "market.owner", "owner@test.com", "password123", UserRole.SUPERUSER));

      mockMvc
          .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(
              content()
                  .json(
                      "{\"id\":7,\"username\":\"market.owner\",\"email\":\"owner@test.com\",\"role\":\"SUPERUSER\"}",
                      JsonCompareMode.STRICT));
    }
  }

  @Nested
  @DisplayName("Registro de usuarios")
  class Registration {
    @Test
    @DisplayName("Registra el usuario y transporta exactamente los datos de entrada")
    void registraUsuarioYMapeaElContratoDeEntrada() throws Exception {
      RegisterRequestDTO request =
          new RegisterRequestDTO("register.user", "register@test.com", "password");

      mockMvc
          .perform(
              post("/api/auth/register")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(content().string(""));

      ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
      verify(authenticationService).register(user.capture());
      assertThat(user.getValue().getUsername()).isEqualTo("register.user");
      assertThat(user.getValue().getEmail()).isEqualTo("register@test.com");
      assertThat(user.getValue().getPassword()).isEqualTo("password");
      assertThat(user.getValue().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("Ignora un rol enviado por el cliente y registra con el rol USER")
    void ignoraElRolEnviadoPorElCliente() throws Exception {
      String body =
          "{\"username\":\"register.user\",\"email\":\"register@test.com\",\"password\":\"password\",\"role\":\"SUPERUSER\"}";

      mockMvc
          .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isCreated())
          .andExpect(content().string(""));

      ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
      verify(authenticationService).register(user.capture());
      assertThat(user.getValue().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("Normaliza el nombre de usuario a minúsculas en el usuario transportado")
    void normalizaElNombreDeUsuarioEnElContratoDeEntrada() throws Exception {
      RegisterRequestDTO request =
          new RegisterRequestDTO("JUAN.PEREZ", "register@test.com", "password");

      mockMvc
          .perform(
              post("/api/auth/register")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(content().string(""));

      ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
      verify(authenticationService).register(user.capture());
      assertThat(user.getValue().getUsername()).isEqualTo("juan.perez");
    }

    @Test
    @DisplayName("Traduce el email duplicado a un conflicto HTTP contractual")
    void traduceEmailDuplicadoAlContratoConflict() throws Exception {
      RegisterRequestDTO request =
          new RegisterRequestDTO("duplicate.user", "duplicate@test.com", "password");
      doThrow(new EmailAlreadyRegisteredException("El email ya esta registrado."))
          .when(authenticationService)
          .register(any(User.class));

      mockMvc
          .perform(
              post("/api/auth/register")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.*").value(hasSize(6)))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.status").value(409))
          .andExpect(jsonPath("$.error").value("Conflict"))
          .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
          .andExpect(jsonPath("$.message").value("El email ya esta registrado."))
          .andExpect(jsonPath("$.path").value("/api/auth/register"));

      verify(authenticationService).register(any(User.class));
    }

    @Test
    @DisplayName("Traduce el nombre de usuario en uso a un conflicto HTTP contractual")
    void traduceNombreDeUsuarioDuplicadoAlContratoConflict() throws Exception {
      RegisterRequestDTO request =
          new RegisterRequestDTO("taken.user", "free@test.com", "password");
      doThrow(new UsernameAlreadyRegisteredException("El nombre de usuario ya esta en uso."))
          .when(authenticationService)
          .register(any(User.class));

      mockMvc
          .perform(
              post("/api/auth/register")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.*").value(hasSize(6)))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.status").value(409))
          .andExpect(jsonPath("$.error").value("Conflict"))
          .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_TAKEN"))
          .andExpect(jsonPath("$.message").value("El nombre de usuario ya esta en uso."))
          .andExpect(jsonPath("$.path").value("/api/auth/register"));

      verify(authenticationService).register(any(User.class));
    }
  }

  @Nested
  @DisplayName("Inicio de sesión")
  class Login {
    @Test
    @DisplayName("Inicia sesión y devuelve únicamente el token contractual")
    void iniciaSesionYDevuelveElContratoExacto() throws Exception {
      LoginRequestDTO request = new LoginRequestDTO("login@test.com", "password");
      when(authenticationService.login("login@test.com", "password")).thenReturn("jwt-token");

      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(content().json("{\"token\":\"jwt-token\"}", JsonCompareMode.STRICT));

      verify(authenticationService).login("login@test.com", "password");
    }

    @Test
    @DisplayName("Conserva el email original al invocar el caso de uso")
    void conservaElEmailDelContratoAlInvocarElCasoDeUso() throws Exception {
      LoginRequestDTO request = new LoginRequestDTO("LOGIN@TEST.COM", "password");
      when(authenticationService.login("LOGIN@TEST.COM", "password")).thenReturn("jwt-token");

      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(content().json("{\"token\":\"jwt-token\"}", JsonCompareMode.STRICT));

      verify(authenticationService).login("LOGIN@TEST.COM", "password");
    }

    @Test
    @DisplayName("Traduce credenciales inválidas a una respuesta no autorizada")
    void traduceCredencialesInvalidasAlContratoUnauthorized() throws Exception {
      LoginRequestDTO request = new LoginRequestDTO("unknown@test.com", "password");
      when(authenticationService.login("unknown@test.com", "password"))
          .thenThrow(new InvalidCredentialsException("Credenciales invalidas."));

      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.*").value(hasSize(6)))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(jsonPath("$.error").value("Unauthorized"))
          .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
          .andExpect(jsonPath("$.message").value("Credenciales invalidas."))
          .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }
  }

  @Nested
  @DisplayName("Validación de solicitudes de autenticación")
  class RequestValidation {
    private static Stream<Arguments> invalidRequests() {
      return Stream.of(
          Arguments.of(
              "/api/auth/register",
              "{\"username\":\"register.user\",\"email\":\"invalid\",\"password\":\"password\"}",
              "El email debe ser válido",
              "auth-register-invalid-email",
              "registro con email inválido",
              true),
          Arguments.of(
              "/api/auth/register",
              "{\"username\":\"register.user\",\"email\":\"\",\"password\":\"password\"}",
              "El email es obligatorio",
              "auth-register-empty-email",
              "registro con email vacío",
              true),
          Arguments.of(
              "/api/auth/register",
              "{\"username\":null,\"email\":\"user@test.com\",\"password\":\"password\"}",
              "El nombre de usuario es obligatorio",
              "auth-register-missing-username",
              "registro sin nombre de usuario",
              true),
          Arguments.of(
              "/api/auth/register",
              "{\"username\":\"ab\",\"email\":\"user@test.com\",\"password\":\"password\"}",
              "El nombre de usuario debe tener entre 3 y 30 caracteres",
              "auth-register-short-username",
              "registro con nombre de usuario corto",
              true),
          Arguments.of(
              "/api/auth/register",
              "{\"username\":\"user name\",\"email\":\"user@test.com\",\"password\":\"password\"}",
              "El nombre de usuario solo admite letras, números, punto, guion y guion bajo",
              "auth-register-invalid-username",
              "registro con nombre de usuario inválido",
              true),
          Arguments.of(
              "/api/auth/register",
              "{\"username\":\"register.user\",\"email\":\"user@test.com\",\"password\":null}",
              "La contraseña es obligatoria",
              "auth-register-empty-password",
              "registro sin contraseña",
              true),
          Arguments.of(
              "/api/auth/register",
              "{\"username\":\"register.user\",\"email\":\"user@test.com\",\"password\":\"1234567\"}",
              "La contraseña debe tener al menos 8 caracteres",
              "auth-register-short-password",
              "registro con contraseña corta",
              true),
          Arguments.of(
              "/api/auth/login",
              "{\"email\":\"invalid\",\"password\":\"password\"}",
              "El email debe ser válido",
              "auth-login-invalid-email",
              "login con email inválido",
              false),
          Arguments.of(
              "/api/auth/login",
              "{\"email\":\"\",\"password\":\"password\"}",
              "El email es obligatorio",
              "auth-login-empty-email",
              "login con email vacío",
              false),
          Arguments.of(
              "/api/auth/login",
              "{\"email\":\"user@test.com\",\"password\":\"\"}",
              "La contraseña es obligatoria",
              "auth-login-empty-password",
              "login con contraseña vacía",
              false));
    }

    @ParameterizedTest(name = "{4}")
    @DisplayName("Rechaza solicitudes inválidas antes de invocar el caso de uso")
    @MethodSource("invalidRequests")
    void rechazaEntradaInvalidaAntesDelCasoDeUso(
        String path,
        String body,
        String message,
        String snippet,
        String description,
        boolean registro)
        throws Exception {
      mockMvc
          .perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.*").value(hasSize(6)))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
          .andExpect(jsonPath("$.message").value(message))
          .andExpect(jsonPath("$.path").value(path));

      verifyNoInteractions(authenticationService);
    }

    /**
     * Un nombre de usuario vacío viola varias restricciones a la vez, porque queda sin contenido la
     * obligatoriedad, la longitud y el formato. El contrato garantiza que el mensaje contenga el
     * primer mensaje de validación disponible, no cuál de ellos es ese, por lo que este caso no
     * fija un mensaje concreto.
     */
    @Test
    @DisplayName("Rechaza un nombre de usuario vacío antes de invocar el caso de uso")
    void rechazaNombreDeUsuarioVacio() throws Exception {
      // Arrange
      String path = "/api/auth/register";
      String body = "{\"username\":\"\",\"email\":\"user@test.com\",\"password\":\"password\"}";

      // Act
      mockMvc
          .perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.*").value(hasSize(6)))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
          .andExpect(
              jsonPath("$.message")
                  .value(
                      anyOf(
                          is("El nombre de usuario es obligatorio"),
                          is("El nombre de usuario debe tener entre 3 y 30 caracteres"),
                          is(
                              "El nombre de usuario solo admite letras, números, punto, guion y guion bajo"))))
          .andExpect(jsonPath("$.path").value(path));

      // Assert
      verifyNoInteractions(authenticationService);
    }
  }
}
