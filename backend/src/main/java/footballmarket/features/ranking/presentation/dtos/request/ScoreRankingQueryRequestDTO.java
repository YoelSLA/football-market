package footballmarket.features.ranking.presentation.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Parámetros de página y filtros del ranking por score. */
public record ScoreRankingQueryRequestDTO(
    @Schema(description = "Página base cero", defaultValue = "0")
        @Min(value = 0, message = "La página debe ser mayor o igual que cero")
        Integer page,
    @Schema(description = "Tamaño entre 1 y 100", defaultValue = "20")
        @Min(value = 1, message = "El tamaño debe ser mayor que cero")
        @Max(value = 100, message = "El tamaño no puede superar 100")
        Integer size,
    @Schema(description = "Nombre parcial; % y _ son literales")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
        String name,
    @Schema(description = "Identificador positivo de liga")
        @Positive(message = "El identificador de liga debe ser positivo")
        Long leagueId,
    @Schema(description = "Posición del catálogo")
        @Size(max = 255, message = "La posición no puede superar 255 caracteres")
        String position) {
  public ScoreRankingQueryRequestDTO {
    page = page == null ? 0 : page;
    size = size == null ? 20 : size;
  }
}
