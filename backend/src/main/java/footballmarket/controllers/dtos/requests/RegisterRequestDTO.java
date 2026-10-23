package footballmarket.controllers.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(
    name = "RegisterRequestDTO",
    description =
        "Datos necesarios para registrar un nuevo usuario. El rol no se recibe: todo alta pública"
            + " recibe el rol USER.")
public record RegisterRequestDTO(
    @Schema(
            description =
                "Nombre de usuario público del usuario. Admite entre 3 y 30 caracteres, únicamente"
                    + " letras, números, punto, guion y guion bajo. No distingue mayúsculas de"
                    + " minúsculas: se normaliza a minúsculas antes de persistirse, de modo que"
                    + " \"Juan.Perez\" y \"JUAN.PEREZ\" son el mismo nombre de usuario. No es una"
                    + " credencial.",
            example = "juan.perez",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(
            min = 3,
            max = 30,
            message = "El nombre de usuario debe tener entre 3 y 30 caracteres")
        @Pattern(
            regexp = "^[A-Za-z0-9._-]+$",
            message = "El nombre de usuario solo admite letras, números, punto, guion y guion bajo")
        String username,
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
