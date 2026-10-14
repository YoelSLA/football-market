package footballmarket.models;

import footballmarket.models.enums.UserRole;
import footballmarket.models.exceptions.EmailEmptyException;
import footballmarket.models.exceptions.EmailInvalidException;
import footballmarket.models.exceptions.EmptyPasswordException;
import footballmarket.models.exceptions.PasswordTooShortException;
import footballmarket.models.exceptions.UsernameEmptyException;
import footballmarket.models.exceptions.UsernameInvalidException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Individuo registrado en el sistema.
 *
 * <p>El nombre de usuario es la identidad pública del usuario y el rol expresa su capacidad en el
 * dominio de mercado. El nombre de usuario nunca es una credencial: la autenticación se resuelve
 * únicamente por correo electrónico y contraseña.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

  private static final int USERNAME_MIN_LENGTH = 3;
  private static final int USERNAME_MAX_LENGTH = 30;
  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]+$");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  /**
   * Construye un usuario con el rol USER, que es el que corresponde a toda alta registrada
   * públicamente.
   *
   * @param username nombre de usuario público
   * @param email correo electrónico del usuario
   * @param password contraseña del usuario
   */
  public User(String username, String email, String password) {
    this.username = this.validateUsername(username);
    this.email = this.validateEmail(email);
    this.password = this.validatePassword(password);
    this.role = UserRole.USER;
  }

  /**
   * Construye un usuario con el rol indicado.
   *
   * <p>Solo debe utilizarse para la provisión interna de una cuenta con capacidad de SUPERUSER. El
   * registro público utiliza la construcción que asigna USER.
   *
   * @param id identificador persistido, o {@code null} si la cuenta es nueva
   * @param username nombre de usuario público
   * @param email correo electrónico del usuario
   * @param password contraseña del usuario
   * @param role capacidad del usuario en el dominio
   */
  public User(Long id, String username, String email, String password, UserRole role) {
    this.id = id;
    this.username = this.validateUsername(username);
    this.email = this.validateEmail(email);
    this.password = this.validatePassword(password);
    this.role = role;
  }

  public void setPassword(String password) {
    this.password = this.validatePassword(password);
  }

  public void setEmail(String email) {
    this.email = this.validateEmail(email);
  }

  /**
   * Normaliza el nombre de usuario a minúsculas y verifica sus invariantes.
   *
   * @param username nombre de usuario recibido
   * @return nombre de usuario normalizado
   * @throws UsernameEmptyException si el nombre de usuario es nulo o vacío
   * @throws UsernameInvalidException si no cumple el formato o la longitud admitidos
   */
  private String validateUsername(String username) {
    if (username == null || username.isBlank()) {
      throw new UsernameEmptyException("El nombre de usuario es obligatorio.");
    }

    String normalized = username.toLowerCase(Locale.ROOT);

    if (normalized.length() < USERNAME_MIN_LENGTH || normalized.length() > USERNAME_MAX_LENGTH) {
      throw new UsernameInvalidException(
          "El nombre de usuario debe tener entre 3 y 30 caracteres.");
    }

    if (!USERNAME_PATTERN.matcher(normalized).matches()) {
      throw new UsernameInvalidException(
          "El nombre de usuario solo admite letras minúsculas, números, punto, guion y guion bajo.");
    }

    return normalized;
  }

  private String validateEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new EmailEmptyException("El email es vacio.");
    }
    if (!email.contains("@")) {
      throw new EmailInvalidException("El email no es valido.");
    }
    return email;
  }

  private String validatePassword(String password) {
    if (password == null || password.isBlank()) {
      throw new EmptyPasswordException("La password no puede ser vacia.");
    }
    if (password.length() < 8) {
      throw new PasswordTooShortException("La longitud debe ser igual o mayor a 8 caracteres.");
    }
    return password;
  }
}
