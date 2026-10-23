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
}
