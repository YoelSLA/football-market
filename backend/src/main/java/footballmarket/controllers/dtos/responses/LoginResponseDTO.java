package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "LoginResponseDTO",
    description = "Respuesta generada al iniciar sesión correctamente.")
public record LoginResponseDTO(
    @Schema(
            description = "Token JWT utilizado para autenticar las solicitudes del usuario.",
            example = "eyJhbGciOiJIUzI1NiJ9...",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String token) {}
