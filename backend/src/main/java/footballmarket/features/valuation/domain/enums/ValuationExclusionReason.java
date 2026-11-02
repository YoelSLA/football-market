package footballmarket.features.valuation.domain.enums;

/** Motivos funcionales de exclusión, declarados en su orden de prioridad. */
public enum ValuationExclusionReason {
  UNRESOLVED_LEAGUE_CONTEXT,
  MISSING_OR_UNCONFIRMED_SNAPSHOT,
  MISSING_REQUIRED_METRICS,
  NO_APPEARANCES
}
