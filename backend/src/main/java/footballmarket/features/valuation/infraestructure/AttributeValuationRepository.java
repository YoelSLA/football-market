package footballmarket.features.valuation.infraestructure;

import footballmarket.features.quote.domain.records.QuoteRankingPage.LeagueOption;
import footballmarket.features.ranking.domain.records.ScoreRankingRow;
import footballmarket.features.valuation.domain.models.AttributeValuation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de valoraciones de atributos. */
public interface AttributeValuationRepository extends JpaRepository<AttributeValuation, Long> {
  @Query(
      value = ScoreRankingSql.ATTRIBUTE + ScoreRankingSql.RANKED + ScoreRankingSql.PAGE,
      nativeQuery = true)
  List<ScoreRankingRow> findRankingPage(
      String name, Long leagueId, String position, int size, long offset);

  @Query(
      value = ScoreRankingSql.ATTRIBUTE + ScoreRankingSql.RANKED + ScoreRankingSql.COUNT,
      nativeQuery = true)
  long countRanking(String name, Long leagueId, String position);

  @Query(
      value = ScoreRankingSql.ATTRIBUTE + ScoreRankingSql.RANKED + ScoreRankingSql.LEAGUES,
      nativeQuery = true)
  List<LeagueOption> findRankingLeagues();

  @Query(
      value = ScoreRankingSql.ATTRIBUTE + ScoreRankingSql.RANKED + ScoreRankingSql.POSITIONS,
      nativeQuery = true)
  List<String> findRankingPositions();

  void deleteByPlayerIdAndValuationDate(Long playerId, LocalDate valuationDate);

  /**
   * Valuación de atributos más reciente del jugador.
   *
   * @param playerId identificador interno del jugador
   * @return valoraciones del jugador, de la más reciente a la más antigua
   */
  @Query(
      "select v from AttributeValuation v where v.player.id = :playerId"
          + " order by v.valuationDate desc, v.calculatedAt desc")
  List<AttributeValuation> findLatestByPlayerId(@Param("playerId") Long playerId);

  /**
   * Última valuación por atributos de cada jugador que tiene cotización vigente.
   *
   * @return valuaciones ordenadas por score descendente y jugador ascendente
   */
  @Query(
      "select v from AttributeValuation v where exists ("
          + "select q.id from PlayerQuote q where q.player.id = v.player.id) and not exists ("
          + "select newer.id from AttributeValuation newer where newer.player.id = v.player.id"
          + " and newer.valuationDate > v.valuationDate)"
          + " and exists (select s.id from PlayerStatistics s where s.player.id = v.player.id"
          + " and s.presentMetrics > 0 and s.extractionDate ="
          + " (select max(latest.extractionDate) from PlayerStatistics latest where latest.player.id = v.player.id))"
          + " order by v.finalGrl desc, v.player.id asc")
  List<AttributeValuation> findScoreRanking();
}
