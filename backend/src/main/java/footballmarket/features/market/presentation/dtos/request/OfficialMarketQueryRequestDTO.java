package footballmarket.features.market.presentation.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Parámetros de consulta para el catálogo del Mercado Oficial con cotizaciones integradas.
 *
 * @param page índice de página (base 0)
 * @param size cantidad de elementos por página (entre 1 y 100)
 * @param sort criterio y dirección de ordenamiento principal
 * @param name filtro por nombre o apellido del jugador
 * @param leagueId identificador de la liga deportiva
 * @param teamId identificador del club o equipo deportivo
 * @param position posición deportiva normalizada
 * @param nationality país de nacionalidad
 * @param hasQuote filtro por disponibilidad de cotización vigente
 * @param minPrice precio unitario mínimo en centésimas de crédito
 * @param maxPrice precio unitario máximo en centésimas de crédito
 */
public record OfficialMarketQueryRequestDTO(
    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size,
    @Size(max = 255) String sort,
    @Size(max = 255) String name,
    @Positive Long leagueId,
    @Positive Long teamId,
    @Size(max = 255) String position,
    @Size(max = 255) String nationality,
    Boolean hasQuote,
    @Min(0) Long minPrice,
    @Min(0) Long maxPrice) {}
