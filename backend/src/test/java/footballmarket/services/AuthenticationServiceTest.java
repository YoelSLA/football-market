package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.User;
import footballmarket.services.exceptions.EmailAlreadyRegisteredException;
import footballmarket.services.exceptions.InvalidCredentialsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthenticationServiceTest {

  @Autowired private AuthenticationService authenticationService;
  @Autowired private UserService userService;

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

    assertThatThrownBy(() -> authenticationService.login("hash-login@test.com", user.getPassword()))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void deberiaLanzarExcepcionCuandoElEmailYaExiste() {
    User firstUser = new User("duplicate@test.com", "password");

    authenticationService.register(firstUser);

    User secondUser = new User("DUPLICATE@TEST.COM", "anotherPassword");

    assertThatThrownBy(() -> authenticationService.register(secondUser))
        .isInstanceOf(EmailAlreadyRegisteredException.class);
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

  @AfterEach
  void eliminar() {
    this.userService.deteleAllUsers();
  }
}
