package footballmarket.features.valuation.infraestructure;

import footballmarket.features.quote.domain.records.QuoteRankingPage.LeagueOption;
import footballmarket.features.ranking.domain.records.ScoreRankingRow;
import footballmarket.features.valuation.domain.models.PositionalValuation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de valoraciones posicionales. */
public interface PositionalValuationRepository extends JpaRepository<PositionalValuation, Long> {
  @Query(
      value = ScoreRankingSql.POSITIONAL + ScoreRankingSql.RANKED + ScoreRankingSql.PAGE,
      nativeQuery = true)
  List<ScoreRankingRow> findRankingPage(
      String name, Long leagueId, String position, int size, long offset);

  @Query(
      value = ScoreRankingSql.POSITIONAL + ScoreRankingSql.RANKED + ScoreRankingSql.COUNT,
      nativeQuery = true)
  long countRanking(String name, Long leagueId, String position);

  @Query(
      value = ScoreRankingSql.POSITIONAL + ScoreRankingSql.RANKED + ScoreRankingSql.LEAGUES,
      nativeQuery = true)
  List<LeagueOption> findRankingLeagues();

  @Query(
      value = ScoreRankingSql.POSITIONAL + ScoreRankingSql.RANKED + ScoreRankingSql.POSITIONS,
      nativeQuery = true)
  List<String> findRankingPositions();

  void deleteByPlayerIdAndValuationDate(Long playerId, LocalDate valuationDate);

  /**
   * Valuación posicional más reciente del jugador.
   *
   * @param playerId identificador interno del jugador
   * @return valoraciones del jugador, de la más reciente a la más antigua
   */
  @Query(
      "select v from PositionalValuation v where v.player.id = :playerId"
          + " order by v.valuationDate desc, v.calculatedAt desc")
  List<PositionalValuation> findLatestByPlayerId(@Param("playerId") Long playerId);

  /**
   * Última valuación posicional de cada jugador que tiene cotización vigente.
   *
   * @return valuaciones ordenadas por score descendente y jugador ascendente
   */
  @Query(
      "select v from PositionalValuation v where exists ("
          + "select q.id from PlayerQuote q where q.player.id = v.player.id) and not exists ("
          + "select newer.id from PositionalValuation newer where newer.player.id = v.player.id"
          + " and newer.valuationDate > v.valuationDate)"
          + " and exists (select s.id from PlayerStatistics s where s.player.id = v.player.id"
          + " and s.presentMetrics > 0 and s.extractionDate ="
          + " (select max(latest.extractionDate) from PlayerStatistics latest where latest.player.id = v.player.id))"
          + " order by v.finalScore desc, v.player.id asc")
  List<PositionalValuation> findScoreRanking();
}
