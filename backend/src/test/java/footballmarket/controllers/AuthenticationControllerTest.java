package footballmarket.controllers;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.controllers.dtos.requests.LoginRequestDTO;
import footballmarket.controllers.dtos.requests.RegisterRequestDTO;
import footballmarket.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(RestDocumentationExtension.class)
class AuthenticationControllerTest {

  @Autowired private WebApplicationContext context;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserService userService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp(RestDocumentationContextProvider restDocumentation) {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(documentationConfiguration(restDocumentation))
            .build();

    userService.deteleAllUsers();
  }

  // ============================================================
  // REGISTER
  // ============================================================

  @Test
  void deberiaRegistrarUsuario() throws Exception {
    RegisterRequestDTO request = new RegisterRequestDTO("register@test.com", "password");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "auth-register-success",
                requestFields(
                    fieldWithPath("email").description("Email del usuario"),
                    fieldWithPath("password").description("Contraseña del usuario"))));
  }

  @Test
  void deberiaRechazarRegistroCuandoElEmailEsInvalido() throws Exception {
    RegisterRequestDTO request = new RegisterRequestDTO("email-invalido", "password");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("El email debe ser válido"))
        .andExpect(jsonPath("$.path").value("/api/auth/register"))
        .andDo(
            document(
                "auth-register-invalid-email",
                requestFields(
                    fieldWithPath("email")
                        .description("Email inválido que no cumple con el formato requerido"),
                    fieldWithPath("password").description("Contraseña del usuario"))));
  }

  @Test
  void deberiaRechazarRegistroCuandoElEmailEstaVacio() throws Exception {
    RegisterRequestDTO request = new RegisterRequestDTO("", "password");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andDo(
            document(
                "auth-register-empty-email",
                requestFields(
                    fieldWithPath("email").description("Email vacío"),
                    fieldWithPath("password").description("Contraseña del usuario"))));
  }

  @Test
  void deberiaRechazarRegistroCuandoLaContrasenaEstaVacia() throws Exception {
    RegisterRequestDTO request = new RegisterRequestDTO("empty-password@test.com", "");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andDo(
            document(
                "auth-register-empty-password",
                requestFields(
                    fieldWithPath("email").description("Email del usuario"),
                    fieldWithPath("password").description("Contraseña vacía"))));
  }

  @Test
  void deberiaRechazarRegistroCuandoLaContrasenaEsMuyCorta() throws Exception {
    RegisterRequestDTO request = new RegisterRequestDTO("short-password@test.com", "1234567");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andDo(
            document(
                "auth-register-short-password",
                requestFields(
                    fieldWithPath("email").description("Email del usuario"),
                    fieldWithPath("password")
                        .description(
                            "Contraseña que no cumple con la longitud mínima requerida"))));
  }

  @Test
  void deberiaRechazarRegistroCuandoElEmailYaExiste() throws Exception {
    RegisterRequestDTO firstRequest = new RegisterRequestDTO("duplicate@test.com", "password");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
        .andExpect(status().isCreated());

    RegisterRequestDTO secondRequest =
        new RegisterRequestDTO("DUPLICATE@TEST.COM", "anotherPassword");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.error").value("Conflict"))
        .andExpect(jsonPath("$.message").value("El email ya esta registrado."))
        .andExpect(jsonPath("$.path").value("/api/auth/register"))
        .andDo(
            document(
                "auth-register-duplicate-email",
                requestFields(
                    fieldWithPath("email")
                        .description(
                            "Email ya registrado, independientemente de mayúsculas y minúsculas"),
                    fieldWithPath("password").description("Contraseña del usuario"))));
  }

  // ============================================================
  // LOGIN
  // ============================================================

  @Test
  void deberiaIniciarSesionCorrectamente() throws Exception {
    RegisterRequestDTO registerRequest =
        new RegisterRequestDTO("login-controller@test.com", "password");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isCreated());

    LoginRequestDTO loginRequest = new LoginRequestDTO("login-controller@test.com", "password");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andDo(
            document(
                "auth-login-success",
                requestFields(
                    fieldWithPath("email").description("Email del usuario"),
                    fieldWithPath("password").description("Contraseña del usuario")),
                responseFields(
                    fieldWithPath("token")
                        .description("Token JWT utilizado para autenticar las solicitudes"))));
  }

  @Test
  void deberiaPermitirIniciarSesionConEmailEnMayusculas() throws Exception {
    RegisterRequestDTO registerRequest =
        new RegisterRequestDTO("uppercase-controller@test.com", "password");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isCreated());

    LoginRequestDTO loginRequest = new LoginRequestDTO("UPPERCASE-CONTROLLER@TEST.COM", "password");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andDo(
            document(
                "auth-login-uppercase-email",
                requestFields(
                    fieldWithPath("email")
                        .description("Email del usuario ingresado utilizando mayúsculas"),
                    fieldWithPath("password").description("Contraseña del usuario")),
                responseFields(
                    fieldWithPath("token")
                        .description("Token JWT utilizado para autenticar las solicitudes"))));
  }

  @Test
  void deberiaRechazarLoginCuandoElEmailNoExiste() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("nonexistent-controller@test.com", "password");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andDo(
            document(
                "auth-login-nonexistent-email",
                requestFields(
                    fieldWithPath("email").description("Email no registrado en el sistema"),
                    fieldWithPath("password").description("Contraseña proporcionada"))));
  }

  @Test
  void deberiaRechazarLoginCuandoLaContrasenaEsIncorrecta() throws Exception {
    RegisterRequestDTO registerRequest =
        new RegisterRequestDTO("wrong-password-controller@test.com", "password");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isCreated());

    LoginRequestDTO loginRequest =
        new LoginRequestDTO("wrong-password-controller@test.com", "wrongPassword");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized())
        .andDo(
            document(
                "auth-login-wrong-password",
                requestFields(
                    fieldWithPath("email").description("Email de un usuario registrado"),
                    fieldWithPath("password").description("Contraseña incorrecta"))));
  }

  @Test
  void deberiaRechazarLoginCuandoElEmailEsInvalido() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("email-invalido", "password");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andDo(
            document(
                "auth-login-invalid-email",
                requestFields(
                    fieldWithPath("email")
                        .description("Email inválido que no cumple con el formato requerido"),
                    fieldWithPath("password").description("Contraseña del usuario"))));
  }

  @Test
  void deberiaRechazarLoginCuandoElEmailEstaVacio() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("", "password");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andDo(
            document(
                "auth-login-empty-email",
                requestFields(
                    fieldWithPath("email").description("Email vacío"),
                    fieldWithPath("password").description("Contraseña del usuario"))));
  }

  @Test
  void deberiaRechazarLoginCuandoLaContrasenaEstaVacia() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("login@test.com", "");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andDo(
            document(
                "auth-login-empty-password",
                requestFields(
                    fieldWithPath("email").description("Email del usuario"),
                    fieldWithPath("password").description("Contraseña vacía"))));
  }
}
