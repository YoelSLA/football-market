package footballmarket.features.businessmetrics.domain.records;

import java.time.Instant;

/** Totales históricos de negocio calculados desde las fuentes persistidas. */
public record BusinessMetricsTotals(
    long purchases,
    long sales,
    long cancelledListings,
    long tokensTransferred,
    long creditsTradedInCents,
    Instant queriedAt) {}
