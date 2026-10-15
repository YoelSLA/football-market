package footballmarket.models.records;

import footballmarket.models.PlayerIdentityLeagueDiscovery;
import footballmarket.models.enums.PlayerIdentityPassType;
import footballmarket.models.enums.PlayerIdentityRunStatus;
import java.time.Instant;
import java.util.List;

/** Resumen interno de una ejecución registrada, sin detalle individual ni datos deportivos. */
public record PlayerIdentitySyncSummary(
    Long id,
    PlayerIdentityPassType passType,
    Long sourceRunId,
    Instant startedAt,
    Instant finishedAt,
    PlayerIdentityRunStatus status,
    String failureReason,
    PlayerIdentitySyncCounters counters,
    List<PlayerIdentityLeagueDiscovery> leagues) {
  public PlayerIdentitySyncSummary {
    leagues = List.copyOf(leagues);
  }
}
