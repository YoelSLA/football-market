package footballmarket.repositories;

import footballmarket.models.AttributeValuation;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de valoraciones de atributos. */
public interface AttributeValuationRepository extends JpaRepository<AttributeValuation, Long> {
  void deleteByPlayerIdAndValuationDate(Long playerId, LocalDate valuationDate);
}
