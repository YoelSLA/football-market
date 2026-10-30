package footballmarket.features.portfolio.application;

import footballmarket.features.portfolio.domain.records.PortfolioView;

/** Consulta de solo lectura del portfolio personal del usuario autenticado. */
public interface PortfolioService {

  /**
   * Devuelve el portfolio del usuario: resumen global del conjunto completo, posiciones de la
   * página solicitada y metadatos de esa paginación.
   *
   * <p>El resumen representa siempre el portfolio completo y es independiente del filtro y de la
   * página. El filtro admite únicamente los estados {@code OPEN} y {@code CLOSED}; sin filtro se
   * devuelven todas las posiciones. Las cifras contables proceden del componente contable de
   * REQ-S2-20 y no se recalculan aquí.
   *
   * @param userEmail correo del usuario de la sesión
   * @param page número de página desde cero
   * @param size tamaño de página, entre 1 y 100
   * @param status filtro opcional de estado de posición
   * @return vista del portfolio con resumen, página de posiciones y metadatos
   */
  PortfolioView portfolio(String userEmail, int page, int size, String status);
}
