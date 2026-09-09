package footballmarket.controllers.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import footballmarket.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthService authService;

  @Autowired private ObjectMapper objectMapper;

  //    @Test
  //    @Disabled
  //    void shouldRegisterUser() throws Exception {
  //        RegisterRequest request = new RegisterRequest();
  //        request.setEmail("test@test.com");
  //        request.setPassword("password");
  //
  //        RegisterResponse response = RegisterResponse.builder()
  //                .id(1L)
  //                .email("test@test.com")
  //                .build();
  //
  //        when(authService.register(any())).thenReturn(response);
  //
  //        mockMvc.perform(post("/api/auth/register")
  //                .contentType(MediaType.APPLICATION_JSON)
  //                .content(objectMapper.writeValueAsString(request)))
  //                .andExpect(status().isCreated())
  //                .andExpect(jsonPath("$.id").value(1L))
  //                .andExpect(jsonPath("$.email").value("test@test.com"));
  //    }
  //
  //    @Test
  //    @Disabled
  //    void shouldLoginSuccessfully() throws Exception {
  //        LoginRequest request = new LoginRequest();
  //        request.setEmail("test@test.com");
  //        request.setPassword("password");
  //
  //        LoginResponse response = LoginResponse.builder()
  //                .token("token")
  //                .build();
  //
  //        when(authService.login(any())).thenReturn(response);
  //
  //        mockMvc.perform(post("/api/auth/login")
  //                .contentType(MediaType.APPLICATION_JSON)
  //                .content(objectMapper.writeValueAsString(request)))
  //                .andExpect(status().isOk())
  //                .andExpect(jsonPath("$.token").value("token"));
  //    }
}
