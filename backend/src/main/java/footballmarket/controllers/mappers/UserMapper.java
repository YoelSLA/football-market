package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.requests.RegisterRequestDTO;
import footballmarket.controllers.dtos.responses.CurrentUserResponseDTO;
import footballmarket.models.User;

/** Transforma entre contratos HTTP de usuarios y modelos de dominio. */
public final class UserMapper {

  private UserMapper() {}

  /**
   * Expone exclusivamente la identidad actual del usuario persistido.
   *
   * @param user usuario recuperado por el servicio
   * @return identificador y email actuales
   */
  public static CurrentUserResponseDTO toCurrentUserResponse(User user) {
    return new CurrentUserResponseDTO(user.getId(), user.getEmail());
  }

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
