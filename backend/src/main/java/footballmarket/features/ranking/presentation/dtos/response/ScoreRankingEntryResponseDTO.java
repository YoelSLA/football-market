package footballmarket.features.ranking.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entrada HTTP del ranking por score.
 *
 * @param position posición consecutiva desde uno
 * @param playerId identificador interno del jugador
 * @param score resultado final persistido, sin transformación
 * @param valuationDate fecha de la valuación utilizada
 */
@Schema(name = "ScoreRankingEntryResponse", description = "Entrada del ranking por score")
public record ScoreRankingEntryResponseDTO(
    @Schema(description = "Posición consecutiva en el ranking", example = "1") Integer position,
    @Schema(description = "Identificador interno del jugador", example = "7821") Long playerId,
    @Schema(
            description = "Resultado final persistido a seis decimales, sin transformación",
            example = "0.913400")
        BigDecimal score,
    @Schema(
            description = "Fecha de la valuación utilizada, en formato yyyy-MM-dd",
            type = "string",
            format = "date",
            example = "2026-10-21")
        LocalDate valuationDate) {}
