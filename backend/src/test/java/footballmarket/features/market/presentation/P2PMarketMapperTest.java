package footballmarket.features.market.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.market.application.P2PMarketQuery;
import footballmarket.features.market.application.exceptions.InvalidPageSizeException;
import footballmarket.features.market.application.exceptions.InvalidPaginationException;
import footballmarket.features.market.application.exceptions.InvalidPriceRangeException;
import footballmarket.features.market.application.exceptions.InvalidSortParameterException;
import footballmarket.features.market.domain.records.P2PMarketCatalog;
import footballmarket.features.market.domain.records.P2PMarketPlayer;
import footballmarket.features.market.domain.records.P2PMarketPriceRange;
import footballmarket.features.market.presentation.dtos.request.P2PMarketQueryRequestDTO;
import footballmarket.features.market.presentation.dtos.response.P2PMarketPageResponseDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@Tag("lightweight")
class P2PMarketMapperTest {

  @Test
  @DisplayName("toQuery aplica valores por defecto para Mercado P2P")
  void toQueryDefaultValues() {
    P2PMarketQueryRequestDTO dto =
        new P2PMarketQueryRequestDTO(null, null, null, null, null, null, null, null, null);

    P2PMarketQuery query = P2PMarketMapper.toQuery(dto, null);

    assertThat(query.page()).isZero();
    assertThat(query.size()).isEqualTo(8);
    assertThat(query.sortProperty()).isEqualTo("price");
    assertThat(query.sortDirection()).isEqualTo("asc");
    assertThat(query.search()).isNull();
  }

  @Test
  @DisplayName("toQuery valida sort property y direction")
  void toQuerySortValidation() {
    P2PMarketQueryRequestDTO validDto =
        new P2PMarketQueryRequestDTO(0, 8, "quantity,desc", "Messi", 1L, 2L, "DEL", 100L, 500L);

    P2PMarketQuery query = P2PMarketMapper.toQuery(validDto, null);

    assertThat(query.sortProperty()).isEqualTo("quantity");
    assertThat(query.sortDirection()).isEqualTo("desc");
    assertThat(query.search()).isEqualTo("Messi");

    P2PMarketQueryRequestDTO invalidSortDto =
        new P2PMarketQueryRequestDTO(0, 8, "invalidProp,asc", null, null, null, null, null, null);

    assertThatThrownBy(() -> P2PMarketMapper.toQuery(invalidSortDto, null))
        .isInstanceOf(InvalidSortParameterException.class);
  }

  @Test
  @DisplayName("toQuery valida rangos y paginación")
  void toQueryRangesAndPagination() {
    P2PMarketQueryRequestDTO negativePriceDto =
        new P2PMarketQueryRequestDTO(0, 8, null, null, null, null, null, -10L, 500L);

    assertThatThrownBy(() -> P2PMarketMapper.toQuery(negativePriceDto, null))
        .isInstanceOf(InvalidPriceRangeException.class);

    P2PMarketQueryRequestDTO invertedRangeDto =
        new P2PMarketQueryRequestDTO(0, 8, null, null, null, null, null, 1000L, 500L);

    assertThatThrownBy(() -> P2PMarketMapper.toQuery(invertedRangeDto, null))
        .isInstanceOf(InvalidPriceRangeException.class);

    P2PMarketQueryRequestDTO invalidPageDto =
        new P2PMarketQueryRequestDTO(-1, 8, null, null, null, null, null, null, null);

    assertThatThrownBy(() -> P2PMarketMapper.toQuery(invalidPageDto, null))
        .isInstanceOf(InvalidPaginationException.class);

    P2PMarketQueryRequestDTO invalidSizeDto =
        new P2PMarketQueryRequestDTO(0, 200, null, null, null, null, null, null, null);

    assertThatThrownBy(() -> P2PMarketMapper.toQuery(invalidSizeDto, null))
        .isInstanceOf(InvalidPageSizeException.class);
  }

  @Test
  @DisplayName("toResponse transforma catálogo P2P de dominio a DTO")
  void toResponseCatalog() {
    P2PMarketPlayer player =
        new P2PMarketPlayer(
            5L,
            "Player P2P",
            "MED",
            "ESP",
            20L,
            "Club B",
            null,
            3L,
            "LaLiga",
            null,
            2L,
            10L,
            1200L);
    P2PMarketCatalog catalog =
        new P2PMarketCatalog(
            new PageImpl<>(List.of(player), PageRequest.of(0, 8), 1),
            new P2PMarketPriceRange(1200L, 3000L));

    P2PMarketPageResponseDTO response = P2PMarketMapper.toResponse(catalog);

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().getFirst().name()).isEqualTo("Player P2P");
    assertThat(response.content().getFirst().listingCount()).isEqualTo(2L);
    assertThat(response.content().getFirst().availableQuantity()).isEqualTo(10L);
    assertThat(response.content().getFirst().minimumUnitPrice()).isEqualTo(1200L);
    assertThat(response.priceRange().min()).isEqualTo(1200L);
    assertThat(response.priceRange().max()).isEqualTo(3000L);
  }
}
