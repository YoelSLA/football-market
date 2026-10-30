package footballmarket.features.portfolio.application;

import footballmarket.features.portfolio.domain.records.OperationHistoryEntry;
import java.util.List;

/** Consulta de solo lectura del historial financiero del usuario autenticado. */
public interface OperationHistoryService {

  /**
   * Devuelve todas las operaciones confirmadas de la identidad de sesión, en orden descendente.
   *
   * @param subjectEmail subject del JWT autenticado
   * @return compras y ventas propias con sus valores históricos
   */
  List<OperationHistoryEntry> history(String subjectEmail);
}
