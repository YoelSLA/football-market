package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.exceptions.EmailEmptyException;
import footballmarket.models.exceptions.EmailInvalidException;
import footballmarket.models.exceptions.EmptyPasswordException;
import footballmarket.models.exceptions.PasswordTooShortException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

  @Nested
  @DisplayName("Creación de usuarios")
  class Creation {
    @Test
    void creacionDeUsuarioExitosa() {
      User user1 = new User(1L, "test1@test.com", "password1");
      User user2 = new User("test2@test.com", "password2");

      assertThat(user1.getEmail()).isEqualTo("test1@test.com");
      assertThat(user1.getPassword()).isEqualTo("password1");
      assertThat(user2.getEmail()).isEqualTo("test2@test.com");
      assertThat(user2.getPassword()).isEqualTo("password2");
    }

    @Test
    void constructorLanzaExcepcionCuandoEmailEsVacio() {
      assertThatThrownBy(() -> new User("", "password")).isInstanceOf(EmailEmptyException.class);
    }

    @Test
    void constructorLanzaExcepcionCuandoEmailEsInvalido() {
      assertThatThrownBy(() -> new User("test.com", "password"))
          .isInstanceOf(EmailInvalidException.class);
    }

    @Test
    void constructorLanzaExcepcionCuandoPasswordEsVacia() {
      assertThatThrownBy(() -> new User("test@test.com", ""))
          .isInstanceOf(EmptyPasswordException.class);
    }

    @Test
    void constructorLanzaExcepcionCuandoPasswordEsMuyCorta() {
      assertThatThrownBy(() -> new User("test@test.com", "pass"))
          .isInstanceOf(PasswordTooShortException.class);
    }
  }

  @Nested
  @DisplayName("Cambio de email")
  class EmailUpdate {
    @Test
    void modificarMailExitoso() {
      User user = new User("test@test.com", "password");

      user.setEmail("new@test.com");

      assertThat(user.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    void setEmailLanzaExcepcionCuandoEmailEsVacio() {
      User user = new User("test@test.com", "password");

      assertThatThrownBy(() -> user.setEmail("")).isInstanceOf(EmailEmptyException.class);
    }

    @Test
    void setEmailLanzaExcepcionCuandoEmailEsInvalido() {
      User user = new User("test@test.com", "password");

      assertThatThrownBy(() -> user.setEmail("test.com")).isInstanceOf(EmailInvalidException.class);
    }
  }

  @Nested
  @DisplayName("Cambio de contraseña")
  class PasswordUpdate {
    @Test
    void modificarPasswordExitoso() {
      User user = new User("test@test.com", "password");

      user.setPassword("newPassword");

      assertThat(user.getPassword()).isEqualTo("newPassword");
    }

    @Test
    void setPasswordLanzaExcepcionCuandoPasswordEsVacia() {
      User user = new User("test@test.com", "password");

      assertThatThrownBy(() -> user.setPassword("")).isInstanceOf(EmptyPasswordException.class);
    }

    @Test
    void setPasswordLanzaExcepcionCuandoPasswordEsMuyCorta() {
      User user = new User("test@test.com", "password");

      assertThatThrownBy(() -> user.setPassword("pass"))
          .isInstanceOf(PasswordTooShortException.class);
    }
  }
}
