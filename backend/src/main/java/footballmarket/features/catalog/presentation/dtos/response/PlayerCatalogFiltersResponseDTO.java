package footballmarket.features.catalog.presentation.dtos.response;

import java.util.List;

/** Opciones únicas del conjunto completo visible, sin cascadas. */
public record PlayerCatalogFiltersResponseDTO(
    List<League> leagues, List<Team> teams, List<String> positions, List<String> nationalities) {
  /** ID y nombre internos de liga. */
  public record League(long id, String name) {}

  /** ID y nombre internos de equipo y su asociación de liga. */
  public record Team(long id, String name, long leagueId) {}
}
