package footballmarket.repositories;

import footballmarket.models.PositionalValuation;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de valoraciones posicionales. */
public interface PositionalValuationRepository extends JpaRepository<PositionalValuation, Long> {
  void deleteByPlayerIdAndValuationDate(Long playerId, LocalDate valuationDate);
}
