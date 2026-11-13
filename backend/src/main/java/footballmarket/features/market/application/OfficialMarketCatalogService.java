package footballmarket.features.market.application;

import footballmarket.features.market.domain.records.OfficialMarketPlayer;
import org.springframework.data.domain.Page;

/** Servicio de aplicación para la consulta consolidada del catálogo del Mercado Oficial. */
public interface OfficialMarketCatalogService {

  /**
   * Obtiene la página de jugadores activos con su cotización vigente integrada conforme a los
   * criterios.
   *
   * @param query criterios normalizados de búsqueda, filtrado, ordenamiento y paginación
   * @return página con los jugadores elegibles y sus cotizaciones consolidadas
   */
  Page<OfficialMarketPlayer> getOfficialCatalog(OfficialMarketQuery query);
}
