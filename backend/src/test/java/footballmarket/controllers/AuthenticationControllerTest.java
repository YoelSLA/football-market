package footballmarket.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.config.SecurityConfig;
import footballmarket.controllers.dtos.requests.LoginRequestDTO;
import footballmarket.controllers.dtos.requests.RegisterRequestDTO;
import footballmarket.models.User;
import footballmarket.services.AuthenticationService;
import footballmarket.services.exceptions.CurrentUserNotFoundException;
import footballmarket.services.exceptions.EmailAlreadyRegisteredException;
import footballmarket.services.exceptions.InvalidCredentialsException;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
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
@ExtendWith(RestDocumentationExtension.class)
class AuthenticationControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey jwtSecretKey;
  @MockitoBean private AuthenticationService authenticationService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp(RestDocumentationContextProvider restDocumentation) {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(security)
            .apply(documentationConfiguration(restDocumentation))
            .build();
  }

  @Nested
  @DisplayName("Consulta del usuario actual")
  class CurrentUser {
    @Test
    void rechazaConsultaSinTokenConCuerpoVacio() throws Exception {
      mockMvc
          .perform(get("/api/auth/me"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""))
          .andDo(document("auth-me-unauthorized"));
      verifyNoInteractions(authenticationService);
    }

    @ParameterizedTest
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
    void devuelveSoloLaIdentidadPersistidaUsandoElSujeto() throws Exception {
      String token =
          Jwts.builder()
              .subject("CURRENT@TEST.COM")
              .claim("id", 999)
              .claim("email", "stale@test.com")
              .signWith(jwtSecretKey, Jwts.SIG.HS256)
              .compact();
      when(authenticationService.getCurrentUser("CURRENT@TEST.COM"))
          .thenReturn(new User(42L, "current@test.com", "password123"));

      mockMvc
          .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(
              content().json("{\"id\":42,\"email\":\"current@test.com\"}", JsonCompareMode.STRICT))
          .andDo(
              document(
                  "auth-me-success",
                  responseFields(
                      fieldWithPath("id").description("Identificador del usuario persistido"),
                      fieldWithPath("email").description("Email actual del usuario persistido"))));
    }
  }

  @Nested
  @DisplayName("Registro de usuarios")
  class Registration {
    @Test
    void registraUsuarioYMapeaElContratoDeEntrada() throws Exception {
      RegisterRequestDTO request = new RegisterRequestDTO("register@test.com", "password");

      mockMvc
          .perform(
              post("/api/auth/register")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(content().string(""))
          .andDo(
              document(
                  "auth-register-success",
                  requestFields(
                      fieldWithPath("email").description("Email del usuario"),
                      fieldWithPath("password").description("Contraseña del usuario"))));

      ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
      verify(authenticationService).register(user.capture());
      assertThat(user.getValue().getEmail()).isEqualTo("register@test.com");
      assertThat(user.getValue().getPassword()).isEqualTo("password");
    }

    @Test
    void traduceEmailDuplicadoAlContratoConflict() throws Exception {
      RegisterRequestDTO request = new RegisterRequestDTO("duplicate@test.com", "password");
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
          .andExpect(jsonPath("$.path").value("/api/auth/register"))
          .andDo(
              document(
                  "auth-register-duplicate-email",
                  requestFields(
                      fieldWithPath("email").description("Email ya registrado"),
                      fieldWithPath("password").description("Contraseña del usuario")),
                  responseFields(
                      fieldWithPath("timestamp").description("Fecha del error"),
                      fieldWithPath("status").description("Código HTTP"),
                      fieldWithPath("error").description("Descripción HTTP"),
                      fieldWithPath("code").description("Código estable del error"),
                      fieldWithPath("message").description("Mensaje seguro"),
                      fieldWithPath("path").description("Ruta solicitada"))));

      verify(authenticationService).register(any(User.class));
    }
  }

  @Nested
  @DisplayName("Inicio de sesión")
  class Login {
    @Test
    void iniciaSesionYDevuelveElContratoExacto() throws Exception {
      LoginRequestDTO request = new LoginRequestDTO("login@test.com", "password");
      when(authenticationService.login("login@test.com", "password")).thenReturn("jwt-token");

      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(content().json("{\"token\":\"jwt-token\"}", JsonCompareMode.STRICT))
          .andDo(
              document(
                  "auth-login-success",
                  requestFields(
                      fieldWithPath("email").description("Email del usuario"),
                      fieldWithPath("password").description("Contraseña del usuario")),
                  responseFields(
                      fieldWithPath("token").description("Token JWT de autenticación"))));

      verify(authenticationService).login("login@test.com", "password");
    }

    @Test
    void conservaElEmailDelContratoAlInvocarElCasoDeUso() throws Exception {
      LoginRequestDTO request = new LoginRequestDTO("LOGIN@TEST.COM", "password");
      when(authenticationService.login("LOGIN@TEST.COM", "password")).thenReturn("jwt-token");

      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(content().json("{\"token\":\"jwt-token\"}", JsonCompareMode.STRICT))
          .andDo(
              document(
                  "auth-login-uppercase-email",
                  requestFields(
                      fieldWithPath("email").description("Email ingresado con mayúsculas"),
                      fieldWithPath("password").description("Contraseña del usuario")),
                  responseFields(
                      fieldWithPath("token").description("Token JWT de autenticación"))));

      verify(authenticationService).login("LOGIN@TEST.COM", "password");
    }

    @Test
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
          .andExpect(jsonPath("$.path").value("/api/auth/login"))
          .andDo(
              document(
                  "auth-login-invalid-credentials",
                  requestFields(
                      fieldWithPath("email").description("Email presentado"),
                      fieldWithPath("password").description("Contraseña presentada")),
                  responseFields(
                      fieldWithPath("timestamp").description("Fecha del error"),
                      fieldWithPath("status").description("Código HTTP"),
                      fieldWithPath("error").description("Descripción HTTP"),
                      fieldWithPath("code").description("Código estable del error"),
                      fieldWithPath("message").description("Mensaje seguro"),
                      fieldWithPath("path").description("Ruta solicitada"))));
    }
  }

  @Nested
  @DisplayName("Validación de solicitudes de autenticación")
  class RequestValidation {
    private static Stream<Arguments> invalidRequests() {
      return Stream.of(
          Arguments.of(
              "/api/auth/register",
              "{\"email\":\"invalid\",\"password\":\"password\"}",
              "El email debe ser válido",
              "auth-register-invalid-email",
              "registro con email inválido"),
          Arguments.of(
              "/api/auth/register",
              "{\"email\":\"\",\"password\":\"password\"}",
              "El email es obligatorio",
              "auth-register-empty-email",
              "registro con email vacío"),
          Arguments.of(
              "/api/auth/register",
              "{\"email\":\"user@test.com\",\"password\":null}",
              "La contraseña es obligatoria",
              "auth-register-empty-password",
              "registro sin contraseña"),
          Arguments.of(
              "/api/auth/register",
              "{\"email\":\"user@test.com\",\"password\":\"1234567\"}",
              "La contraseña debe tener al menos 8 caracteres",
              "auth-register-short-password",
              "registro con contraseña corta"),
          Arguments.of(
              "/api/auth/login",
              "{\"email\":\"invalid\",\"password\":\"password\"}",
              "El email debe ser válido",
              "auth-login-invalid-email",
              "login con email inválido"),
          Arguments.of(
              "/api/auth/login",
              "{\"email\":\"\",\"password\":\"password\"}",
              "El email es obligatorio",
              "auth-login-empty-email",
              "login con email vacío"),
          Arguments.of(
              "/api/auth/login",
              "{\"email\":\"user@test.com\",\"password\":\"\"}",
              "La contraseña es obligatoria",
              "auth-login-empty-password",
              "login con contraseña vacía"));
    }

    @ParameterizedTest(name = "{4}")
    @MethodSource("invalidRequests")
    void rechazaEntradaInvalidaAntesDelCasoDeUso(
        String path, String body, String message, String snippet, String description)
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
          .andExpect(jsonPath("$.path").value(path))
          .andDo(
              document(
                  snippet,
                  requestFields(
                      fieldWithPath("email").description("Email presentado"),
                      fieldWithPath("password").description("Contraseña presentada")),
                  responseFields(
                      fieldWithPath("timestamp").description("Fecha del error"),
                      fieldWithPath("status").description("Código HTTP"),
                      fieldWithPath("error").description("Descripción HTTP"),
                      fieldWithPath("code").description("Código estable del error"),
                      fieldWithPath("message").description("Mensaje de validación"),
                      fieldWithPath("path").description("Ruta solicitada"))));

      verifyNoInteractions(authenticationService);
    }
  }
}
