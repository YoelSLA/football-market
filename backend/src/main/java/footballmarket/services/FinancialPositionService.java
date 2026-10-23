package footballmarket.services;

import footballmarket.models.PlayerFinancialPosition;
import java.util.List;

/**
 * Fachada contable de solo lectura, reutilizable sin endpoint ni conocimiento del portfolio HTTP.
 */
public interface FinancialPositionService {
  /**
   * Reconstruye y valora posiciones del sujeto resuelto por el consumidor desde su sesión. Requiere
   * REPEATABLE READ también en una transacción envolvente del consumidor.
   *
   * @param userId identidad de confianza, nunca recibida directamente del cliente
   * @return conjunto completo e inmutable de posiciones, ordenadas por jugador
   */
  List<PlayerFinancialPosition> positionsOf(Long userId);
}
