package footballmarket.features.market.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Contenedor paginado del catálogo del Mercado Oficial.
 *
 * @param content lista de jugadores de la página actual
 * @param page índice de página actual (base 0)
 * @param size tamaño de página efectivo
 * @param totalElements cantidad total de jugadores coincidentes con los filtros
 * @param totalPages cantidad total de páginas disponibles
 */
@Schema(
    name = "OfficialMarketPageResponse",
    description = "Respuesta paginada del catálogo del Mercado Oficial")
public record OfficialMarketPageResponseDTO(
    @Schema(description = "Elementos de la página actual")
        List<OfficialMarketPlayerResponseDTO> content,
    @Schema(description = "Índice de página actual (base 0)", example = "0") int page,
    @Schema(description = "Tamaño de página solicitado o efectivo", example = "20") int size,
    @Schema(description = "Total de jugadores que coinciden con todos los filtros", example = "42")
        long totalElements,
    @Schema(description = "Total de páginas disponibles", example = "3") int totalPages) {}
