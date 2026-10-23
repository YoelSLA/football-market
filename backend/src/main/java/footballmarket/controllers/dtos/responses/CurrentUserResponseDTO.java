package footballmarket.controllers.dtos.responses;

import footballmarket.models.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Identidad actual del usuario persistido asociado a la sesión.
 *
 * @param id identificador del usuario persistido
 * @param username nombre de usuario público, normalizado en minúsculas
 * @param email email actual del usuario
 * @param role capacidad del usuario en el dominio; es informativo y el backend sigue evaluando toda
 *     autorización a partir del rol persistido
 */
public record CurrentUserResponseDTO(
    @Schema(
            description = "Identificador del usuario",
            example = "123",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,
    @Schema(
            description = "Nombre de usuario público del usuario",
            example = "juan.perez",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String username,
    @Schema(
            description = "Email actual del usuario",
            example = "usuario@email.com",
            format = "email",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String email,
    @Schema(
            description =
                "Rol del usuario. USER es el usuario registrado públicamente y SUPERUSER es"
                    + " el dueño inicial de los tokens. Es informativo: el backend no delega en él la"
                    + " autorización.",
            example = "USER",
            requiredMode = Schema.RequiredMode.REQUIRED)
        UserRole role) {}
