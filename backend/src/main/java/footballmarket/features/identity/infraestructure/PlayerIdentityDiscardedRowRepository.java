package footballmarket.features.identity.infraestructure;

import footballmarket.features.identity.domain.models.PlayerIdentityDiscardedRow;
import org.springframework.data.jpa.repository.JpaRepository;

/** Conteo de filas descartadas, independiente de resultados de jugadores. */
public interface PlayerIdentityDiscardedRowRepository
    extends JpaRepository<PlayerIdentityDiscardedRow, Long> {
  long countByRunId(Long runId);
}
