package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Ranking de jugadores por cotización vigente.
 *
 * @param ranking entradas del ranking, de mayor a menor precio; vacío si todavía no hay
 *     cotizaciones
 */
@Schema(
    name = "PlayerRankingResponse",
    description = "Ranking de jugadores por cotización vigente, de mayor a menor precio")
public record PlayerRankingResponseDTO(
    @Schema(description = "Entradas del ranking, de mayor a menor precio")
        List<RankingResponseDTO> ranking) {}
