package footballmarket.repositories;

import footballmarket.models.AttributeValuation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de valoraciones de atributos. */
public interface AttributeValuationRepository extends JpaRepository<AttributeValuation, Long> {
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
          + " order by v.finalGrl desc, v.player.id asc")
  List<AttributeValuation> findScoreRanking();
}
