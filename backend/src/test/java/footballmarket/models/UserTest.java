package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class UserTest {

  @Test
  void deberiaCrearUsuario() {
    User user = new User();

    user.setEmail("test@test.com");
    user.setPassword("password");

    assertThat(user.getEmail()).isEqualTo("test@test.com");
    assertThat(user.getPassword()).isEqualTo("password");
  }

  @Test
  void deberiaModificarEmail() {
    User user = new User();

    user.setEmail("test@test.com");
    user.setEmail("new@test.com");

    assertThat(user.getEmail()).isEqualTo("new@test.com");
  }

  @Test
  void deberiaModificarPassword() {
    User user = new User();

    user.setPassword("password");
    user.setPassword("newPassword");

    assertThat(user.getPassword()).isEqualTo("newPassword");
  }
}
