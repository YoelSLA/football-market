package footballmarket.features.market.application;

import footballmarket.features.market.domain.records.OfficialMarketCatalog;

/** Servicio de aplicación para la consulta consolidada del catálogo del Mercado Oficial. */
public interface OfficialMarketCatalogService {

  /**
   * Obtiene el catálogo de jugadores activos con su cotización vigente integrada conforme a los
   * criterios y los extremos globales de cotizaciones vigentes.
   *
   * @param query criterios normalizados de búsqueda, filtrado, ordenamiento y paginación
   * @return catálogo con jugadores elegibles, cotizaciones consolidadas y rango global de precios
   */
  OfficialMarketCatalog getOfficialCatalog(OfficialMarketQuery query);
}
