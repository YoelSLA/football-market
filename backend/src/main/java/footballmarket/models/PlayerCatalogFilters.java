package footballmarket.models;

import java.util.List;

/** Opciones globales del conjunto visible; nunca son facetas de una sola página. */
public record PlayerCatalogFilters(
    List<LeagueOption> leagues,
    List<TeamOption> teams,
    List<String> positions,
    List<String> nationalities) {
  public PlayerCatalogFilters {
    leagues = List.copyOf(leagues);
    teams = List.copyOf(teams);
    positions = List.copyOf(positions);
    nationalities = List.copyOf(nationalities);
  }

  /** Liga interna con nombre original. */
  public record LeagueOption(long id, String name) {}

  /** Equipo interno y su liga, sin cascadas implícitas. */
  public record TeamOption(long id, String name, long leagueId) {}
}
