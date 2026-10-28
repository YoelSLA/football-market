package footballmarket.features.identity.infraestructure;

import footballmarket.features.identity.domain.models.PlayerIdentitySyncRun;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de ejecuciones de identidad, sin consulta al proveedor. */
public interface PlayerIdentitySyncRunRepository
    extends JpaRepository<PlayerIdentitySyncRun, Long> {}
