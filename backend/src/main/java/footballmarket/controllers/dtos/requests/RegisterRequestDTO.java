package footballmarket.controllers.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
    name = "RegisterRequestDTO",
    description = "Datos necesarios para registrar un nuevo usuario.")
public record RegisterRequestDTO(
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
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password) {}
