package footballmarket.integration.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import footballmarket.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class RegistrationIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private ObjectMapper objectMapper;

  //    @Test
  //    @Disabled
  //    void shouldRegisterUserSuccessfully() throws Exception {
  //        RegisterRequest request = new RegisterRequest();
  //        request.setEmail("newuser@test.com");
  //        request.setPassword("password123");
  //
  //        mockMvc.perform(post("/api/auth/register")
  //                .contentType(MediaType.APPLICATION_JSON)
  //                .content(objectMapper.writeValueAsString(request)))
  //                .andExpect(status().isCreated())
  //                .andExpect(jsonPath("$.email").value("newuser@test.com"));
  //
  //        assertThat(userRepository.existsByEmail("newuser@test.com")).isTrue();
  //    }
}
