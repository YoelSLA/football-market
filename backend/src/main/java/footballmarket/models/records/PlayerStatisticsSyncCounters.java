package footballmarket.models.records;

public record PlayerStatisticsSyncCounters(
    long eligible,
    long excludedWithoutReference,
    long selected,
    long completed,
    long withoutData,
    long failed,
    long pending,
    long present,
    long absent,
    long notDerivable) {}
