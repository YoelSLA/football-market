package footballmarket.features.market.domain.records;

import org.springframework.data.domain.Page;

/** Catálogo paginado del Mercado P2P con rango de precios global. */
public record P2PMarketCatalog(Page<P2PMarketPlayer> players, P2PMarketPriceRange priceRange) {}
