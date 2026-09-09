package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class UserTest {

  @Test
  void shouldCreateUser() {
    User user = User.builder().email("test@test.com").password("password").build();

    assertThat(user.getEmail()).isEqualTo("test@test.com");
    assertThat(user.getPassword()).isEqualTo("password");
  }
}
