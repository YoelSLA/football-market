package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.User;
import footballmarket.services.exceptions.CurrentUserNotFoundException;
import footballmarket.services.exceptions.EmailAlreadyRegisteredException;
import footballmarket.services.exceptions.InvalidCredentialsException;
import footballmarket.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class AuthenticationServiceTest {

  @Autowired private AuthenticationService authenticationService;

  @Nested
  @DisplayName("Consulta del usuario actual")
  class CurrentUser {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "missing@test.com"})
    void rechazaSujetoSinUsuarioActual(String subject) {
      assertThatThrownBy(() -> authenticationService.getCurrentUser(subject))
          .isInstanceOf(CurrentUserNotFoundException.class);
    }

    @Test
    void recuperaElUsuarioPersistidoPorSujetoNormalizado() {
      User registered = new User("current@test.com", "password123");
      authenticationService.register(registered);

      User current = authenticationService.getCurrentUser("CURRENT@TEST.COM");

      assertThat(current.getId()).isNotNull().isEqualTo(registered.getId());
      assertThat(current.getEmail()).isEqualTo("current@test.com");
    }
  }

  @Nested
  @DisplayName("Registro de usuarios")
  class Registration {
    @Test
    void seRegistraUnUsuarioCorrectamente() {
      User user = new User("test@test.com", "password");

      authenticationService.register(user);

      String token = authenticationService.login("test@test.com", "password");

      assertThat(token).isNotBlank();
    }

    @Test
    void seNormalizaElEmailAlRegistrarUsuario() {
      User user = new User("test@test.com", "password");

      authenticationService.register(user);

      assertThat(user.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void deberiaHashearLaContrasenaAlRegistrarUsuario() {
      User user = new User();
      user.setEmail("hash@test.com");
      user.setPassword("password");

      authenticationService.register(user);

      assertThat(user.getPassword()).isNotEqualTo("password");
    }

    @Test
    void deberiaLanzarExcepcionCuandoElEmailYaExiste() {
      User firstUser = new User("duplicate@test.com", "password");

      authenticationService.register(firstUser);

      User secondUser = new User("DUPLICATE@TEST.COM", "anotherPassword");

      assertThatThrownBy(() -> authenticationService.register(secondUser))
          .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
  }

  @Nested
  @DisplayName("Inicio de sesión")
  class Login {
    @Test
    void deberiaPoderIniciarSesionConLaContrasenaOriginal() {
      User user = new User();
      user.setEmail("original@test.com");
      user.setPassword("password");

      authenticationService.register(user);

      String token = authenticationService.login("original@test.com", "password");

      assertThat(token).isNotBlank();
    }

    @Test
    void noDeberiaPoderIniciarSesionConLaContrasenaHasheada() {
      User user = new User("hash-login@test.com", "password");

      authenticationService.register(user);

      assertThatThrownBy(
              () -> authenticationService.login("hash-login@test.com", user.getPassword()))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void deberiaIniciarSesionCorrectamente() {
      User user = new User();
      user.setEmail("login@test.com");
      user.setPassword("password");

      authenticationService.register(user);

      String token = authenticationService.login("login@test.com", "password");

      assertThat(token).isNotBlank();
    }

    @Test
    void deberiaNormalizarEmailAlIniciarSesion() {
      User user = new User();
      user.setEmail("normalize@test.com");
      user.setPassword("password");

      authenticationService.register(user);

      String token = authenticationService.login("NORMALIZE@TEST.COM", "password");

      assertThat(token).isNotBlank();
    }

    @Test
    void deberiaLanzarExcepcionCuandoElUsuarioNoExiste() {
      assertThatThrownBy(() -> authenticationService.login("nonexistent@test.com", "password"))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void deberiaLanzarExcepcionCuandoLaContrasenaEsIncorrecta() {
      User user = new User("invalid@test.com", "password");

      authenticationService.register(user);

      assertThatThrownBy(() -> authenticationService.login("invalid@test.com", "wrongPassword"))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void deberiaGenerarUnTokenParaElUsuarioAutenticado() {
      User user = new User();
      user.setEmail("token@test.com");
      user.setPassword("password");

      authenticationService.register(user);

      String token = authenticationService.login("token@test.com", "password");

      assertThat(token).isNotNull().isNotBlank();
    }
  }
}
