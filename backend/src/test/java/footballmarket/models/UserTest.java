package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.enums.UserRole;
import footballmarket.models.exceptions.EmailEmptyException;
import footballmarket.models.exceptions.EmailInvalidException;
import footballmarket.models.exceptions.EmptyPasswordException;
import footballmarket.models.exceptions.PasswordTooShortException;
import footballmarket.models.exceptions.UsernameEmptyException;
import footballmarket.models.exceptions.UsernameInvalidException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {

  @Nested
  @DisplayName("Creación de usuarios")
  class Creation {
    @Test
    @DisplayName("Construye usuarios con nombre de usuario, email y contraseña válidos")
    void creacionDeUsuarioExitosa() {
      User user1 = new User("juan.perez", "test1@test.com", "password1");
      User user2 = new User(1L, "maria.gomez", "test2@test.com", "password2", UserRole.USER);

      assertThat(user1.getUsername()).isEqualTo("juan.perez");
      assertThat(user1.getEmail()).isEqualTo("test1@test.com");
      assertThat(user1.getPassword()).isEqualTo("password1");
      assertThat(user2.getUsername()).isEqualTo("maria.gomez");
      assertThat(user2.getEmail()).isEqualTo("test2@test.com");
      assertThat(user2.getPassword()).isEqualTo("password2");
    }

    @ParameterizedTest
    @DisplayName("Rechaza la creación sin nombre de usuario")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void rechazaNombreDeUsuarioVacio(String username) {
      assertThatThrownBy(() -> new User(username, "test@test.com", "password"))
          .isInstanceOf(UsernameEmptyException.class);
    }

    @ParameterizedTest
    @DisplayName("Rechaza nombres de usuario con formato no admitido")
    @ValueSource(
        strings = {"juan perez", "juan@perez", "juán.pérez", "juan_perez!", "user+tag", "user#1"})
    void rechazaNombreDeUsuarioConFormatoInvalido(String username) {
      assertThatThrownBy(() -> new User(username, "test@test.com", "password"))
          .isInstanceOf(UsernameInvalidException.class);
    }

    @ParameterizedTest
    @DisplayName("Rechaza nombres de usuario por debajo de la longitud mínima")
    @ValueSource(strings = {"ab", "1"})
    void rechazaNombreDeUsuarioDemasiadoCorto(String username) {
      assertThatThrownBy(() -> new User(username, "test@test.com", "password"))
          .isInstanceOf(UsernameInvalidException.class);
    }

    @Test
    @DisplayName("Rechaza nombres de usuario por encima de la longitud máxima")
    void rechazaNombreDeUsuarioDemasiadoLargo() {
      String username = "a".repeat(31);

      assertThatThrownBy(() -> new User(username, "test@test.com", "password"))
          .isInstanceOf(UsernameInvalidException.class);
    }

    @Test
    @DisplayName("Acepta nombres de usuario en los límites de longitud admitidos")
    void aceptaNombresDeUsuarioEnLosLimitesDeLongitud() {
      User minimo = new User("abc", "minimo@test.com", "password");
      User maximo = new User("a".repeat(30), "maximo@test.com", "password");

      assertThat(minimo.getUsername()).isEqualTo("abc");
      assertThat(maximo.getUsername()).hasSize(30);
    }

    @Test
    @DisplayName("Rechaza la creación con email vacío")
    void rechazaEmailVacio() {
      assertThatThrownBy(() -> new User("juan.perez", "", "password"))
          .isInstanceOf(EmailEmptyException.class);
    }

    @Test
    @DisplayName("Rechaza la creación con email mal formado")
    void rechazaEmailInvalido() {
      assertThatThrownBy(() -> new User("juan.perez", "test.com", "password"))
          .isInstanceOf(EmailInvalidException.class);
    }

    @Test
    @DisplayName("Rechaza la creación con contraseña vacía")
    void rechazaPasswordVacia() {
      assertThatThrownBy(() -> new User("juan.perez", "test@test.com", ""))
          .isInstanceOf(EmptyPasswordException.class);
    }

    @Test
    @DisplayName("Rechaza la creación con contraseña demasiado corta")
    void rechazaPasswordMuyCorta() {
      assertThatThrownBy(() -> new User("juan.perez", "test@test.com", "pass"))
          .isInstanceOf(PasswordTooShortException.class);
    }

    @Test
    @DisplayName("Conserva el email vigente cuando un cambio posterior es rechazado")
    void conservaElEmailAnteUnCambioRechazado() {
      User user = new User("juan.perez", "test@test.com", "password");

      assertThatThrownBy(() -> user.setEmail("")).isInstanceOf(EmailEmptyException.class);
      assertThat(user.getEmail()).isEqualTo("test@test.com");
    }
  }

  @Nested
  @DisplayName("Normalización del nombre de usuario")
  class UsernameNormalization {
    @Test
    @DisplayName("Normaliza el nombre de usuario a minúsculas al construir el usuario")
    void normalizaElNombreDeUsuarioAMinusculas() {
      User user = new User("Juan.PEREZ", "test@test.com", "password");

      assertThat(user.getUsername()).isEqualTo("juan.perez");
    }

    @Test
    @DisplayName("Admite un nombre de usuario escrito en mayúsculas")
    void admiteNombreDeUsuarioEnMayusculas() {
      User user = new User("JUANPEREZ", "test@test.com", "password");

      assertThat(user.getUsername()).isEqualTo("juanperez");
    }
  }

  @Nested
  @DisplayName("Rol del usuario")
  class Role {
    @Test
    @DisplayName("Asigna el rol USER a todo usuario construido para el registro público")
    void asignaRolUserPorDefecto() {
      User user = new User("juan.perez", "test@test.com", "password");

      assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("Conserva el rol indicado en la construcción interna de una cuenta con capacidad")
    void conservaElRolIndicado() {
      User superuser =
          new User(null, "market.owner", "owner@test.com", "password", UserRole.SUPERUSER);

      assertThat(superuser.getRole()).isEqualTo(UserRole.SUPERUSER);
    }
  }

  @Nested
  @DisplayName("Cambio de email")
  class EmailUpdate {
    @Test
    @DisplayName("Actualiza el email de un usuario existente")
    void actualizaElEmail() {
      User user = new User("juan.perez", "test@test.com", "password");

      user.setEmail("new@test.com");

      assertThat(user.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    @DisplayName("Rechaza cambiar el email por uno vacío")
    void rechazaEmailVacioEnElCambio() {
      User user = new User("juan.perez", "test@test.com", "password");

      assertThatThrownBy(() -> user.setEmail("")).isInstanceOf(EmailEmptyException.class);
    }

    @Test
    @DisplayName("Rechaza cambiar el email por uno mal formado")
    void rechazaEmailInvalidoEnElCambio() {
      User user = new User("juan.perez", "test@test.com", "password");

      assertThatThrownBy(() -> user.setEmail("test.com")).isInstanceOf(EmailInvalidException.class);
    }
  }

  @Nested
  @DisplayName("Cambio de contraseña")
  class PasswordUpdate {
    @Test
    @DisplayName("Actualiza la contraseña de un usuario existente")
    void actualizaLaPassword() {
      User user = new User("juan.perez", "test@test.com", "password");

      user.setPassword("newPassword");

      assertThat(user.getPassword()).isEqualTo("newPassword");
    }

    @Test
    @DisplayName("Rechaza cambiar la contraseña por una vacía")
    void rechazaPasswordVaciaEnElCambio() {
      User user = new User("juan.perez", "test@test.com", "password");

      assertThatThrownBy(() -> user.setPassword("")).isInstanceOf(EmptyPasswordException.class);
    }

    @Test
    @DisplayName("Rechaza cambiar la contraseña por una demasiado corta")
    void rechazaPasswordMuyCortaEnElCambio() {
      User user = new User("juan.perez", "test@test.com", "password");

      assertThatThrownBy(() -> user.setPassword("pass"))
          .isInstanceOf(PasswordTooShortException.class);
    }
  }
}
