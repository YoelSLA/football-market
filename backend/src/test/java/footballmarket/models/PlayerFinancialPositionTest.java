package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.portfolio.domain.exceptions.FinancialPositionIntegrityException;
import footballmarket.features.portfolio.domain.models.PlayerFinancialPosition;
import footballmarket.features.quote.domain.records.CurrentQuote;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Invariantes contables, sin Spring, persistencia ni proveedores. */
@Tag("unit")
class PlayerFinancialPositionTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-20T12:00:00Z");

  @Nested
  @DisplayName("Costo, liquidación y ciclos")
  class CostBasis {
    @Test
    @DisplayName("Pondera compras y atribuye proporcionalmente una venta parcial")
    void ponderaComprasYVenta() {
      // Arrange
      PlayerFinancialPosition start = PlayerFinancialPosition.empty(1L);
      // Act
      PlayerFinancialPosition result =
          start
              .purchase(10, 1000, NOW)
              .purchase(10, 1500, NOW.plusSeconds(1))
              .sale(5, 1000, NOW.plusSeconds(2));
      // Assert
      assertThat(result.openQuantity()).isEqualTo(15);
      assertThat(result.acquisitionCost()).isEqualTo(1875);
      assertThat(result.averageUnitPrice()).isEqualTo(125L);
      assertThat(result.totalPaid()).isEqualTo(2500);
      assertThat(result.realizedProfitLoss()).isEqualTo(375);
      assertThat(result.openedAt()).isEqualTo(NOW);
      assertThat(result.closedAt()).isNull();
      assertThat(result.buyOperationCount()).isEqualTo(2);
      assertThat(result.sellOperationCount()).isEqualTo(1);
      assertThat(start.openQuantity()).isZero();
    }

    @Test
    @DisplayName("Retiene el residuo, liquida exactamente y reabre sin perder el realizado")
    void conservaResiduoYCiclos() {
      // Arrange
      PlayerFinancialPosition start =
          PlayerFinancialPosition.empty(1L).purchase(2, 200, NOW).purchase(1, 101, NOW);
      // Act
      PlayerFinancialPosition partial = start.sale(1, 150, NOW.plusSeconds(1));
      PlayerFinancialPosition closed = partial.sale(2, 300, NOW.plusSeconds(2));
      PlayerFinancialPosition reopened = closed.purchase(4, 800, NOW.plusSeconds(3));
      // Assert
      assertThat(partial.acquisitionCost()).isEqualTo(201);
      assertThat(partial.averageUnitPrice()).isEqualTo(101L);
      assertThat(partial.realizedProfitLoss()).isEqualTo(50);
      assertThat(closed.acquisitionCost()).isZero();
      assertThat(closed.averageUnitPrice()).isNull();
      assertThat(closed.realizedProfitLoss()).isEqualTo(149);
      assertThat(closed.closedAt()).isEqualTo(NOW.plusSeconds(2));
      assertThat(closed.totalProfitLoss()).isEqualTo(149L);
      assertThat(reopened.acquisitionCost()).isEqualTo(800);
      assertThat(reopened.totalPaid()).isEqualTo(1101);
      assertThat(reopened.realizedProfitLoss()).isEqualTo(149);
      assertThat(reopened.openedAt()).isEqualTo(NOW.plusSeconds(3));
      assertThat(reopened.lastTransactionAt()).isEqualTo(NOW.plusSeconds(3));
      assertThat(reopened.closedAt()).isNull();
    }

    @Test
    @DisplayName("La emisión diluye sin incrementar compras y sus ventas realizan el ingreso")
    void emisionSinCompraFicticia() {
      // Arrange
      PlayerFinancialPosition issued = PlayerFinancialPosition.empty(1L).issued(100, NOW);
      // Act
      PlayerFinancialPosition mixed =
          issued.purchase(10, 1000, NOW.plusSeconds(1)).sale(10, 1500, NOW.plusSeconds(2));
      PlayerFinancialPosition seller = issued.sale(10, 1000, NOW.plusSeconds(1));
      // Assert
      assertThat(issued.openedAt()).isEqualTo(NOW);
      assertThat(issued.lastTransactionAt()).isNull();
      assertThat(issued.buyOperationCount()).isZero();
      assertThat(issued.sellOperationCount()).isZero();
      assertThat(mixed.acquisitionCost()).isEqualTo(909);
      assertThat(mixed.realizedProfitLoss()).isEqualTo(1409);
      assertThat(mixed.buyOperationCount()).isEqualTo(1);
      assertThat(seller.realizedProfitLoss()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Una venta por debajo del costo conserva el signo de la pérdida")
    void admitePerdidas() {
      // Arrange
      PlayerFinancialPosition position = PlayerFinancialPosition.empty(1L).purchase(2, 400, NOW);
      // Act / Assert
      assertThat(position.sale(1, 100, NOW).realizedProfitLoss()).isEqualTo(-100);
    }

    @Test
    @DisplayName("Rechaza sobreventa, cantidades inválidas e importes fuera de rango sin mutar")
    void rechazaEntradasInvalidas() {
      // Arrange
      PlayerFinancialPosition position = PlayerFinancialPosition.empty(1L).purchase(1, 100, NOW);
      // Act / Assert
      assertThatThrownBy(() -> position.sale(2, 200, NOW))
          .isInstanceOf(FinancialPositionIntegrityException.class);
      assertThatThrownBy(() -> position.purchase(-1, 100, NOW))
          .isInstanceOf(FinancialPositionIntegrityException.class);
      assertThatThrownBy(() -> position.purchase(1, 0, NOW))
          .isInstanceOf(FinancialPositionIntegrityException.class);
      assertThatThrownBy(() -> position.purchase(1, Long.MAX_VALUE, NOW))
          .isInstanceOf(FinancialPositionIntegrityException.class);
      assertThat(position.acquisitionCost()).isEqualTo(100);
    }
  }

  @Nested
  @DisplayName("Valoración de la posición")
  class Valuation {
    @Test
    @DisplayName("Conserva precio e instante y calcula los resultados sin redondear importes")
    void valoraConSnapshot() {
      // Arrange
      PlayerFinancialPosition position =
          PlayerFinancialPosition.empty(1L).purchase(20, 2500, NOW).sale(5, 1000, NOW);
      CurrentQuote quote = new CurrentQuote(1L, 180, NOW.minusHours(1), "v1");
      // Act
      PlayerFinancialPosition valued = position.valuedAt(quote);
      // Assert
      assertThat(valued.currentUnitPrice()).isEqualTo(180L);
      assertThat(valued.quoteCalculatedAt()).isEqualTo(quote.calculatedAt());
      assertThat(valued.currentValue()).isEqualTo(2700L);
      assertThat(valued.unrealizedProfitLoss()).isEqualTo(825L);
      assertThat(valued.totalProfitLoss()).isEqualTo(1200L);
      assertThat(position.currentUnitPrice()).isNull();
    }

    @Test
    @DisplayName("La ausencia de cotización no inventa ceros ni borra el resultado realizado")
    void ausenciaNoEsCero() {
      // Arrange
      PlayerFinancialPosition position =
          PlayerFinancialPosition.empty(1L).purchase(2, 200, NOW).sale(1, 150, NOW);
      // Act
      PlayerFinancialPosition unvalued = position.valuedAt(null);
      // Assert
      assertThat(unvalued.currentUnitPrice()).isNull();
      assertThat(unvalued.quoteCalculatedAt()).isNull();
      assertThat(unvalued.currentValue()).isNull();
      assertThat(unvalued.unrealizedProfitLoss()).isNull();
      assertThat(unvalued.totalProfitLoss()).isNull();
      assertThat(unvalued.realizedProfitLoss()).isEqualTo(50);
    }
  }
}
