package footballmarket.features.market.domain.records;

import org.springframework.data.domain.Page;

/** Página solicitada del mercado oficial acompañada por sus extremos globales de precio. */
public record OfficialMarketCatalog(
    Page<OfficialMarketPlayer> page, OfficialMarketPriceRange priceRange) {}
