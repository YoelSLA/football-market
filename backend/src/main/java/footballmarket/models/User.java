package footballmarket.models;

import footballmarket.models.exceptions.EmailEmptyException;
import footballmarket.models.exceptions.EmailInvalidException;
import footballmarket.models.exceptions.EmptyPasswordException;
import footballmarket.models.exceptions.PasswordTooShortException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  public User(Long id, String email, String password) {
    this.id = id;
    this.email = this.validateEmail(email);
    this.password = this.validatePassword(password);
  }

  public User(String email, String password) {
    this.email = this.validateEmail(email);
    this.password = this.validatePassword(password);
  }

  public void setPassword(String password) {
    this.password = this.validatePassword(password);
  }

  public void setEmail(String email) {
    this.email = this.validateEmail(email);
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
