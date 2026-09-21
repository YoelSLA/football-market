package footballmarket.services;

import footballmarket.models.User;

/** Contrato de registro y autenticación de usuarios. */
public interface AuthenticationService {

  /**
   * Registra un usuario nuevo.
   *
   * @param user usuario a registrar
   */
  void register(User user);

  /**
   * Autentica credenciales y genera un token.
   *
   * @param email email del usuario
   * @param password contraseña sin codificar
   * @return token JWT
   */
  String login(String email, String password);

  /**
   * Recupera la identidad persistida actual a partir del sujeto de la sesión.
   *
   * @param subjectEmail email sujeto, normalizado con las reglas de autenticación
   * @return usuario persistido actual
   * @throws footballmarket.services.exceptions.CurrentUserNotFoundException si el sujeto es nulo,
   *     vacío o no identifica a un usuario existente
   */
  User getCurrentUser(String subjectEmail);
}
