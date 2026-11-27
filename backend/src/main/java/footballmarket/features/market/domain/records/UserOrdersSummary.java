package footballmarket.features.market.domain.records;

/** Métricas agregadas de las publicaciones de un vendedor. */
public record UserOrdersSummary(
    int activeOrders,
    int activeTokens,
    long activeValue,
    int soldOrders,
    int soldTokens,
    long soldValue,
    int cancelledOrders,
    int expiredOrders,
    int totalOrders) {}
