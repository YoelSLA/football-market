package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta HTTP del ranking por score.
 *
 * @param strategy estrategia de la última ejecución, nula si no existe ninguna
 * @param quoteDate fecha de la última ejecución, nula si no existe ninguna
 * @param ranking entradas ordenadas, siempre presentes
 */
@Schema(name = "ScoreRankingResponse", description = "Ranking por score de la última estrategia")
public record ScoreRankingResponseDTO(
    @Schema(
            description = "Estrategia de la última ejecución",
            allowableValues = {"POSITIONAL_PERFORMANCE", "GENERAL_ATTRIBUTES"},
            nullable = true)
        String strategy,
    @Schema(
            description = "Fecha de cotización de la última ejecución, en formato yyyy-MM-dd",
            type = "string",
            format = "date",
            nullable = true)
        LocalDate quoteDate,
    @Schema(description = "Entradas ordenadas del ranking; está presente aunque no haya datos")
        List<ScoreRankingEntryResponseDTO> ranking) {}
