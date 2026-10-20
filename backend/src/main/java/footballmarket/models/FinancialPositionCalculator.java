package footballmarket.models;

import footballmarket.models.exceptions.FinancialPositionIntegrityException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Reconstruye y agrega posiciones sin persistencia ni acceso a proveedores. */
public final class FinancialPositionCalculator {
  private FinancialPositionCalculator() {}

  /** Cifras financieras globales sobre el conjunto completo, no sobre una página. */
  public record Summary(
      long historicalCost,
      long investedCost,
      Long currentValue,
      long realizedProfitLoss,
      Long unrealizedProfitLoss,
      Long totalProfitLoss,
      BigDecimal profitLossPercentage,
      boolean valuationComplete) {}

  /**
   * Reconstruye el historial completo de una parte. La emisión se aplica como cantidad de costo
   * cero; las operaciones se ordenan por instante real e identidad. Rechaza registros duplicados,
   * ajenos o incoherentes en lugar de entregar posiciones incorrectas.
   */
  public static List<PlayerFinancialPosition> reconstruct(
      Long userId, List<MarketTransaction> history, List<PlayerTokenEmission> emissions) {
    if (userId == null || history == null || emissions == null) {
      throw new FinancialPositionIntegrityException();
    }
    Map<Long, PlayerFinancialPosition> positions = new TreeMap<>();
    for (PlayerTokenEmission emission : emissions) {
      if (emission == null
          || emission.getPlayerId() == null
          || emission.getOwner() == null
          || !userId.equals(emission.getOwner().getId())
          || positions.containsKey(emission.getPlayerId())) {
        throw new FinancialPositionIntegrityException();
      }
      positions.put(
          emission.getPlayerId(),
          PlayerFinancialPosition.empty(emission.getPlayerId())
              .issued(emission.getInitialQuantity(), emission.getEmittedAt()));
    }
    Set<Long> seen = new HashSet<>();
    for (MarketTransaction transaction : history) {
      validateTransaction(userId, transaction);
      if (!seen.add(transaction.getId())) {
        throw new FinancialPositionIntegrityException();
      }
    }
    // OffsetDateTime.compareTo incluye la zona: comparar Instant evita desempates artificiales.
    List<MarketTransaction> sorted =
        history.stream()
            .sorted(
                Comparator.comparing((MarketTransaction t) -> t.getExecutedAt().toInstant())
                    .thenComparing(MarketTransaction::getId))
            .toList();
    for (MarketTransaction transaction : sorted) {
      Long playerId = transaction.getPlayer().getId();
      PlayerFinancialPosition position =
          positions.getOrDefault(playerId, PlayerFinancialPosition.empty(playerId));
      PlayerFinancialPosition next =
          userId.equals(transaction.getBuyer().getId())
              ? position.purchase(
                  transaction.getQuantity(),
                  transaction.getTotalAmount(),
                  transaction.getExecutedAt())
              : position.sale(
                  transaction.getQuantity(),
                  transaction.getTotalAmount(),
                  transaction.getExecutedAt());
      positions.put(playerId, next);
    }
    return List.copyOf(positions.values());
  }

  /** Suma cifras autoritativas y convierte a puntos porcentuales antes de redondear. */
  public static Summary aggregate(List<PlayerFinancialPosition> positions) {
    long paid = 0;
    long invested = 0;
    long realized = 0;
    long current = 0;
    long unrealized = 0;
    boolean complete = true;
    Set<Long> players = new HashSet<>();
    if (positions == null) {
      throw new FinancialPositionIntegrityException();
    }
    try {
      for (PlayerFinancialPosition position : positions) {
        if (position == null || !players.add(position.playerId())) {
          throw new FinancialPositionIntegrityException();
        }
        paid = Math.addExact(paid, position.totalPaid());
        invested = Math.addExact(invested, position.acquisitionCost());
        realized = Math.addExact(realized, position.realizedProfitLoss());
        if (position.isOpen()) {
          Long value = position.currentValue();
          if (value == null) {
            complete = false;
          } else {
            current = Math.addExact(current, value);
            unrealized = Math.addExact(unrealized, position.unrealizedProfitLoss());
          }
        }
      }
      Long total = complete ? Math.addExact(realized, unrealized) : null;
      BigDecimal percentage =
          !complete || paid == 0
              ? null
              : BigDecimal.valueOf(total)
                  .multiply(BigDecimal.valueOf(100))
                  .divide(BigDecimal.valueOf(paid), 2, RoundingMode.HALF_UP);
      return new Summary(
          paid,
          invested,
          complete ? current : null,
          realized,
          complete ? unrealized : null,
          total,
          percentage,
          complete);
    } catch (ArithmeticException overflow) {
      throw new FinancialPositionIntegrityException();
    }
  }

  private static void validateTransaction(Long userId, MarketTransaction transaction) {
    if (transaction == null
        || transaction.getId() == null
        || transaction.getExecutedAt() == null
        || transaction.getPlayer() == null
        || transaction.getPlayer().getId() == null
        || transaction.getBuyer() == null
        || transaction.getBuyer().getId() == null
        || transaction.getSeller() == null
        || transaction.getSeller().getId() == null
        || transaction.getBuyer().getId().equals(transaction.getSeller().getId())
        || (!userId.equals(transaction.getBuyer().getId())
            && !userId.equals(transaction.getSeller().getId()))
        || transaction.getQuantity() <= 0
        || transaction.getUnitPrice() <= 0) {
      throw new FinancialPositionIntegrityException();
    }
    try {
      if (Math.multiplyExact((long) transaction.getQuantity(), transaction.getUnitPrice())
          != transaction.getTotalAmount()) {
        throw new FinancialPositionIntegrityException();
      }
    } catch (ArithmeticException overflow) {
      throw new FinancialPositionIntegrityException();
    }
  }
}
