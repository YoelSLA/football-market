package footballmarket.features.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import footballmarket.features.market.application.impl.OfficialMarketCatalogServiceImpl;
import footballmarket.features.market.domain.records.OfficialMarketPlayer;
import footballmarket.features.market.infraestructure.OfficialMarketRepository;
import footballmarket.features.market.infraestructure.projections.OfficialMarketPlayerProjection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

@Tag("unit")
class OfficialMarketCatalogServiceTest {

  private OfficialMarketRepository repository;
  private OfficialMarketCatalogServiceImpl service;

  @BeforeEach
  void setUp() {
    repository = mock(OfficialMarketRepository.class);
    service = new OfficialMarketCatalogServiceImpl(repository);
  }

  @Nested
  @DisplayName("Estrategia desacoplada de conteo y optimización de consultas")
  class CountOptimization {

    @Test
    @DisplayName("Usa conteo ligero sin cotizaciones cuando no hay filtros financieros")
    void usesLightweightCountWithoutFinancialFilters() {
      // Arrange
      OfficialMarketQuery query =
          new OfficialMarketQuery(
              0, 20, "id", "asc", "Messi", 12L, 45L, "DEL", "Argentina", null, null, null);
      when(repository.countOfficialMarketPlayersWithoutFinancialFilters(
              "Messi", 12L, 45L, "DEL", "Argentina"))
          .thenReturn(1L);

      OfficialMarketPlayerProjection projection = mock(OfficialMarketPlayerProjection.class);
      when(projection.getId()).thenReturn(10L);
      when(projection.getName()).thenReturn("Messi");
      when(repository.findOfficialMarketPlayers(
              "Messi", 12L, 45L, "DEL", "Argentina", null, null, null, "id", "asc", 20, 0))
          .thenReturn(List.of(projection));

      // Act
      Page<OfficialMarketPlayer> result = service.getOfficialCatalog(query);

      // Assert
      assertThat(result.getTotalElements()).isEqualTo(1L);
      verify(repository)
          .countOfficialMarketPlayersWithoutFinancialFilters("Messi", 12L, 45L, "DEL", "Argentina");
      verify(repository, never())
          .countOfficialMarketPlayersWithFinancialFilters(
              any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Usa conteo con cotizaciones cuando se filtran rangos de precio o disponibilidad")
    void usesQuoteAwareCountWhenFinancialFiltersPresent() {
      // Arrange
      OfficialMarketQuery query =
          new OfficialMarketQuery(
              0, 20, "price", "desc", null, null, null, null, null, true, 1000L, 50000L);
      when(repository.countOfficialMarketPlayersWithFinancialFilters(
              null, null, null, null, null, true, 1000L, 50000L))
          .thenReturn(0L);

      // Act
      Page<OfficialMarketPlayer> result = service.getOfficialCatalog(query);

      // Assert
      assertThat(result.getTotalElements()).isZero();
      assertThat(result.getContent()).isEmpty();
      verify(repository)
          .countOfficialMarketPlayersWithFinancialFilters(
              null, null, null, null, null, true, 1000L, 50000L);
      verify(repository, never())
          .countOfficialMarketPlayersWithoutFinancialFilters(any(), any(), any(), any(), any());
      verify(repository, never())
          .findOfficialMarketPlayers(
              any(),
              any(),
              any(),
              any(),
              any(),
              any(),
              any(),
              any(),
              anyString(),
              anyString(),
              anyInt(),
              anyInt());
    }

    @Test
    @DisplayName(
        "Devuelve lista vacía sin consultar datos cuando el offset supera el total de elementos")
    void returnsEmptyListWhenOffsetExceedsTotalElements() {
      // Arrange
      OfficialMarketQuery query =
          new OfficialMarketQuery(
              5, 20, "id", "asc", null, null, null, null, null, null, null, null);
      when(repository.countOfficialMarketPlayersWithoutFinancialFilters(
              null, null, null, null, null))
          .thenReturn(15L);

      // Act
      Page<OfficialMarketPlayer> result = service.getOfficialCatalog(query);

      // Assert
      assertThat(result.getTotalElements()).isEqualTo(15L);
      assertThat(result.getContent()).isEmpty();
      verify(repository, never())
          .findOfficialMarketPlayers(
              any(),
              any(),
              any(),
              any(),
              any(),
              any(),
              any(),
              any(),
              anyString(),
              anyString(),
              anyInt(),
              anyInt());
    }
  }

  @Nested
  @DisplayName("Mapeo de proyecciones a modelos de dominio")
  class DomainMapping {

    @Test
    @DisplayName("Mapea correctamente los campos deportivos y de cotización consolidados")
    void mapsSportsAndQuoteFields() {
      // Arrange
      OfficialMarketQuery query =
          new OfficialMarketQuery(
              0, 10, "id", "asc", null, null, null, null, null, null, null, null);
      when(repository.countOfficialMarketPlayersWithoutFinancialFilters(
              null, null, null, null, null))
          .thenReturn(1L);

      OfficialMarketPlayerProjection projection = mock(OfficialMarketPlayerProjection.class);
      when(projection.getId()).thenReturn(100L);
      when(projection.getName()).thenReturn("Kylian Mbappé");
      when(projection.getPosition()).thenReturn("DEL");
      when(projection.getNationality()).thenReturn("Francia");
      when(projection.getTeamId()).thenReturn(1L);
      when(projection.getTeamName()).thenReturn("Real Madrid");
      when(projection.getTeamBadgeUrl()).thenReturn(null);
      when(projection.getLeagueId()).thenReturn(2L);
      when(projection.getLeagueName()).thenReturn("La Liga");
      when(projection.getImageUrl()).thenReturn("https://img/mbappe.png");
      when(projection.getQuoteId()).thenReturn(501L);
      when(projection.getPrice()).thenReturn(30000L);
      when(projection.getPreviousPrice()).thenReturn(28000L);
      when(projection.getVariationPercentage()).thenReturn(7.14);
      when(projection.getScore()).thenReturn(9.10);
      when(projection.getMultiplier()).thenReturn(1.30);
      when(projection.getOrigin()).thenReturn("RECOMPUTED");
      when(projection.getEffectiveDate()).thenReturn(LocalDate.of(2026, 11, 12));
      when(projection.getCalculatedAt()).thenReturn(Instant.parse("2026-11-12T12:00:00Z"));

      when(repository.findOfficialMarketPlayers(
              null, null, null, null, null, null, null, null, "id", "asc", 10, 0))
          .thenReturn(List.of(projection));

      // Act
      Page<OfficialMarketPlayer> result = service.getOfficialCatalog(query);

      // Assert
      assertThat(result.getContent()).hasSize(1);
      OfficialMarketPlayer player = result.getContent().get(0);
      assertThat(player.id()).isEqualTo(100L);
      assertThat(player.name()).isEqualTo("Kylian Mbappé");
      assertThat(player.quote()).isNotNull();
      assertThat(player.quote().price()).isEqualTo(30000L);
      assertThat(player.quote().previousPrice()).isEqualTo(28000L);
      assertThat(player.quote().variationPercentage()).isEqualTo(7.14);
      assertThat(player.quote().performanceScore()).isEqualTo(9.10);
      assertThat(player.quote().multiplier()).isEqualTo(1.30);
      assertThat(player.quote().calculationOrigin()).isEqualTo("RECOMPUTED");
    }
  }
}
