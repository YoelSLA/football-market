package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.domain.enums.UserRole;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.models.PlayerTokenEmission;
import footballmarket.features.portfolio.domain.exceptions.FinancialPositionIntegrityException;
import footballmarket.features.portfolio.domain.models.FinancialPositionCalculator;
import footballmarket.features.portfolio.domain.models.PlayerFinancialPosition;
import footballmarket.features.quote.domain.records.CurrentQuote;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Reconstrucción por rol, orden total y agregados sobre posiciones completas. */
@Tag("unit")
class FinancialPositionCalculatorTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-20T12:00:00Z");

  @Nested
  @DisplayName("Reconstrucción trazable")
  class Replay {
    @Test
    @DisplayName("Ordena empates por identidad y distingue compras y ventas de la misma parte")
    void ordenaHistorial() throws ReflectiveOperationException {
      // Arrange
      MarketTransaction buy = operation(1L, 1L, 2L, 10, 100);
      MarketTransaction sell = operation(2L, 3L, 1L, 5, 200);
      List<MarketTransaction> history = List.of(sell, buy);
      // Act
      List<PlayerFinancialPosition> result =
          FinancialPositionCalculator.reconstruct(1L, history, List.of());
      // Assert
      assertThat(result).hasSize(1);
      assertThat(result.getFirst().acquisitionCost()).isEqualTo(500);
      assertThat(result.getFirst().realizedProfitLoss()).isEqualTo(500);
      assertThat(result.getFirst().buyOperationCount()).isEqualTo(1);
      assertThat(result.getFirst().sellOperationCount()).isEqualTo(1);
      assertThat(FinancialPositionCalculator.reconstruct(1L, history, List.of())).isEqualTo(result);
    }

    @Test
    @DisplayName(
        "La emisión no crea operaciones y la venta de su propietario realiza todo el ingreso")
    void aplicaEmisionAntesDeVenta() throws ReflectiveOperationException {
      // Arrange
      MarketTransaction sale = operation(1L, 1L, 2L, 10, 100);
      User issuer = user(2L);
      PlayerTokenEmission emission =
          PlayerTokenEmission.restore(10L, issuer, 100, NOW.minusDays(1));
      List<PlayerTokenEmission> emissions = List.of(emission);
      List<MarketTransaction> history = List.of(sale);
      // Act
      PlayerFinancialPosition result =
          FinancialPositionCalculator.reconstruct(2L, history, emissions).getFirst();
      // Assert
      assertThat(result.openQuantity()).isEqualTo(90);
      assertThat(result.totalPaid()).isZero();
      assertThat(result.realizedProfitLoss()).isEqualTo(1000);
      assertThat(result.openedAt()).isEqualTo(emission.getEmittedAt());
      assertThat(result.buyOperationCount()).isZero();
    }

    @Test
    @DisplayName("Rechaza doble registro e importes incoherentes en lugar de duplicar resultados")
    void rechazaDatosIncoherentes() throws ReflectiveOperationException {
      // Arrange
      MarketTransaction operation = operation(1L, 1L, 2L, 10, 100);
      List<MarketTransaction> repeated = List.of(operation, operation);
      Field amount = MarketTransaction.class.getDeclaredField("totalAmount");
      amount.setAccessible(true);
      // Act / Assert
      assertThatThrownBy(() -> FinancialPositionCalculator.reconstruct(1L, repeated, List.of()))
          .isInstanceOf(FinancialPositionIntegrityException.class);
      amount.setLong(operation, 999);
      assertThatThrownBy(
              () -> FinancialPositionCalculator.reconstruct(1L, List.of(operation), List.of()))
          .isInstanceOf(FinancialPositionIntegrityException.class);
    }
  }

  @Nested
  @DisplayName("Agregación financiera completa")
  class Aggregation {
    @Test
    @DisplayName("Multiplica por cien antes de redondear y distingue costo histórico y remanente")
    void calculaTercioSinRedondeoIntermedio() {
      // Arrange
      PlayerFinancialPosition closed =
          PlayerFinancialPosition.empty(1L).purchase(1, 3, NOW).sale(1, 4, NOW);
      List<PlayerFinancialPosition> all = List.of(closed);
      // Act
      FinancialPositionCalculator.Summary result = FinancialPositionCalculator.aggregate(all);
      // Assert
      assertThat(result.historicalCost()).isEqualTo(3);
      assertThat(result.investedCost()).isZero();
      assertThat(result.realizedProfitLoss()).isEqualTo(1);
      assertThat(result.currentValue()).isZero();
      assertThat(result.profitLossPercentage()).isEqualByComparingTo("33.33");
      assertThat(result.valuationComplete()).isTrue();
    }

    @Test
    @DisplayName("La falta de una cotización invalida los agregados dependientes, no el realizado")
    void propagaAusencia() {
      // Arrange
      PlayerFinancialPosition missing = PlayerFinancialPosition.empty(1L).purchase(1, 100, NOW);
      CurrentQuote quote = new CurrentQuote(2L, 150, NOW, null);
      PlayerFinancialPosition valued =
          PlayerFinancialPosition.empty(2L).purchase(1, 100, NOW).valuedAt(quote);
      List<PlayerFinancialPosition> all = List.of(missing, valued);
      // Act
      FinancialPositionCalculator.Summary result = FinancialPositionCalculator.aggregate(all);
      // Assert
      assertThat(result.historicalCost()).isEqualTo(200);
      assertThat(result.investedCost()).isEqualTo(200);
      assertThat(result.realizedProfitLoss()).isZero();
      assertThat(result.currentValue()).isNull();
      assertThat(result.unrealizedProfitLoss()).isNull();
      assertThat(result.totalProfitLoss()).isNull();
      assertThat(result.profitLossPercentage()).isNull();
      assertThat(result.valuationComplete()).isFalse();
    }

    @Test
    @DisplayName("El conjunto vacío no necesita cotización y su porcentaje no tiene denominador")
    void admiteVacio() {
      // Arrange / Act
      FinancialPositionCalculator.Summary result = FinancialPositionCalculator.aggregate(List.of());
      // Assert
      assertThat(result.currentValue()).isZero();
      assertThat(result.historicalCost()).isZero();
      assertThat(result.profitLossPercentage()).isNull();
      assertThat(result.valuationComplete()).isTrue();
    }
  }

  private static User user(Long id) {
    return new User(id, "user" + id, "user" + id + "@example.com", "password123", UserRole.USER);
  }

  /** Construye auditorías en memoria con identidad persistida, sin Spring ni mocks de dominio. */
  private static MarketTransaction operation(
      Long id, Long buyerId, Long sellerId, int quantity, long price)
      throws ReflectiveOperationException {
    League league = new League("Liga");
    Team team = new Team("Equipo", league, true);
    Player player = new Player("Jugador", team, "Forward");
    Field playerId = Player.class.getDeclaredField("id");
    playerId.setAccessible(true);
    playerId.set(player, 10L);
    User buyer = user(buyerId);
    User seller = user(sellerId);
    MarketTransaction operation =
        MarketTransaction.record(
            buyer,
            seller,
            player,
            quantity,
            price,
            quantity * price,
            10000,
            9000,
            0,
            1000,
            0,
            quantity,
            100,
            100 - quantity,
            NOW,
            null,
            NOW);
    Field transactionId = MarketTransaction.class.getDeclaredField("id");
    transactionId.setAccessible(true);
    transactionId.set(operation, id);
    return operation;
  }
}
