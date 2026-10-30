package footballmarket.features.identity.domain.enums;

/** Categorías funcionales de revisión sin estado ni cierre automático. */
public enum ReviewCategory {
  PLAYER_TEAM_UNRESOLVED,
  TEAM_LEAGUE_UNRESOLVED,
  EXTERNAL_IDENTITY_CONFLICT,
  PLAYER_OPTIONAL_CONFLICT,
  INVALID_SUBJECT_DATA
}
