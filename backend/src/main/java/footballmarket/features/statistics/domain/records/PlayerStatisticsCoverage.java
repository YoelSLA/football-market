package footballmarket.features.statistics.domain.records;

/** Población y pendientes internos necesarios para cerrar una sincronización. */
public record PlayerStatisticsCoverage(
    long eligiblePlayers,
    long excludedWithoutReference,
    long pendingPlayers,
    boolean coverageComplete) {}
