package footballmarket.features.market.presentation;

import footballmarket.features.market.application.OfficialMarketQuery;
import footballmarket.features.market.application.exceptions.InvalidPageSizeException;
import footballmarket.features.market.application.exceptions.InvalidPaginationException;
import footballmarket.features.market.application.exceptions.InvalidPriceRangeException;
import footballmarket.features.market.application.exceptions.InvalidSortParameterException;
import footballmarket.features.market.domain.records.OfficialMarketPlayer;
import footballmarket.features.market.domain.records.OfficialMarketQuote;
import footballmarket.features.market.infraestructure.projections.OfficialMarketPlayerProjection;
import footballmarket.features.market.presentation.dtos.request.OfficialMarketQueryRequestDTO;
import footballmarket.features.market.presentation.dtos.response.OfficialMarketPageResponseDTO;
import footballmarket.features.market.presentation.dtos.response.OfficialMarketPlayerResponseDTO;
import footballmarket.features.market.presentation.dtos.response.OfficialMarketQuoteResponseDTO;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;

/**
 * Mapper estático para la transformación entre representaciones de presentación, dominio y
 * persistencia del catálogo del Mercado Oficial.
 */
public final class OfficialMarketMapper {

  private static final Set<String> ALLOWED_SORT_PROPERTIES =
      Set.of("id", "name", "price", "variation");
  private static final Set<String> ALLOWED_SORT_DIRECTIONS = Set.of("asc", "desc");

  private OfficialMarketMapper() {}

  /**
   * Transforma y valida los criterios de consulta HTTP hacia el value object de dominio {@link
   * OfficialMarketQuery}.
   *
   * @param requestDTO DTO con parámetros de consulta
   * @param rawSortValues valores crudos del parámetro sort para detectar múltiples ocurrencias
   * @return consulta normalizada para la capa de aplicación
   */
  public static OfficialMarketQuery toQuery(
      OfficialMarketQueryRequestDTO requestDTO, String[] rawSortValues) {
    if (rawSortValues != null && rawSortValues.length > 1) {
      throw new InvalidSortParameterException(
          "Solo se admite un único criterio principal de ordenamiento");
    }

    int page = requestDTO.page() == null ? 0 : requestDTO.page();
    if (page < 0) {
      throw new InvalidPaginationException();
    }

    int size = requestDTO.size() == null ? 20 : requestDTO.size();
    if (size < 1 || size > 100) {
      throw new InvalidPageSizeException();
    }

    Long minPrice = requestDTO.minPrice();
    Long maxPrice = requestDTO.maxPrice();

    if (minPrice != null && minPrice < 0) {
      throw new InvalidPriceRangeException("El precio mínimo no puede ser negativo");
    }
    if (maxPrice != null && maxPrice < 0) {
      throw new InvalidPriceRangeException("El precio máximo no puede ser negativo");
    }
    if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
      throw new InvalidPriceRangeException(
          "El precio mínimo no puede ser mayor que el precio máximo");
    }

    String sortProperty = "id";
    String sortDirection = "asc";

    String rawSort = requestDTO.sort();
    if (rawSort != null && !rawSort.isBlank()) {
      String trimmedSort = rawSort.trim();
      String[] parts = trimmedSort.split(",");
      if (parts.length != 2) {
        throw new InvalidSortParameterException(
            "El parámetro sort debe tener el formato 'propiedad,direccion'");
      }
      String property = parts[0].trim().toLowerCase(Locale.ROOT);
      String direction = parts[1].trim().toLowerCase(Locale.ROOT);

      if (!ALLOWED_SORT_PROPERTIES.contains(property)
          || !ALLOWED_SORT_DIRECTIONS.contains(direction)) {
        throw new InvalidSortParameterException(
            "Criterio o dirección de ordenamiento no permitida: " + trimmedSort);
      }
      sortProperty = property;
      sortDirection = direction;
    }

    String name = requestDTO.name();
    if (name != null) {
      String trimmedName = name.trim();
      name = trimmedName.isEmpty() ? null : trimmedName;
    }

    return new OfficialMarketQuery(
        page,
        size,
        sortProperty,
        sortDirection,
        name,
        requestDTO.leagueId(),
        requestDTO.teamId(),
        requestDTO.position(),
        requestDTO.nationality(),
        requestDTO.hasQuote(),
        minPrice,
        maxPrice);
  }

  /**
   * Convierte una proyección SQL nativa en la entidad inmutable de dominio {@link
   * OfficialMarketPlayer}.
   *
   * @param projection fila proyectada desde la base de datos
   * @return jugador del mercado oficial
   */
  public static OfficialMarketPlayer toDomain(OfficialMarketPlayerProjection projection) {
    OfficialMarketQuote quote = null;
    if (projection.getQuoteId() != null) {
      Double variation = null;
      if (projection.getPreviousPrice() != null && projection.getPreviousPrice() > 0) {
        if (projection.getPreviousPrice().equals(projection.getPrice())) {
          variation = 0.0;
        } else if (projection.getVariationPercentage() != null) {
          variation = projection.getVariationPercentage();
        } else {
          double calc =
              ((double) (projection.getPrice() - projection.getPreviousPrice())
                      / projection.getPreviousPrice())
                  * 100.0;
          variation = Math.round(calc * 100.0) / 100.0;
        }
      }

      double score = projection.getScore() != null ? projection.getScore() : 0.0;
      double multiplier = projection.getMultiplier() != null ? projection.getMultiplier() : 1.0;
      String origin = projection.getOrigin() != null ? projection.getOrigin() : "INITIAL";

      quote =
          new OfficialMarketQuote(
              projection.getPrice(),
              projection.getPreviousPrice(),
              variation,
              score,
              multiplier,
              origin,
              projection.getEffectiveDate(),
              projection.getCalculatedAt());
    }

    return new OfficialMarketPlayer(
        projection.getId(),
        projection.getName(),
        projection.getPosition(),
        projection.getNationality(),
        projection.getTeamId(),
        projection.getTeamName(),
        projection.getTeamBadgeUrl(),
        projection.getLeagueId(),
        projection.getLeagueName(),
        projection.getImageUrl(),
        quote);
  }

  /**
   * Transforma una página de dominio en el contenedor paginado de respuesta HTTP {@link
   * OfficialMarketPageResponseDTO}.
   *
   * @param page página de dominio
   * @return respuesta HTTP paginada
   */
  public static OfficialMarketPageResponseDTO toResponse(Page<OfficialMarketPlayer> page) {
    List<OfficialMarketPlayerResponseDTO> content =
        page.getContent().stream().map(OfficialMarketMapper::toResponse).toList();
    return new OfficialMarketPageResponseDTO(
        content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  /**
   * Transforma una entidad de dominio en su DTO de respuesta HTTP {@link
   * OfficialMarketPlayerResponseDTO}.
   *
   * @param player jugador de dominio
   * @return DTO de respuesta sin campos de existencias
   */
  public static OfficialMarketPlayerResponseDTO toResponse(OfficialMarketPlayer player) {
    OfficialMarketQuoteResponseDTO quoteDTO = null;
    if (player.quote() != null) {
      OfficialMarketQuote q = player.quote();
      quoteDTO =
          new OfficialMarketQuoteResponseDTO(
              q.price(),
              q.previousPrice(),
              q.variationPercentage(),
              q.performanceScore(),
              q.multiplier(),
              q.calculationOrigin(),
              q.effectiveDate(),
              q.calculatedAt());
    }

    return new OfficialMarketPlayerResponseDTO(
        player.id(),
        player.name(),
        player.position(),
        player.nationality(),
        player.teamId(),
        player.teamName(),
        player.teamBadgeUrl(),
        player.leagueId(),
        player.leagueName(),
        player.imageUrl(),
        quoteDTO);
  }
}
