package footballmarket.features.quote.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Historial de cotizaciones de un jugador.
 *
 * @param playerId identificador interno del jugador
 * @param quotes filas del historial, de la más reciente a la más antigua; vacío si nunca fue
 *     cotizado
 */
@Schema(
    name = "QuoteHistoryResponse",
    description = "Historial de cotizaciones de un jugador, de la más reciente a la más antigua")
public record QuoteHistoryResponseDTO(
    @Schema(description = "Identificador interno del jugador", example = "7821") Long playerId,
    @Schema(description = "Filas del historial, de la más reciente a la más antigua")
        List<QuoteResponseDTO> quotes) {}
