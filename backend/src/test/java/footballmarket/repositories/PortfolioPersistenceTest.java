package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.market.domain.models.PlayerTokenEmission;
import footballmarket.features.market.infraestructure.projections.HoldingQuantity;
import footballmarket.features.market.infraestructure.projections.ReservedQuantity;
import footballmarket.support.MarketTestData;
import footballmarket.support.PortfolioTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Consultas de conjunto que sostienen la consulta del portfolio.
 *
 * <p>Esta categoría observa la base de datos: afirma el contenido persistido y la existencia del
 * índice por propietario, que la consulta de portfolio necesita.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("integration")
class PortfolioPersistenceTest {

  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-18T10:00:00Z");

  @Autowired private PlayerTokenRepository holdings;
  @Autowired private TokenListingRepository listings;
  @Autowired private PlayerRepository players;
  @Autowired private PortfolioTestData data;
  @Autowired private MarketTestData market;

  @Nested
  @DisplayName("Tenencias del propietario")
  class Tenencias {

    @Test
    @DisplayName("Devuelve las tenencias del propietario ordenadas por jugador")
    void devuelveTenenciasDelPropietario() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player first = data.createVisiblePlayer();
      Player second = data.createVisiblePlayer();
      data.buy(buyer, seller, first, 10, 100L, NOW);
      data.buy(buyer, seller, second, 20, 100L, NOW);
      // Act
      List<HoldingQuantity> result = holdings.findHoldingsByOwner(buyer.getId());
      // Assert
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getPlayerId()).isEqualTo(first.getId());
      assertThat(result.get(0).getQuantity()).isEqualTo(10);
      assertThat(result.get(1).getPlayerId()).isEqualTo(second.getId());
      assertThat(result.get(1).getQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("No devuelve las tenencias de otro propietario")
    void aíslaElPropietario() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, seller, player, 10, 100L, NOW);
      // Act
      List<HoldingQuantity> result = holdings.findHoldingsByOwner(seller.getId());
      // Assert: la cantidad devuelta es la del propietario consultado y no la del comprador
      assertThat(result)
          .filteredOn(holding -> holding.getPlayerId().equals(player.getId()))
          .extracting(HoldingQuantity::getQuantity)
          .containsExactly(PlayerTokenEmission.EMITTED_TOKENS - 10);
      assertThat(holdings.findHoldingsByOwner(buyer.getId()))
          .filteredOn(holding -> holding.getPlayerId().equals(player.getId()))
          .extracting(HoldingQuantity::getQuantity)
          .containsExactly(10);
    }
  }

  @Nested
  @DisplayName("Reservas del propietario")
  class Reservas {

    @Test
    @DisplayName("Agrupa las reservas vigentes por jugador y excluye las no vigentes")
    void agrupaReservasVigentes() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, seller, player, 10, 100L, NOW);
      data.publish(buyer, player, 4, NOW);
      // Act
      Map<Long, Long> reserved =
          listings.reservedQuantitiesByPlayer(buyer.getId(), NOW).stream()
              .collect(
                  Collectors.toMap(ReservedQuantity::getPlayerId, ReservedQuantity::getQuantity));
      // Assert
      assertThat(reserved).containsEntry(player.getId(), 4L);
    }

    @Test
    @DisplayName("No reserva nada cuando el propietario no tiene publicaciones vigentes")
    void sinReservas() {
      // Arrange
      User buyer = data.registerBuyer();
      // Act
      List<ReservedQuantity> result = listings.reservedQuantitiesByPlayer(buyer.getId(), NOW);
      // Assert
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Jugadores por lote")
  class Jugadores {

    @Test
    @DisplayName("Devuelve los jugadores indicados con su equipo disponible")
    void devuelveJugadoresConEquipo() {
      // Arrange
      Player first = market.createVisiblePlayer();
      Player second = market.createVisiblePlayer();
      Player inactive = market.createNotVisiblePlayer();
      // Act
      List<Player> result =
          players.findAllByIdInWithTeam(List.of(first.getId(), second.getId(), inactive.getId()));
      // Assert
      assertThat(result).hasSize(3);
      assertThat(result)
          .extracting(Player::getId)
          .containsExactlyInAnyOrder(first.getId(), second.getId(), inactive.getId());
      assertThat(result).allSatisfy(player -> assertThat(player.getTeam()).isNotNull());
      assertThat(result)
          .filteredOn(player -> player.getId().equals(inactive.getId()))
          .allSatisfy(player -> assertThat(player.isActive()).isFalse());
    }
  }
}
