package footballmarket.features.identity.domain.records;

import footballmarket.features.identity.domain.enums.PlayerIdentityPassType;
import footballmarket.features.identity.domain.enums.PlayerIdentityRunStatus;
import footballmarket.features.identity.domain.models.PlayerIdentityLeagueDiscovery;
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
