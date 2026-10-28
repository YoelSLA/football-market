package footballmarket.features.identity.domain.enums;

/** Causas estables, independientes del mensaje y de los valores observados. */
public enum ReviewCause {
  TEAM_UNRESOLVED,
  LEAGUE_UNRESOLVED,
  EXTERNAL_IDENTITY_OWNED,
  MISSING_REQUIRED_DATA,
  CONFLICTING_PLAYER_TEAMS,
  CONFLICTING_PLAYER_STATE,
  CONFLICTING_DATE_OF_BIRTH,
  CONFLICTING_NATIONALITY
}
