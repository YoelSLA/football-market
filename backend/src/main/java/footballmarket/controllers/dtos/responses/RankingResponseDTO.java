package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Entrada del ranking de jugadores por cotización vigente.
 *
 * @param position posición en el ranking, desde uno
 * @param playerId identificador interno del jugador
 * @param price precio vigente en centésimas de crédito
 * @param effectiveDate fecha de vigencia de la cotización vigente
 * @param origin origen de la fila vigente: {@code INITIAL}, {@code RECOMPUTED} o {@code
 *     CARRIED_FORWARD}
 * @param calculatedAt instante de cálculo de la cotización vigente, no el de la lectura
 */
@Schema(
    name = "RankingResponse",
    description = "Entrada del ranking de jugadores por cotización vigente")
public record RankingResponseDTO(
    @Schema(description = "Posición en el ranking", example = "1") int position,
    @Schema(description = "Identificador interno del jugador", example = "7821") Long playerId,
    @Schema(description = "Precio vigente en centésimas de crédito", example = "105") long price,
    @Schema(description = "Fecha de vigencia de la cotización vigente", example = "2026-10-20")
        LocalDate effectiveDate,
    @Schema(
            description = "Origen de la fila vigente",
            example = "RECOMPUTED",
            allowableValues = {"INITIAL", "RECOMPUTED", "CARRIED_FORWARD"})
        String origin,
    @Schema(
            description = "Instante de cálculo de la cotización vigente, no el de la lectura",
            example = "2026-10-20T18:00:00Z")
        Instant calculatedAt) {}
