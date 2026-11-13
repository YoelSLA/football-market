package footballmarket.features.ranking.presentation;

import footballmarket.features.ranking.domain.records.ScoreRanking;
import footballmarket.features.ranking.domain.records.ScoreRankingPage;
import footballmarket.features.ranking.domain.records.ScoreRankingQuery;
import footballmarket.features.ranking.presentation.dtos.request.ScoreRankingQueryRequestDTO;
import footballmarket.features.ranking.presentation.dtos.response.ScoreRankingEntryResponseDTO;
import footballmarket.features.ranking.presentation.dtos.response.ScoreRankingPageResponseDTO;
import footballmarket.features.ranking.presentation.dtos.response.ScoreRankingResponseDTO;

/** Transformación estructural del ranking por score al contrato HTTP. */
public final class ScoreRankingMapper {
  private ScoreRankingMapper() {}

  public static ScoreRankingQuery toQuery(ScoreRankingQueryRequestDTO request) {
    return new ScoreRankingQuery(
        request.page(), request.size(), request.name(), request.leagueId(), request.position());
  }

  public static ScoreRankingPageResponseDTO toResponse(ScoreRankingPage page) {
    return new ScoreRankingPageResponseDTO(
        page.strategy() == null ? null : page.strategy().name(),
        page.quoteDate(),
        page.content().stream()
            .map(
                entry ->
                    new ScoreRankingPageResponseDTO.Entry(
                        entry.position(),
                        entry.playerId(),
                        entry.score(),
                        entry.valuationDate(),
                        new ScoreRankingPageResponseDTO.Player(
                            entry.player().name(),
                            entry.player().position(),
                            entry.player().imageUrl(),
                            entry.player().team() == null
                                ? null
                                : new ScoreRankingPageResponseDTO.Team(
                                    entry.player().team().id(),
                                    entry.player().team().name(),
                                    entry.player().team().crestUrl()),
                            entry.player().league() == null
                                ? null
                                : new ScoreRankingPageResponseDTO.League(
                                    entry.player().league().id(), entry.player().league().name()))))
            .toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages(),
        new ScoreRankingPageResponseDTO.FilterOptions(
            page.filterOptions().leagues().stream()
                .map(league -> new ScoreRankingPageResponseDTO.League(league.id(), league.name()))
                .toList(),
            page.filterOptions().positions()));
  }

  /**
   * @param ranking resultado del caso de uso
   * @return respuesta HTTP con el mismo orden y posiciones
   */
  public static ScoreRankingResponseDTO toResponse(ScoreRanking ranking) {
    return new ScoreRankingResponseDTO(
        ranking.strategy() == null ? null : ranking.strategy().name(),
        ranking.quoteDate(),
        ranking.entries().stream()
            .map(
                entry ->
                    new ScoreRankingEntryResponseDTO(
                        entry.position(), entry.playerId(), entry.score(), entry.valuationDate()))
            .toList());
  }
}
