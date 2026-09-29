package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Identidad actual del usuario persistido asociado a la sesión.
 *
 * @param id identificador del usuario persistido
 * @param email email actual del usuario
 */
public record CurrentUserResponseDTO(
    @Schema(
            description = "Identificador del usuario",
            example = "123",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,
    @Schema(
            description = "Email actual del usuario",
            example = "usuario@email.com",
            format = "email",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String email) {}
