package footballmarket.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.config.SecurityConfig;
import footballmarket.controllers.dtos.requests.LoginRequestDTO;
import footballmarket.controllers.dtos.requests.RegisterRequestDTO;
import footballmarket.models.User;
import footballmarket.services.AuthenticationService;
import footballmarket.services.exceptions.EmailAlreadyRegisteredException;
import footballmarket.services.exceptions.InvalidCredentialsException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
  @MockitoBean private AuthenticationService authenticationService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp(RestDocumentationContextProvider restDocumentation) {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(this.context)
            .addFilters(this.security)
            .apply(documentationConfiguration(restDocumentation))
            .build();
  }

  @Test
  void registraUsuarioYMapeaElContratoDeEntrada() throws Exception {
    RegisterRequestDTO request = new RegisterRequestDTO("register@test.com", "password");

    this.mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(content().string(""))
        .andDo(
            document(
                "auth-register-success",
                requestFields(
                    fieldWithPath("email").description("Email del usuario"),
                    fieldWithPath("password").description("Contraseña del usuario"))));

    ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
    verify(this.authenticationService).register(user.capture());
    assertThat(user.getValue().getEmail()).isEqualTo("register@test.com");
    assertThat(user.getValue().getPassword()).isEqualTo("password");
  }

  @Test
  void traduceEmailDuplicadoAlContratoConflict() throws Exception {
    RegisterRequestDTO request = new RegisterRequestDTO("duplicate@test.com", "password");
    doThrow(new EmailAlreadyRegisteredException("El email ya esta registrado."))
        .when(this.authenticationService)
        .register(any(User.class));

    this.mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.*").value(hasSize(5)))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.error").value("Conflict"))
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
                    fieldWithPath("message").description("Mensaje seguro"),
                    fieldWithPath("path").description("Ruta solicitada"))));

    verify(this.authenticationService).register(any(User.class));
  }

  @Test
  void iniciaSesionYDevuelveElContratoExacto() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("login@test.com", "password");
    when(this.authenticationService.login("login@test.com", "password")).thenReturn("jwt-token");

    this.mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"token\":\"jwt-token\"}", JsonCompareMode.STRICT))
        .andDo(
            document(
                "auth-login-success",
                requestFields(
                    fieldWithPath("email").description("Email del usuario"),
                    fieldWithPath("password").description("Contraseña del usuario")),
                responseFields(fieldWithPath("token").description("Token JWT de autenticación"))));

    verify(this.authenticationService).login("login@test.com", "password");
  }

  @Test
  void conservaElEmailDelContratoAlInvocarElCasoDeUso() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("LOGIN@TEST.COM", "password");
    when(this.authenticationService.login("LOGIN@TEST.COM", "password")).thenReturn("jwt-token");

    this.mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"token\":\"jwt-token\"}", JsonCompareMode.STRICT))
        .andDo(
            document(
                "auth-login-uppercase-email",
                requestFields(
                    fieldWithPath("email").description("Email ingresado con mayúsculas"),
                    fieldWithPath("password").description("Contraseña del usuario")),
                responseFields(fieldWithPath("token").description("Token JWT de autenticación"))));

    verify(this.authenticationService).login("LOGIN@TEST.COM", "password");
  }

  @Test
  void traduceCredencialesInvalidasAlContratoUnauthorized() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("unknown@test.com", "password");
    when(this.authenticationService.login("unknown@test.com", "password"))
        .thenThrow(new InvalidCredentialsException("Credenciales invalidas."));

    this.mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.*").value(hasSize(5)))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.error").value("Unauthorized"))
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
                    fieldWithPath("message").description("Mensaje seguro"),
                    fieldWithPath("path").description("Ruta solicitada"))));
  }

  @ParameterizedTest(name = "{4}")
  @MethodSource("invalidRequests")
  void rechazaEntradaInvalidaAntesDelCasoDeUso(
      String path, String body, String message, String snippet, String description)
      throws Exception {
    this.mockMvc
        .perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.*").value(hasSize(5)))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
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
                    fieldWithPath("message").description("Mensaje de validación"),
                    fieldWithPath("path").description("Ruta solicitada"))));

    verifyNoInteractions(this.authenticationService);
  }

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
}
