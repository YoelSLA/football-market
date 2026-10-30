package footballmarket.features.identity.infraestructure;

import footballmarket.features.identity.domain.models.PlayerIdentityLeagueDiscovery;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Lectura durable de ámbitos de una ejecución en orden estable. */
public interface PlayerIdentityLeagueDiscoveryRepository
    extends JpaRepository<PlayerIdentityLeagueDiscovery, Long> {
  List<PlayerIdentityLeagueDiscovery> findByRunIdOrderById(Long runId);
}
