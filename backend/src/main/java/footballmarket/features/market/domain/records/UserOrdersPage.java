package footballmarket.features.market.domain.records;

import java.util.List;

/** Resultado paginado de publicaciones de un vendedor con resumen métrico. */
public record UserOrdersPage(
    UserOrdersSummary summary,
    List<TokenListingView> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {}
