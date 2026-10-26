package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.requests.PlayerCatalogQueryRequestDTO;
import footballmarket.controllers.dtos.responses.PlayerCatalogEntryResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerCatalogFiltersResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerCatalogPageResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerSyncResponseDTO;
import footballmarket.controllers.dtos.responses.PlayersPageResponseDTO;
import footballmarket.models.Player;
import footballmarket.models.PlayerCatalogFilters;
import footballmarket.models.PlayerCatalogPage;
import footballmarket.models.records.PlayerCatalogCriteria;
import footballmarket.models.records.PlayerSynchronizationResult;
import org.springframework.data.domain.Page;

/** Traducción del catálogo local a representaciones HTTP sin referencias externas. */
public final class PlayerMapper {
  private PlayerMapper() {}

  public static PlayerCatalogCriteria toCriteria(PlayerCatalogQueryRequestDTO query) {
    return new PlayerCatalogCriteria(
        query.name(),
        query.leagueId(),
        query.teamId(),
        query.position(),
        query.nationality(),
        query.sort(),
        Boolean.TRUE.equals(query.includeStatistics()));
  }

  public static PlayerCatalogPageResponseDTO toResponse(PlayerCatalogPage page) {
    return new PlayerCatalogPageResponseDTO(
        page.content().stream()
            .map(
                entry -> {
                  Player player = entry.player();
                  footballmarket.models.PlayerCatalogStatistics stats = entry.statistics();
                  PlayerCatalogEntryResponseDTO.Statistics statistics =
                      stats == null
                          ? null
                          : new PlayerCatalogEntryResponseDTO.Statistics(
                              stats.seasonKey(),
                              stats.seasonLabel(),
                              stats.extractionDate(),
                              stats.competitionKey(),
                              stats.appearancesTotal(),
                              stats.goalsTotal(),
                              stats.assistsTotal(),
                              new PlayerCatalogEntryResponseDTO.Sources(
                                  source(stats.sources().appearancesTotal()),
                                  source(stats.sources().goalsTotal()),
                                  source(stats.sources().assistsTotal())));
                  return new PlayerCatalogEntryResponseDTO(
                      player.getId(),
                      player.getName(),
                      player.getTeam().getId(),
                      player.getTeam().getName(),
                      player.getLeague().getId(),
                      player.getLeague().getName(),
                      player.getPosition(),
                      player.getDateOfBirth(),
                      player.getNationality(),
                      player.getImageUrl(),
                      player.getFallbackImageUrl(),
                      statistics);
                })
            .toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }

  public static PlayerCatalogFiltersResponseDTO toResponse(PlayerCatalogFilters filters) {
    return new PlayerCatalogFiltersResponseDTO(
        filters.leagues().stream()
            .map(option -> new PlayerCatalogFiltersResponseDTO.League(option.id(), option.name()))
            .toList(),
        filters.teams().stream()
            .map(
                option ->
                    new PlayerCatalogFiltersResponseDTO.Team(
                        option.id(), option.name(), option.leagueId()))
            .toList(),
        filters.positions(),
        filters.nationalities());
  }

  private static String source(footballmarket.models.enums.MetricSource source) {
    return source == null ? null : source.name();
  }

  public static PlayerSyncResponseDTO toResponse(PlayerSynchronizationResult result) {
    return new PlayerSyncResponseDTO(
        result.obtained(),
        result.created(),
        result.updated(),
        result.markedInactive(),
        result.discardedInvalid());
  }

  public static PlayerResponseDTO toResponse(Player player) {
    return new PlayerResponseDTO(
        player.getId(),
        player.getName(),
        player.getTeam().getId(),
        player.getTeam().getName(),
        player.getLeague().getId(),
        player.getLeague().getName(),
        player.getPosition(),
        player.getDateOfBirth(),
        player.getNationality(),
        player.getImageUrl(),
        player.getFallbackImageUrl());
  }

  public static PlayersPageResponseDTO toResponse(Page<Player> page) {
    return new PlayersPageResponseDTO(
        page.getContent().stream().map(PlayerMapper::toResponse).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
