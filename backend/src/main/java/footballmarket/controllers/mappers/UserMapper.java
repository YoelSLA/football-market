package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.requests.RegisterRequestDTO;
import footballmarket.models.User;

/** Transforma contratos HTTP de usuarios en modelos de dominio. */
public final class UserMapper {

  private UserMapper() {}

  /**
   * Convierte una solicitud de registro validada en un usuario.
   *
   * @param request solicitud de registro validada
   * @return usuario con los datos recibidos
   */
  public static User toModel(RegisterRequestDTO request) {
    return new User(request.email(), request.password());
  }
}
