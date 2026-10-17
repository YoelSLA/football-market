package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.User;
import footballmarket.models.enums.UserRole;
import footballmarket.services.exceptions.CurrentUserNotFoundException;
import footballmarket.services.exceptions.EmailAlreadyRegisteredException;
import footballmarket.services.exceptions.InvalidCredentialsException;
import footballmarket.services.exceptions.UsernameAlreadyRegisteredException;
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
    @DisplayName("Rechaza sujetos sin un usuario actual persistido")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "missing@test.com"})
    void rechazaSujetoSinUsuarioActual(String subject) {
      assertThatThrownBy(() -> authenticationService.getCurrentUser(subject))
          .isInstanceOf(CurrentUserNotFoundException.class);
    }

    @Test
    @DisplayName("Recupera el usuario persistido aunque el sujeto cambie de mayúsculas")
    void recuperaElUsuarioPersistidoPorSujetoNormalizado() {
      User registered = new User("current.user", "current@test.com", "password123");
      authenticationService.register(registered);

      User current = authenticationService.getCurrentUser("CURRENT@TEST.COM");

      assertThat(current.getId()).isNotNull().isEqualTo(registered.getId());
      assertThat(current.getEmail()).isEqualTo("current@test.com");
    }

    @Test
    @DisplayName("Expone el nombre de usuario y el rol del usuario persistido")
    void exponeNombreDeUsuarioYRolPersistidos() {
      User registered = new User("identity.user", "identity@test.com", "password123");
      authenticationService.register(registered);

      User current = authenticationService.getCurrentUser("identity@test.com");

      assertThat(current.getUsername()).isEqualTo("identity.user");
      assertThat(current.getRole()).isEqualTo(UserRole.USER);
    }
  }

  @Nested
  @DisplayName("Registro de usuarios")
  class Registration {
    @Test
    @DisplayName("Registra un usuario que puede iniciar sesión con sus credenciales")
    void registraUnUsuarioQuePuedeIniciarSesion() {
      User user = new User("basic.user", "test@test.com", "password");

      authenticationService.register(user);

      String token = authenticationService.login("test@test.com", "password");

      assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Normaliza el email al registrar un usuario")
    void normalizaElEmailAlRegistrar() {
      User user = new User("basic.user", "TEST@TEST.COM", "password");

      authenticationService.register(user);

      assertThat(user.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("Normaliza el nombre de usuario a minúsculas al registrar un usuario")
    void normalizaElNombreDeUsuarioAlRegistrar() {
      User user = new User("JUAN.PEREZ", "juan@test.com", "password");

      authenticationService.register(user);

      assertThat(user.getUsername()).isEqualTo("juan.perez");
    }

    @Test
    @DisplayName("Asigna el rol USER a todo usuario registrado públicamente")
    void asignaRolUserAlRegistrar() {
      User user = new User("public.user", "public@test.com", "password");

      authenticationService.register(user);

      User registered = authenticationService.getCurrentUser("public@test.com");

      assertThat(registered.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("Al registrar un usuario se almacena la contraseña cifrada")
    void hasheaLaContrasenaAlRegistrar() {
      User user = new User("hash.user", "hash@test.com", "password");

      authenticationService.register(user);

      assertThat(user.getPassword()).isNotEqualTo("password");
    }

    @Test
    @DisplayName("Rechaza registrar un email que ya existe sin distinguir mayúsculas")
    void rechazaEmailDuplicado() {
      User firstUser = new User("duplicate.one", "duplicate@test.com", "password");
      authenticationService.register(firstUser);

      User secondUser = new User("duplicate.two", "DUPLICATE@TEST.COM", "anotherPassword");

      assertThatThrownBy(() -> authenticationService.register(secondUser))
          .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    @DisplayName("Rechaza registrar un nombre de usuario que ya existe")
    void rechazaNombreDeUsuarioDuplicado() {
      User firstUser = new User("taken.name", "first@test.com", "password");
      authenticationService.register(firstUser);

      User secondUser = new User("taken.name", "second@test.com", "anotherPassword");

      assertThatThrownBy(() -> authenticationService.register(secondUser))
          .isInstanceOf(UsernameAlreadyRegisteredException.class);
    }

    @Test
    @DisplayName("Rechaza registrar un nombre de usuario duplicado por diferencia de mayúsculas")
    void rechazaNombreDeUsuarioDuplicadoPorMayusculas() {
      User firstUser = new User("Taken.Name", "first@test.com", "password");
      authenticationService.register(firstUser);

      User secondUser = new User("TAKEN.NAME", "second@test.com", "anotherPassword");

      assertThatThrownBy(() -> authenticationService.register(secondUser))
          .isInstanceOf(UsernameAlreadyRegisteredException.class);
    }

    @Test
    @DisplayName("No crea el usuario cuando el nombre de usuario ya está en uso")
    void noCreaElUsuarioCuandoElNombreDeUsuarioEstaEnUso() {
      User firstUser = new User("unique.name", "first@test.com", "password");
      authenticationService.register(firstUser);

      User secondUser = new User("unique.name", "second@test.com", "anotherPassword");

      assertThatThrownBy(() -> authenticationService.register(secondUser))
          .isInstanceOf(UsernameAlreadyRegisteredException.class);

      assertThatThrownBy(() -> authenticationService.getCurrentUser("second@test.com"))
          .isInstanceOf(CurrentUserNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("Inicio de sesión")
  class Login {
    @Test
    @DisplayName("Permite iniciar sesión con la contraseña original")
    void permiteIniciarSesionConLaContrasenaOriginal() {
      User user = new User("original.user", "original@test.com", "password");
      authenticationService.register(user);

      String token = authenticationService.login("original@test.com", "password");

      assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Rechaza iniciar sesión con la contraseña almacenada cifrada")
    void rechazaLoginConLaContrasenaHasheada() {
      User user = new User("hash.login", "hash-login@test.com", "password");
      authenticationService.register(user);

      assertThatThrownBy(
              () -> authenticationService.login("hash-login@test.com", user.getPassword()))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Emite un token al iniciar sesión con credenciales válidas")
    void emiteTokenConCredencialesValidas() {
      User user = new User("login.user", "login@test.com", "password");
      authenticationService.register(user);

      String token = authenticationService.login("login@test.com", "password");

      assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Acepta un email en mayúsculas al iniciar sesión")
    void normalizaElEmailAlIniciarSesion() {
      User user = new User("normalize.user", "normalize@test.com", "password");
      authenticationService.register(user);

      String token = authenticationService.login("NORMALIZE@TEST.COM", "password");

      assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Rechaza iniciar sesión con el nombre de usuario en lugar del email")
    void rechazaLoginConNombreDeUsuario() {
      User user = new User("credential.user", "credential@test.com", "password");
      authenticationService.register(user);

      assertThatThrownBy(() -> authenticationService.login("credential.user", "password"))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Rechaza iniciar sesión cuando el usuario no existe")
    void rechazaLoginDeUsuarioInexistente() {
      assertThatThrownBy(() -> authenticationService.login("nonexistent@test.com", "password"))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Rechaza iniciar sesión con una contraseña incorrecta")
    void rechazaLoginConContrasenaIncorrecta() {
      User user = new User("invalid.user", "invalid@test.com", "password");
      authenticationService.register(user);

      assertThatThrownBy(() -> authenticationService.login("invalid@test.com", "wrongPassword"))
          .isInstanceOf(InvalidCredentialsException.class);
    }
  }
}
