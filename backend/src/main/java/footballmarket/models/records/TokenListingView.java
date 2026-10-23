package footballmarket.models.records;

import footballmarket.models.enums.TokenListingStatus;
import java.time.OffsetDateTime;

/** Instantánea del dominio con estado efectivo calculado una sola vez para la consulta. */
public record TokenListingView(
    Long listingId,
    Long sellerId,
    Long playerId,
    int quantity,
    long unitPrice,
    long totalAmount,
    TokenListingStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt,
    OffsetDateTime closedAt) {}
