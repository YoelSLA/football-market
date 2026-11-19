package footballmarket.features.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.infraestructure.UserRepository;
import footballmarket.features.market.application.impl.P2PMarketCatalogServiceImpl;
import footballmarket.features.market.domain.records.P2PMarketCatalog;
import footballmarket.features.market.infraestructure.P2PMarketRepository;
import footballmarket.features.market.infraestructure.projections.P2PMarketPlayerProjection;
import footballmarket.features.market.infraestructure.projections.P2PMarketPriceRangeProjection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("lightweight")
class P2PMarketCatalogServiceTest {

  private UserRepository users;
  private P2PMarketRepository p2pRepository;
  private Clock clock;
  private P2PMarketCatalogService service;

  @BeforeEach
  void setUp() {
    users = mock(UserRepository.class);
    p2pRepository = mock(P2PMarketRepository.class);
    clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneId.of("UTC"));
    service = new P2PMarketCatalogServiceImpl(users, p2pRepository, clock);
  }

  @Test
  @DisplayName("Retorna catálogo vacío cuando no hay elementos que coincidan")
  void emptyCatalogWhenNoElements() {
    User viewer = mock(User.class);
    when(viewer.getId()).thenReturn(99L);
    when(users.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));

    P2PMarketPriceRangeProjection priceRange = mock(P2PMarketPriceRangeProjection.class);
    when(priceRange.getMin()).thenReturn(500L);
    when(priceRange.getMax()).thenReturn(5000L);
    when(p2pRepository.findGlobalP2PMarketPriceRange(eq(99L), any())).thenReturn(priceRange);

    when(p2pRepository.countP2PMarketPlayers(
            eq(99L), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(0L);

    P2PMarketQuery query =
        new P2PMarketQuery(0, 8, "price", "asc", null, null, null, null, null, null);

    P2PMarketCatalog catalog = service.getP2PCatalog("viewer@example.com", query);

    assertThat(catalog).isNotNull();
    assertThat(catalog.players().getContent()).isEmpty();
    assertThat(catalog.players().getTotalElements()).isZero();
    assertThat(catalog.priceRange().min()).isEqualTo(500L);
    assertThat(catalog.priceRange().max()).isEqualTo(5000L);
  }

  @Test
  @DisplayName("Retorna jugadores proyectados cuando existen coincidencias")
  void catalogWithPlayers() {
    User viewer = mock(User.class);
    when(viewer.getId()).thenReturn(1L);
    when(users.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));

    when(p2pRepository.countP2PMarketPlayers(
            eq(1L), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1L);

    P2PMarketPlayerProjection projection = mock(P2PMarketPlayerProjection.class);
    when(projection.getId()).thenReturn(10L);
    when(projection.getName()).thenReturn("Test Player");
    when(projection.getPosition()).thenReturn("DEL");
    when(projection.getListingCount()).thenReturn(2L);
    when(projection.getAvailableQuantity()).thenReturn(15L);
    when(projection.getMinimumUnitPrice()).thenReturn(1200L);

    when(p2pRepository.findP2PMarketPlayers(
            eq(1L),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            eq("price"),
            eq("asc"),
            eq(8),
            eq(0)))
        .thenReturn(List.of(projection));

    P2PMarketQuery query =
        new P2PMarketQuery(0, 8, "price", "asc", null, null, null, null, null, null);

    P2PMarketCatalog catalog = service.getP2PCatalog("viewer@example.com", query);

    assertThat(catalog.players().getContent()).hasSize(1);
    assertThat(catalog.players().getContent().getFirst().name()).isEqualTo("Test Player");
    assertThat(catalog.players().getContent().getFirst().availableQuantity()).isEqualTo(15L);
  }
}
