package footballmarket.features.auth.presentation;

import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.presentation.dtos.request.RegisterRequestDTO;
import footballmarket.features.auth.presentation.dtos.response.CurrentUserResponseDTO;

/** Transforma entre contratos HTTP de usuarios y modelos de dominio. */
public final class UserMapper {

  private UserMapper() {}

  /**
   * Expone la identidad actual del usuario persistido.
   *
   * @param user usuario recuperado por el servicio
   * @return identificador, nombre de usuario, email y rol actuales
   */
  public static CurrentUserResponseDTO toCurrentUserResponse(User user) {
    return new CurrentUserResponseDTO(
        user.getId(), user.getUsername(), user.getEmail(), user.getRole());
  }

  /**
   * Convierte una solicitud de registro validada en un usuario.
   *
   * <p>El rol no se toma de la solicitud: la construcción del usuario le asigna USER como
   * invariante, de modo que el cliente no puede solicitar SUPERUSER desde el registro público.
   *
   * @param request solicitud de registro validada
   * @return usuario con los datos recibidos y el rol USER
   */
  public static User toModel(RegisterRequestDTO request) {
    return new User(request.username(), request.email(), request.password());
  }
}
