package footballmarket.features.auth.application;

import footballmarket.features.auth.application.exceptions.CurrentUserNotFoundException;
import footballmarket.features.auth.application.exceptions.EmailAlreadyRegisteredException;
import footballmarket.features.auth.application.exceptions.InvalidCredentialsException;
import footballmarket.features.auth.application.exceptions.UsernameAlreadyRegisteredException;
import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.domain.records.CurrentUserView;

/** Contrato de registro y autenticación de usuarios. */
public interface AuthenticationService {

  /**
   * Registra un usuario público nuevo con el rol USER.
   *
   * @param user usuario a registrar
   * @throws EmailAlreadyRegisteredException si el correo electrónico ya está registrado
   * @throws UsernameAlreadyRegisteredException si el nombre de usuario ya está en uso
   */
  void register(User user);

  /**
   * Auténtica credencial y genera un token.
   *
   * <p>Las credenciales son el correo electrónico y la contraseña. El nombre de usuario no es una
   * credencial y no puede utilizarse en su lugar.
   *
   * @param email email del usuario
   * @param password contraseña sin codificar
   * @return token JWT
   * @throws InvalidCredentialsException si las credenciales no identifican a un usuario registrado
   */
  String login(String email, String password);

  /**
   * Recupera la identidad persistida actual y sus créditos a partir del sujeto de la sesión.
   *
   * @param subjectEmail email sujeto, normalizado con las reglas de autenticación
   * @return vista con los datos del usuario actual y su saldo disponible
   * @throws CurrentUserNotFoundException si el sujeto es nulo, vacío o no identifica a un usuario
   *     existente
   */
  CurrentUserView getCurrentUser(String subjectEmail);
}
