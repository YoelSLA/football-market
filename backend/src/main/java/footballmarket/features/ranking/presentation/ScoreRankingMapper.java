package footballmarket.features.ranking.presentation;

import footballmarket.features.ranking.domain.records.ScoreRanking;
import footballmarket.features.ranking.presentation.dtos.response.ScoreRankingEntryResponseDTO;
import footballmarket.features.ranking.presentation.dtos.response.ScoreRankingResponseDTO;

/** Transformación estructural del ranking por score al contrato HTTP. */
public final class ScoreRankingMapper {
  private ScoreRankingMapper() {}

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
