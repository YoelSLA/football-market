package footballmarket.controllers.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(
    name = "LoginRequestDTO",
    description = "Describe los datos necesarios para iniciar sesión.")
public record LoginRequestDTO(
    @Schema(
            description = "Correo electrónico del usuario.",
            example = "usuario@email.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe ser válido")
        String email,
    @Schema(
            description = "Contraseña del usuario.",
            example = "Password123",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La contraseña es obligatoria")
        String password) {}
