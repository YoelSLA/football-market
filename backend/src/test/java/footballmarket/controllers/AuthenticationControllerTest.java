package footballmarket.controllers;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
class AuthenticationControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserService userService;

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
        .andExpect(status().isOk())
        .andDo(
            document(
                "auth-register",
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
        .andExpect(status().isBadRequest());
  }

  @Test
  void deberiaRechazarRegistroCuandoElEmailEstaVacio() throws Exception {
    RegisterRequestDTO request = new RegisterRequestDTO("", "password");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deberiaRechazarRegistroCuandoLaContrasenaEstaVacia() throws Exception {
    RegisterRequestDTO request = new RegisterRequestDTO("empty-password@test.com", "");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deberiaRechazarRegistroCuandoLaContrasenaEsMuyCorta() throws Exception {
    RegisterRequestDTO request = new RegisterRequestDTO("short-password@test.com", "1234567");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deberiaRechazarRegistroCuandoElEmailYaExiste() throws Exception {
    RegisterRequestDTO firstRequest = new RegisterRequestDTO("duplicate@test.com", "password");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
        .andExpect(status().isOk());

    RegisterRequestDTO secondRequest =
        new RegisterRequestDTO("DUPLICATE@TEST.COM", "anotherPassword");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
        .andExpect(status().isConflict());
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
        .andExpect(status().isOk());

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
                "auth-login",
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
        .andExpect(status().isOk());

    LoginRequestDTO loginRequest = new LoginRequestDTO("UPPERCASE-CONTROLLER@TEST.COM", "password");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void deberiaRechazarLoginCuandoElEmailNoExiste() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("nonexistent-controller@test.com", "password");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
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
        .andExpect(status().isOk());

    LoginRequestDTO loginRequest =
        new LoginRequestDTO("wrong-password-controller@test.com", "wrongPassword");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deberiaRechazarLoginCuandoElEmailEsInvalido() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("email-invalido", "password");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deberiaRechazarLoginCuandoElEmailEstaVacio() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("", "password");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deberiaRechazarLoginCuandoLaContrasenaEstaVacia() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("login@test.com", "");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ============================================================
  // CLEANUP
  // ============================================================

  @BeforeEach
  void deleteAll() {
    userService.deteleAllUsers();
  }
}
