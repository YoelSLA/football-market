package footballmarket.features.identity.application.impl;

import footballmarket.features.identity.application.PlayerIdentitySynchronizationService;
import footballmarket.features.identity.application.exceptions.PlayerIdentitySynchronizationException;
import footballmarket.features.identity.application.internal.PlayerIdentityLeagueDiscoveryProcessor;
import footballmarket.features.identity.application.internal.PlayerIdentityResolutionProcessor;
import footballmarket.features.identity.application.internal.PlayerIdentitySnapshotCapture;
import footballmarket.features.identity.domain.enums.PlayerIdentityItemResult;
import footballmarket.features.identity.domain.enums.PlayerIdentityRunStatus;
import footballmarket.features.identity.domain.records.PlayerIdentitySyncSummary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Ejecuta actual e histórico en una invocación y agrega un resultado por jugador local. */
@Service
public class PlayerIdentitySynchronizationServiceImpl
    implements PlayerIdentitySynchronizationService {
  private final PlayerIdentitySnapshotCapture snapshot;
  private final PlayerIdentityLeagueDiscoveryProcessor discovery;
  private final PlayerIdentityResolutionProcessor resolution;

  public PlayerIdentitySynchronizationServiceImpl(
      PlayerIdentitySnapshotCapture snapshot,
      PlayerIdentityLeagueDiscoveryProcessor discovery,
      PlayerIdentityResolutionProcessor resolution) {
    this.snapshot = snapshot;
    this.discovery = discovery;
    this.resolution = resolution;
  }

  @Override
  public PlayerIdentitySyncSummary synchronize() {
    boolean interrupted = false;
    try {
      Pass current = this.execute(this.snapshot.current());
      interrupted = current.interrupted();
      Map<Long, PlayerIdentityItemResult> finalResults = new LinkedHashMap<>(current.results());
      PlayerIdentityRunStatus status = current.status();
      if (status != PlayerIdentityRunStatus.FAILED && !interrupted) {
        List<Long> eligible =
            finalResults.entrySet().stream()
                .filter(
                    entry ->
                        entry.getValue() == PlayerIdentityItemResult.NO_MATCH
                            || entry.getValue() == PlayerIdentityItemResult.AMBIGUOUS)
                .map(Map.Entry::getKey)
                .toList();
        if (!eligible.isEmpty()) {
          PlayerIdentitySnapshotCapture.Capture historical = this.snapshot.historical(eligible);
          eligible.forEach(id -> finalResults.put(id, PlayerIdentityItemResult.PERSISTENCE_ERROR));
          Pass complement = this.execute(historical);
          interrupted |= complement.interrupted();
          finalResults.putAll(complement.results());
          status = status.combine(complement.status());
          if (complement.results().size() < eligible.size()) {
            status = status.combine(PlayerIdentityRunStatus.PARTIAL);
          }
        }
      }
      return PlayerIdentitySyncSummary.from(status, finalResults.values());
    } catch (RuntimeException failure) {
      throw new PlayerIdentitySynchronizationException();
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private Pass execute(PlayerIdentitySnapshotCapture.Capture capture) {
    PlayerIdentityLeagueDiscoveryProcessor.Discovery discovered = this.discovery.discover(capture);
    PlayerIdentityResolutionProcessor.Resolution resolved =
        this.resolution.resolve(capture, discovered);
    boolean needsDiscovery =
        capture.items().stream().anyMatch(item -> item.getInitialExternalIds().isEmpty());
    PlayerIdentityRunStatus status =
        PlayerIdentityRunStatus.forPass(
            resolved.results().values(),
            needsDiscovery ? capture.competitions().size() : 0,
            discovered.completedLeagues().size(),
            resolved.interrupted());
    return new Pass(status, resolved.results(), resolved.interrupted());
  }

  private record Pass(
      PlayerIdentityRunStatus status,
      Map<Long, PlayerIdentityItemResult> results,
      boolean interrupted) {}
}
