package footballmarket.features.identity.domain.enums;

import java.util.Collection;

/** Finalización del trabajo, independiente de la cobertura de identidades. */
public enum PlayerIdentityRunStatus {
  COMPLETED,
  PARTIAL,
  FAILED;

  /** Clasifica finalización del trabajo sin exigir un porcentaje de identidades resueltas. */
  public static PlayerIdentityRunStatus forPass(
      Collection<PlayerIdentityItemResult> results,
      int expectedLeagues,
      int completedLeagues,
      boolean interrupted) {
    boolean technicalErrors = results.stream().anyMatch(PlayerIdentityRunStatus::isTechnicalError);
    if (!interrupted && !technicalErrors && completedLeagues == expectedLeagues) {
      return COMPLETED;
    }
    boolean useful =
        completedLeagues > 0 || results.stream().anyMatch(result -> !isTechnicalError(result));
    return useful ? PARTIAL : FAILED;
  }

  private static boolean isTechnicalError(PlayerIdentityItemResult result) {
    return result == PlayerIdentityItemResult.DISCOVERY_ERROR
        || result == PlayerIdentityItemResult.PERSISTENCE_ERROR
        || result == PlayerIdentityItemResult.INTERRUPTED;
  }

  /** El estado global conserva el peor estado de los pases ejecutados. */
  public PlayerIdentityRunStatus combine(PlayerIdentityRunStatus other) {
    if (this == FAILED || other == FAILED) {
      return FAILED;
    }
    return this == PARTIAL || other == PARTIAL ? PARTIAL : COMPLETED;
  }
}
