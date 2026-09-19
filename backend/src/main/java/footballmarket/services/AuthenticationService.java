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
}
