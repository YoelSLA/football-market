package footballmarket.models.records;

import footballmarket.models.enums.ExtractionRunStatus;
import footballmarket.models.enums.MetricSource;
import java.time.Instant;
import java.time.LocalDate;

public record PlayerStatisticsSyncSummary(
    Long runId,
    MetricSource source,
    LocalDate extractionDate,
    ExtractionRunStatus status,
    Instant startedAt,
    Instant finishedAt,
    PlayerStatisticsSyncCounters counters,
    long providerCalls,
    boolean coverageComplete) {}
