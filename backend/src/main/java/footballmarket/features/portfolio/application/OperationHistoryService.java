package footballmarket.features.portfolio.application;

import footballmarket.features.portfolio.domain.records.OperationHistoryView;
import java.time.OffsetDateTime;

/** Consulta de solo lectura del historial financiero del usuario autenticado. */
public interface OperationHistoryService {

  /**
   * Devuelve una página de operaciones confirmadas y los agregados del conjunto filtrado.
   *
   * @param subjectEmail subject del JWT autenticado
   * @param page página basada en cero
   * @param size tamaño entre uno y cien
   * @param search nombre actual opcional del jugador
   * @param type tipo opcional BUY o SELL
   * @param executedFrom instante inicial inclusivo
   * @param executedBefore instante final exclusivo
   * @param sort orden cerrado del contrato
   * @return historial personal paginado
   */
  OperationHistoryView history(
      String subjectEmail,
      int page,
      int size,
      String search,
      String type,
      OffsetDateTime executedFrom,
      OffsetDateTime executedBefore,
      String sort);
}
