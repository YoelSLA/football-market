package footballmarket.features.portfolio.domain.models;

import footballmarket.features.market.domain.models.PlayerTokenEmission;
import footballmarket.features.portfolio.domain.exceptions.FinancialPositionIntegrityException;
import footballmarket.features.quote.domain.records.CurrentQuote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

/**
 * Posición contable inmutable. El costo remanente es autoritativo y el promedio es informativo. Las
 * operaciones devuelven estados nuevos, conservando costo y resultado histórico entre ciclos.
 */
public record PlayerFinancialPosition(
    Long playerId,
    int openQuantity,
    long acquisitionCost,
    long totalPaid,
    long realizedProfitLoss,
    Long currentUnitPrice,
    OffsetDateTime quoteCalculatedAt,
    OffsetDateTime openedAt,
    OffsetDateTime closedAt,
    OffsetDateTime lastTransactionAt,
    int buyOperationCount,
    int sellOperationCount) {

  /**
   * Valida el estado e impide construir una posición negativa o con una pareja precio-instante
   * incoherente.
   *
   * <p>El constructor canónico de un record público no puede estrechar su visibilidad, de modo que
   * la construcción directa queda cubierta por esta validación: cualquier combinación inválida
   * falla de inmediato. Las posiciones se construyen normalmente con las operaciones de dominio
   * {@code issued}, {@code purchase} y {@code sale}, que son la vía recomendada.
   */
  public PlayerFinancialPosition {
    if (playerId == null
        || openQuantity < 0
        || acquisitionCost < 0
        || totalPaid < acquisitionCost
        || buyOperationCount < 0
        || sellOperationCount < 0
        || (openQuantity == 0 && acquisitionCost != 0)
        || (openQuantity > 0 && (openedAt == null || closedAt != null))
        || (currentUnitPrice == null) != (quoteCalculatedAt == null)
        || (currentUnitPrice != null && (currentUnitPrice <= 0 || openQuantity == 0))) {
      throw new FinancialPositionIntegrityException();
    }
  }

  /** Estado vacío interno anterior a una emisión o primera compra. */
  public static PlayerFinancialPosition empty(Long playerId) {
    return new PlayerFinancialPosition(playerId, 0, 0, 0, 0, null, null, null, null, null, 0, 0);
  }

  /** Incorpora la emisión inicial sin crear una compra ficticia. */
  public PlayerFinancialPosition issued(int quantity, OffsetDateTime emittedAt) {
    if (quantity != PlayerTokenEmission.EMITTED_TOKENS
        || emittedAt == null
        || this.openQuantity != 0
        || this.openedAt != null) {
      throw new FinancialPositionIntegrityException();
    }
    return new PlayerFinancialPosition(
        this.playerId, quantity, 0, 0, 0, null, null, emittedAt, null, null, 0, 0);
  }

  /** Suma cantidad y costo exactos; abre un ciclo solo si no había cantidad. */
  public PlayerFinancialPosition purchase(int quantity, long amount, OffsetDateTime executedAt) {
    requireOperation(quantity, amount, executedAt);
    try {
      return new PlayerFinancialPosition(
          this.playerId,
          Math.addExact(this.openQuantity, quantity),
          Math.addExact(this.acquisitionCost, amount),
          Math.addExact(this.totalPaid, amount),
          this.realizedProfitLoss,
          null,
          null,
          this.isOpen() ? this.openedAt : executedAt,
          null,
          executedAt,
          Math.incrementExact(this.buyOperationCount),
          this.sellOperationCount);
    } catch (ArithmeticException overflow) {
      throw new FinancialPositionIntegrityException();
    }
  }

  /** Atribuye costo con HALF_UP y retiene el residuo en el costo abierto. */
  public PlayerFinancialPosition sale(int quantity, long amount, OffsetDateTime executedAt) {
    requireOperation(quantity, amount, executedAt);
    if (quantity > this.openQuantity) {
      throw new FinancialPositionIntegrityException();
    }
    try {
      long attributed =
          BigDecimal.valueOf(this.acquisitionCost)
              .multiply(BigDecimal.valueOf(quantity))
              .divide(BigDecimal.valueOf(this.openQuantity), 0, RoundingMode.HALF_UP)
              .longValueExact();
      int remaining = this.openQuantity - quantity;
      return new PlayerFinancialPosition(
          this.playerId,
          remaining,
          this.acquisitionCost - attributed,
          this.totalPaid,
          Math.addExact(this.realizedProfitLoss, Math.subtractExact(amount, attributed)),
          null,
          null,
          this.openedAt,
          remaining == 0 ? executedAt : null,
          executedAt,
          this.buyOperationCount,
          Math.incrementExact(this.sellOperationCount));
    } catch (ArithmeticException overflow) {
      throw new FinancialPositionIntegrityException();
    }
  }

  /** Conserva precio e instante del mismo snapshot; una posición cerrada no se cotiza. */
  public PlayerFinancialPosition valuedAt(CurrentQuote quote) {
    if (!this.isOpen()) {
      return this;
    }
    if (quote != null
        && (!this.playerId.equals(quote.playerId())
            || quote.price() <= 0
            || quote.calculatedAt() == null)) {
      throw new FinancialPositionIntegrityException();
    }
    if (quote != null) {
      try {
        Math.multiplyExact((long) this.openQuantity, quote.price());
      } catch (ArithmeticException overflow) {
        throw new FinancialPositionIntegrityException();
      }
    }
    return new PlayerFinancialPosition(
        this.playerId,
        this.openQuantity,
        this.acquisitionCost,
        this.totalPaid,
        this.realizedProfitLoss,
        quote == null ? null : quote.price(),
        quote == null ? null : quote.calculatedAt(),
        this.openedAt,
        this.closedAt,
        this.lastTransactionAt,
        this.buyOperationCount,
        this.sellOperationCount);
  }

  public boolean isOpen() {
    return this.openQuantity > 0;
  }

  /** Promedio informativo; no existe en una posición cerrada. */
  public Long averageUnitPrice() {
    return this.isOpen()
        ? BigDecimal.valueOf(this.acquisitionCost)
            .divide(BigDecimal.valueOf(this.openQuantity), 0, RoundingMode.HALF_UP)
            .longValueExact()
        : null;
  }

  /** Valor no definido para posición cerrada o sin cotización. */
  public Long currentValue() {
    try {
      return this.currentUnitPrice == null
          ? null
          : Math.multiplyExact((long) this.openQuantity, this.currentUnitPrice);
    } catch (ArithmeticException overflow) {
      throw new FinancialPositionIntegrityException();
    }
  }

  public Long unrealizedProfitLoss() {
    Long value = this.currentValue();
    return value == null ? null : value - this.acquisitionCost;
  }

  /** Sin cotización el total es indefinido; una posición cerrada conserva su realizado. */
  public Long totalProfitLoss() {
    if (!this.isOpen()) {
      return this.realizedProfitLoss;
    }
    Long unrealized = this.unrealizedProfitLoss();
    try {
      return unrealized == null ? null : Math.addExact(this.realizedProfitLoss, unrealized);
    } catch (ArithmeticException overflow) {
      throw new FinancialPositionIntegrityException();
    }
  }

  private static void requireOperation(int quantity, long amount, OffsetDateTime executedAt) {
    if (quantity <= 0 || amount <= 0 || executedAt == null) {
      throw new FinancialPositionIntegrityException();
    }
  }
}
