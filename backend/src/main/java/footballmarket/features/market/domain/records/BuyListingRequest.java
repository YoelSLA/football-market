package footballmarket.features.market.domain.records;

/** Intención de adquirir una publicación completa, sin precio, cantidad ni vendedor elegibles. */
public record BuyListingRequest(Long listingId) {}
