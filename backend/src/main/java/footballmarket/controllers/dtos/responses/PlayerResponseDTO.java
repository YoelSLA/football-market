package footballmarket.controllers.dtos.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Representación pública local; no expone identidades de proveedores ni estado de persistencia. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PlayerResponseDTO(
    @Schema(
            description = "Identificador interno de FootballMarket, independiente de proveedores",
            example = "7821",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,
    @Schema(
            description = "Nombre del jugador",
            example = "Joel Robles",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
    @Schema(
            description = "Equipo actual",
            example = "Real Betis Balompié",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String team,
    @Schema(
            description = "Primera competición en el orden configurado",
            example = "Primera Division",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String league,
    @Schema(
            description = "Posición del jugador",
            example = "Goalkeeper",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String position,
    @Schema(
            description = "Fecha de nacimiento; null cuando la fuente no la informa",
            format = "date",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1990-06-20")
        LocalDate dateOfBirth,
    @Schema(
            description = "Nacionalidad; null cuando la fuente no la informa",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Spain")
        String nationality,
    @Schema(
            description = "Imagen principal opcional persistida localmente",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        String imageUrl,
    @Schema(
            description = "Imagen alternativa opcional persistida localmente",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        String fallbackImageUrl) {}
