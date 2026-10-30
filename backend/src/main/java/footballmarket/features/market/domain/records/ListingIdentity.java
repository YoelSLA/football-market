package footballmarket.features.market.domain.records;

/**
 * Lectura orientativa escalar; evita cachear la entidad que después debe revalidarse bajo bloqueo.
 */
public record ListingIdentity(Long sellerId, Long playerId) {}
