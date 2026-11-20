package footballmarket.features.market.presentation;

import footballmarket.features.market.application.P2PMarketQuery;
import footballmarket.features.market.application.exceptions.InvalidPageSizeException;
import footballmarket.features.market.application.exceptions.InvalidPaginationException;
import footballmarket.features.market.application.exceptions.InvalidPriceRangeException;
import footballmarket.features.market.application.exceptions.InvalidSortParameterException;
import footballmarket.features.market.domain.records.P2PMarketCatalog;
import footballmarket.features.market.domain.records.P2PMarketPlayer;
import footballmarket.features.market.presentation.dtos.request.P2PMarketQueryRequestDTO;
import footballmarket.features.market.presentation.dtos.response.P2PMarketPageResponseDTO;
import footballmarket.features.market.presentation.dtos.response.P2PMarketPlayerResponseDTO;
import footballmarket.features.market.presentation.dtos.response.P2PMarketPriceRangeResponseDTO;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;

/**
 * Mapper estático para la transformación entre representaciones de presentación y dominio del
 * catálogo del Mercado P2P.
 */
public final class P2PMarketMapper {

  private static final Set<String> ALLOWED_SORT_PROPERTIES =
      Set.of("id", "name", "price", "quantity");
  private static final Set<String> ALLOWED_SORT_DIRECTIONS = Set.of("asc", "desc");

  private P2PMarketMapper() {}

  /**
   * Transforma y valida los criterios de consulta HTTP hacia el value object de dominio {@link
   * P2PMarketQuery}.
   *
   * @param requestDTO DTO con parámetros de consulta
   * @param rawSortValues valores crudos del parámetro sort para detectar múltiples ocurrencias
   * @return consulta normalizada para la capa de aplicación
   */
  public static P2PMarketQuery toQuery(
      P2PMarketQueryRequestDTO requestDTO, String[] rawSortValues) {
    if (rawSortValues != null && rawSortValues.length > 1) {
      throw new InvalidSortParameterException(
          "Solo se admite un único criterio principal de ordenamiento");
    }

    int page = requestDTO.page() == null ? 0 : requestDTO.page();
    if (page < 0) {
      throw new InvalidPaginationException();
    }

    int size = requestDTO.size() == null ? 8 : requestDTO.size();
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

    String sortProperty = "price";
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

    String search = requestDTO.search();
    if (search != null) {
      String trimmedSearch = search.trim();
      search = trimmedSearch.isEmpty() ? null : trimmedSearch;
    }

    return new P2PMarketQuery(
        page,
        size,
        sortProperty,
        sortDirection,
        search,
        requestDTO.leagueId(),
        requestDTO.teamId(),
        requestDTO.position(),
        minPrice,
        maxPrice);
  }

  /**
   * Transforma el catálogo de dominio al DTO de respuesta paginado {@link
   * P2PMarketPageResponseDTO}.
   */
  public static P2PMarketPageResponseDTO toResponse(P2PMarketCatalog catalog) {
    Page<P2PMarketPlayer> page = catalog.players();
    List<P2PMarketPlayerResponseDTO> content =
        page.getContent().stream().map(P2PMarketMapper::toResponse).toList();
    return new P2PMarketPageResponseDTO(
        content,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        new P2PMarketPriceRangeResponseDTO(catalog.priceRange().min(), catalog.priceRange().max()));
  }

  /** Transforma un jugador de dominio al DTO de respuesta {@link P2PMarketPlayerResponseDTO}. */
  public static P2PMarketPlayerResponseDTO toResponse(P2PMarketPlayer player) {
    return new P2PMarketPlayerResponseDTO(
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
        player.listingCount(),
        player.availableQuantity(),
        player.minimumUnitPrice());
  }
}
