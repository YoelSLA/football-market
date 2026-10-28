package footballmarket.features.catalog.presentation;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.records.PlayerCatalogCriteria;
import footballmarket.features.catalog.domain.records.PlayerCatalogFilters;
import footballmarket.features.catalog.domain.records.PlayerCatalogPage;
import footballmarket.features.catalog.domain.records.PlayerSynchronizationResult;
import footballmarket.features.catalog.presentation.dtos.request.PlayerCatalogQueryRequestDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayerCatalogEntryResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayerCatalogFiltersResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayerCatalogPageResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayerResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayerSyncResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayersPageResponseDTO;
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
                  footballmarket.features.catalog.domain.records.PlayerCatalogStatistics stats =
                      entry.statistics();
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

  private static String source(
      footballmarket.features.statistics.domain.enums.MetricSource source) {
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
