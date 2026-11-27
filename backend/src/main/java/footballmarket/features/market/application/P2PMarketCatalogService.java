package footballmarket.features.market.application;

import footballmarket.features.market.domain.records.P2PMarketCatalog;

/**
 * Servicio de aplicación para la consulta del catálogo del Mercado P2P con publicaciones agrupadas.
 */
public interface P2PMarketCatalogService {

  /**
   * Obtiene el catálogo paginado de jugadores con publicaciones activas en la comunidad.
   *
   * @param viewerEmail email del usuario solicitante, o nulo si no está autenticado
   * @param query criterios de filtrado, ordenamiento y paginación
   * @return catálogo paginado con métricas P2P agregadas
   */
  P2PMarketCatalog getP2PCatalog(String viewerEmail, P2PMarketQuery query);
}
