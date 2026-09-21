package footballmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.controllers.dtos.requests.LoginRequestDTO;
import footballmarket.controllers.dtos.requests.RegisterRequestDTO;
import footballmarket.controllers.dtos.responses.CurrentUserResponseDTO;
import footballmarket.controllers.dtos.responses.LoginResponseDTO;
import footballmarket.controllers.dtos.responses.PlayersPageResponseDTO;
import footballmarket.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthenticationJourneyE2ETest {

  @LocalServerPort private int port;

  private RestClient restClient;

  @BeforeEach
  void setUp() {
    restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
  }

  @Nested
  @DisplayName("Registro, inicio de sesión y acceso protegido")
  class AuthenticationJourney {
    @Test
    void registraIniciaSesionYAccedeAlCatalogoProtegido() {
      RegisterRequestDTO registerRequest =
          new RegisterRequestDTO("critical-journey@test.com", "password123");

      ResponseEntity<Void> register =
          restClient
              .post()
              .uri("/api/auth/register")
              .body(registerRequest)
              .retrieve()
              .toBodilessEntity();

      assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(register.hasBody()).isFalse();

      LoginRequestDTO loginRequest =
          new LoginRequestDTO("critical-journey@test.com", "password123");
      ResponseEntity<LoginResponseDTO> login =
          restClient
              .post()
              .uri("/api/auth/login")
              .body(loginRequest)
              .retrieve()
              .toEntity(LoginResponseDTO.class);

      assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(login.getBody()).isNotNull();
      String token = login.getBody().token();
      assertThat(token).isNotBlank();

      ResponseEntity<CurrentUserResponseDTO> current =
          restClient
              .get()
              .uri("/api/auth/me")
              .headers(headers -> headers.setBearerAuth(token))
              .retrieve()
              .toEntity(CurrentUserResponseDTO.class);

      assertThat(current.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(current.getBody()).isNotNull();
      assertThat(current.getBody().id()).isPositive();
      assertThat(current.getBody().email()).isEqualTo(registerRequest.email());

      ResponseEntity<PlayersPageResponseDTO> catalog =
          restClient
              .get()
              .uri("/api/players")
              .headers(headers -> headers.setBearerAuth(token))
              .retrieve()
              .toEntity(PlayersPageResponseDTO.class);

      assertThat(catalog.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(catalog.getBody()).isNotNull();
      assertThat(catalog.getBody().content()).isEmpty();
    }
  }
}
