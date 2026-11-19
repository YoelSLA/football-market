package footballmarket.features.market.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import footballmarket.features.market.application.OfficialMarketQuery;
import footballmarket.features.market.application.exceptions.InvalidPageSizeException;
import footballmarket.features.market.application.exceptions.InvalidPaginationException;
import footballmarket.features.market.application.exceptions.InvalidPriceRangeException;
import footballmarket.features.market.application.exceptions.InvalidSortParameterException;
import footballmarket.features.market.domain.records.OfficialMarketCatalog;
import footballmarket.features.market.domain.records.OfficialMarketPlayer;
import footballmarket.features.market.domain.records.OfficialMarketPriceRange;
import footballmarket.features.market.domain.records.OfficialMarketQuote;
import footballmarket.features.market.infraestructure.projections.OfficialMarketPlayerProjection;
import footballmarket.features.market.presentation.dtos.request.OfficialMarketQueryRequestDTO;
import footballmarket.features.market.presentation.dtos.response.OfficialMarketPageResponseDTO;
import footballmarket.features.market.presentation.dtos.response.OfficialMarketPlayerResponseDTO;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@Tag("lightweight")
class OfficialMarketMapperTest {

  @Test
  @DisplayName("toQuery aplica valores por defecto cuando los campos son nulos")
  void toQueryDefaultValues() {
    OfficialMarketQueryRequestDTO dto =
        new OfficialMarketQueryRequestDTO(
            null, null, null, null, null, null, null, null, null, null, null);

    OfficialMarketQuery query = OfficialMarketMapper.toQuery(dto, null);

    assertThat(query.page()).isZero();
    assertThat(query.size()).isEqualTo(20);
    assertThat(query.sortProperty()).isEqualTo("id");
    assertThat(query.sortDirection()).isEqualTo("asc");
    assertThat(query.name()).isNull();
    assertThat(query.leagueId()).isNull();
    assertThat(query.teamId()).isNull();
    assertThat(query.position()).isNull();
    assertThat(query.nationality()).isNull();
    assertThat(query.hasQuote()).isNull();
    assertThat(query.minPrice()).isNull();
    assertThat(query.maxPrice()).isNull();
  }

  @Test
  @DisplayName("toQuery mapea y normaliza parámetros completos válidos")
  void toQueryValidFullParameters() {
    OfficialMarketQueryRequestDTO dto =
        new OfficialMarketQueryRequestDTO(
            2,
            50,
            "price,desc",
            "  Lionel Messi  ",
            12L,
            45L,
            "DEL",
            "Argentina",
            true,
            1000L,
            50000L);

    OfficialMarketQuery query = OfficialMarketMapper.toQuery(dto, null);

    assertThat(query.page()).isEqualTo(2);
    assertThat(query.size()).isEqualTo(50);
    assertThat(query.sortProperty()).isEqualTo("price");
    assertThat(query.sortDirection()).isEqualTo("desc");
    assertThat(query.name()).isEqualTo("Lionel Messi");
    assertThat(query.leagueId()).isEqualTo(12L);
    assertThat(query.teamId()).isEqualTo(45L);
    assertThat(query.position()).isEqualTo("DEL");
    assertThat(query.nationality()).isEqualTo("Argentina");
    assertThat(query.hasQuote()).isTrue();
    assertThat(query.minPrice()).isEqualTo(1000L);
    assertThat(query.maxPrice()).isEqualTo(50000L);
  }

  @Test
  @DisplayName("toQuery normaliza a null un nombre compuesto solo de espacios")
  void toQueryTrimsBlankNameToNull() {
    OfficialMarketQueryRequestDTO dto =
        new OfficialMarketQueryRequestDTO(
            null, null, null, "   ", null, null, null, null, null, null, null);

    OfficialMarketQuery query = OfficialMarketMapper.toQuery(dto, null);

    assertThat(query.name()).isNull();
  }

  @Test
  @DisplayName("toQuery rechaza página negativa")
  void toQueryInvalidPagination() {
    OfficialMarketQueryRequestDTO dto =
        new OfficialMarketQueryRequestDTO(
            -1, null, null, null, null, null, null, null, null, null, null);

    assertThatThrownBy(() -> OfficialMarketMapper.toQuery(dto, null))
        .isInstanceOf(InvalidPaginationException.class);
  }

  @Test
  @DisplayName("toQuery rechaza tamaño de página menor a 1 o mayor a 100")
  void toQueryInvalidPageSize() {
    OfficialMarketQueryRequestDTO tooSmallDto =
        new OfficialMarketQueryRequestDTO(
            null, 0, null, null, null, null, null, null, null, null, null);

    assertThatThrownBy(() -> OfficialMarketMapper.toQuery(tooSmallDto, null))
        .isInstanceOf(InvalidPageSizeException.class);

    OfficialMarketQueryRequestDTO tooLargeDto =
        new OfficialMarketQueryRequestDTO(
            null, 101, null, null, null, null, null, null, null, null, null);

    assertThatThrownBy(() -> OfficialMarketMapper.toQuery(tooLargeDto, null))
        .isInstanceOf(InvalidPageSizeException.class);
  }

  @Test
  @DisplayName("toQuery valida rangos de precios coherentes y no negativos")
  void toQueryInvalidPriceRange() {
    OfficialMarketQueryRequestDTO negativeMinDto =
        new OfficialMarketQueryRequestDTO(
            null, null, null, null, null, null, null, null, null, -10L, 500L);

    assertThatThrownBy(() -> OfficialMarketMapper.toQuery(negativeMinDto, null))
        .isInstanceOf(InvalidPriceRangeException.class);

    OfficialMarketQueryRequestDTO negativeMaxDto =
        new OfficialMarketQueryRequestDTO(
            null, null, null, null, null, null, null, null, null, 100L, -5L);

    assertThatThrownBy(() -> OfficialMarketMapper.toQuery(negativeMaxDto, null))
        .isInstanceOf(InvalidPriceRangeException.class);

    OfficialMarketQueryRequestDTO invertedRangeDto =
        new OfficialMarketQueryRequestDTO(
            null, null, null, null, null, null, null, null, null, 2000L, 1000L);

    assertThatThrownBy(() -> OfficialMarketMapper.toQuery(invertedRangeDto, null))
        .isInstanceOf(InvalidPriceRangeException.class);
  }

  @Test
  @DisplayName("toQuery rechaza criterios de ordenamiento inválidos")
  void toQueryInvalidSort() {
    OfficialMarketQueryRequestDTO dto =
        new OfficialMarketQueryRequestDTO(
            null, null, "price,asc", null, null, null, null, null, null, null, null);

    // Múltiples criterios
    assertThatThrownBy(
            () -> OfficialMarketMapper.toQuery(dto, new String[] {"price,asc", "name,desc"}))
        .isInstanceOf(InvalidSortParameterException.class);

    // Formato sin coma
    OfficialMarketQueryRequestDTO noCommaDto =
        new OfficialMarketQueryRequestDTO(
            null, null, "price", null, null, null, null, null, null, null, null);
    assertThatThrownBy(() -> OfficialMarketMapper.toQuery(noCommaDto, null))
        .isInstanceOf(InvalidSortParameterException.class);

    // Propiedad no permitida
    OfficialMarketQueryRequestDTO invalidPropDto =
        new OfficialMarketQueryRequestDTO(
            null, null, "invalidProp,asc", null, null, null, null, null, null, null, null);
    assertThatThrownBy(() -> OfficialMarketMapper.toQuery(invalidPropDto, null))
        .isInstanceOf(InvalidSortParameterException.class);

    // Dirección no permitida
    OfficialMarketQueryRequestDTO invalidDirDto =
        new OfficialMarketQueryRequestDTO(
            null, null, "price,sideways", null, null, null, null, null, null, null, null);
    assertThatThrownBy(() -> OfficialMarketMapper.toQuery(invalidDirDto, null))
        .isInstanceOf(InvalidSortParameterException.class);
  }

  @Test
  @DisplayName("toResponse transforma página de jugadores de dominio a DTO paginado")
  void toResponsePage() {
    OfficialMarketQuote quote =
        OfficialMarketQuote.fromRawValues(
            1500L, 1000L, 50.0, 8.5, 1.2, "POSITIONAL_PERFORMANCE", LocalDate.now(), Instant.now());
    OfficialMarketPlayer player =
        new OfficialMarketPlayer(
            1L,
            "Player",
            "DEL",
            "ARG",
            10L,
            "Club",
            "https://images.example.com/team.png",
            5L,
            "Liga",
            "https://images.example.com/player.png",
            quote);
    Page<OfficialMarketPlayer> page = new PageImpl<>(List.of(player), PageRequest.of(0, 20), 1);

    OfficialMarketPageResponseDTO response = OfficialMarketMapper.toResponse(page);

    assertThat(response.content()).hasSize(1);
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.totalElements()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(1);

    OfficialMarketPlayerResponseDTO item = response.content().getFirst();
    assertThat(item.id()).isEqualTo(1L);
    assertThat(item.name()).isEqualTo("Player");
    assertThat(item.position()).isEqualTo("DEL");
    assertThat(item.nationality()).isEqualTo("ARG");
    assertThat(item.teamId()).isEqualTo(10L);
    assertThat(item.teamName()).isEqualTo("Club");
    assertThat(item.teamBadgeUrl()).isEqualTo("https://images.example.com/team.png");
    assertThat(item.leagueId()).isEqualTo(5L);
    assertThat(item.leagueName()).isEqualTo("Liga");
    assertThat(item.imageUrl()).isEqualTo("https://images.example.com/player.png");
    assertThat(item.quote()).isNotNull();
    assertThat(item.quote().price()).isEqualTo(1500L);
    assertThat(item.quote().previousPrice()).isEqualTo(1000L);
    assertThat(item.quote().variationPercentage()).isEqualTo(50.0);
    assertThat(item.quote().performanceScore()).isEqualTo(8.5);
    assertThat(item.quote().multiplier()).isEqualTo(1.2);
    assertThat(item.quote().calculationOrigin()).isEqualTo("POSITIONAL_PERFORMANCE");
  }

  @Test
  @DisplayName("toResponse transforma catálogo oficial de dominio con priceRange a DTO paginado")
  void toResponseCatalog() {
    OfficialMarketPlayer player =
        new OfficialMarketPlayer(
            1L, "Player", "DEL", "ARG", 10L, "Club", null, 5L, "Liga", null, null);
    Page<OfficialMarketPlayer> page = new PageImpl<>(List.of(player), PageRequest.of(0, 20), 1);
    OfficialMarketCatalog catalog =
        new OfficialMarketCatalog(page, new OfficialMarketPriceRange(100L, 5000L));

    OfficialMarketPageResponseDTO response = OfficialMarketMapper.toResponse(catalog);

    assertThat(response.priceRange()).isNotNull();
    assertThat(response.priceRange().min()).isEqualTo(100L);
    assertThat(response.priceRange().max()).isEqualTo(5000L);
  }

  @Test
  @DisplayName("toResponse mapea jugador individual sin cotización")
  void toResponsePlayerWithoutQuote() {
    OfficialMarketPlayer player =
        new OfficialMarketPlayer(
            2L,
            "Player Sin Cotización",
            "MED",
            "ESP",
            15L,
            "Club 2",
            null,
            6L,
            "Liga 2",
            null,
            null);

    OfficialMarketPlayerResponseDTO dto = OfficialMarketMapper.toResponse(player);

    assertThat(dto.id()).isEqualTo(2L);
    assertThat(dto.name()).isEqualTo("Player Sin Cotización");
    assertThat(dto.quote()).isNull();
  }

  @Test
  @DisplayName("toDomain transforma proyección SQL a modelo de dominio con cálculo de variación")
  void toDomainFromProjection() {
    OfficialMarketPlayerProjection projection = mock(OfficialMarketPlayerProjection.class);
    when(projection.getId()).thenReturn(10L);
    when(projection.getName()).thenReturn("Test Player");
    when(projection.getPosition()).thenReturn("DEF");
    when(projection.getNationality()).thenReturn("BRA");
    when(projection.getTeamId()).thenReturn(20L);
    when(projection.getTeamName()).thenReturn("Team X");
    when(projection.getTeamBadgeUrl()).thenReturn("https://badge.png");
    when(projection.getLeagueId()).thenReturn(3L);
    when(projection.getLeagueName()).thenReturn("League Y");
    when(projection.getImageUrl()).thenReturn("https://player.png");

    when(projection.getQuoteId()).thenReturn(100L);
    when(projection.getPrice()).thenReturn(2000L);
    when(projection.getPreviousPrice()).thenReturn(1000L);
    when(projection.getVariationPercentage()).thenReturn(null); // Debe calcularse: +100.0%
    when(projection.getScore()).thenReturn(7.5);
    when(projection.getMultiplier()).thenReturn(1.1);
    when(projection.getOrigin()).thenReturn("INITIAL");
    LocalDate date = LocalDate.of(2026, 11, 10);
    Instant instant = Instant.parse("2026-11-10T12:00:00Z");
    when(projection.getEffectiveDate()).thenReturn(date);
    when(projection.getCalculatedAt()).thenReturn(instant);

    OfficialMarketPlayer player = OfficialMarketMapper.toDomain(projection);

    assertThat(player.id()).isEqualTo(10L);
    assertThat(player.name()).isEqualTo("Test Player");
    assertThat(player.position()).isEqualTo("DEF");
    assertThat(player.nationality()).isEqualTo("BRA");
    assertThat(player.teamId()).isEqualTo(20L);
    assertThat(player.teamName()).isEqualTo("Team X");
    assertThat(player.teamBadgeUrl()).isEqualTo("https://badge.png");
    assertThat(player.leagueId()).isEqualTo(3L);
    assertThat(player.leagueName()).isEqualTo("League Y");
    assertThat(player.imageUrl()).isEqualTo("https://player.png");

    assertThat(player.quote()).isNotNull();
    assertThat(player.quote().price()).isEqualTo(2000L);
    assertThat(player.quote().previousPrice()).isEqualTo(1000L);
    assertThat(player.quote().variationPercentage()).isEqualTo(100.0);
    assertThat(player.quote().performanceScore()).isEqualTo(7.5);
    assertThat(player.quote().multiplier()).isEqualTo(1.1);
    assertThat(player.quote().calculationOrigin()).isEqualTo("INITIAL");
    assertThat(player.quote().effectiveDate()).isEqualTo(date);
    assertThat(player.quote().calculatedAt()).isEqualTo(instant);
  }

  @Test
  @DisplayName("toDomain asigna variación 0.0 cuando el precio es idéntico al anterior")
  void toDomainEqualPriceZeroVariation() {
    OfficialMarketPlayerProjection projection = mock(OfficialMarketPlayerProjection.class);
    when(projection.getId()).thenReturn(11L);
    when(projection.getQuoteId()).thenReturn(101L);
    when(projection.getPrice()).thenReturn(1500L);
    when(projection.getPreviousPrice()).thenReturn(1500L);

    OfficialMarketPlayer player = OfficialMarketMapper.toDomain(projection);

    assertThat(player.quote()).isNotNull();
    assertThat(player.quote().variationPercentage()).isEqualTo(0.0);
  }

  @Test
  @DisplayName("toDomain asigna quote null cuando la proyección no tiene cotización")
  void toDomainWithoutQuote() {
    OfficialMarketPlayerProjection projection = mock(OfficialMarketPlayerProjection.class);
    when(projection.getId()).thenReturn(12L);
    when(projection.getQuoteId()).thenReturn(null);

    OfficialMarketPlayer player = OfficialMarketMapper.toDomain(projection);

    assertThat(player.id()).isEqualTo(12L);
    assertThat(player.quote()).isNull();
  }
}
