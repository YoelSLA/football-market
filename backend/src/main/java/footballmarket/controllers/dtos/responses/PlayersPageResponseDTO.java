package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PlayersPageResponseDTO(
    @Schema(
            description = "Jugadores activos de la página",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<PlayerResponseDTO> content,
    @Schema(
            description = "Número de página desde cero",
            example = "0",
            minimum = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int page,
    @Schema(
            description = "Tamaño solicitado",
            example = "20",
            minimum = "1",
            maximum = "100",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int size,
    @Schema(
            description = "Cantidad total de jugadores activos",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalElements,
    @Schema(
            description = "Cantidad total de páginas",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int totalPages) {}
