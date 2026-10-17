package footballmarket.repositories;

import footballmarket.models.MarketTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Acceso a la auditoría de compras.
 *
 * <p>Solo expone la persistencia de registros confirmados. La aplicación no ofrece operaciones de
 * modificación o eliminación sobre ellos, de modo que la auditoría es append-only.
 */
@Repository
public interface MarketTransactionRepository extends JpaRepository<MarketTransaction, Long> {

  /**
   * Lista los registros financieros de un jugador, del más reciente al más antiguo.
   *
   * @param playerId jugador cuyos registros se consultan
   * @return registros del jugador ordenados por fecha de ejecución descendente
   */
  List<MarketTransaction> findByPlayerIdOrderByExecutedAtDescIdDesc(Long playerId);
}
