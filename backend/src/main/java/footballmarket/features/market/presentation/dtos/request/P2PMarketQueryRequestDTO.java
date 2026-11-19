package footballmarket.features.market.presentation.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Parámetros de consulta HTTP para el catálogo del Mercado P2P.
 *
 * @param page índice de página (base 0)
 * @param size cantidad de elementos por página (entre 1 y 100)
 * @param sort criterio y dirección de ordenamiento (ej. 'price,asc', 'quantity,desc')
 * @param search filtro por nombre del jugador o nombre del club
 * @param leagueId identificador de la liga
 * @param teamId identificador del club
 * @param position posición deportiva
 * @param minPrice precio unitario mínimo en centésimas
 * @param maxPrice precio unitario máximo en centésimas
 */
public record P2PMarketQueryRequestDTO(
    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size,
    @Size(max = 255) String sort,
    @Size(max = 255) String search,
    @Positive Long leagueId,
    @Positive Long teamId,
    @Size(max = 255) String position,
    @Min(0) Long minPrice,
    @Min(0) Long maxPrice) {}
