package footballmarket.repositories;

import footballmarket.models.PlayerIdentitySyncRun;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de ejecuciones de identidad, sin consulta al proveedor. */
public interface PlayerIdentitySyncRunRepository
    extends JpaRepository<PlayerIdentitySyncRun, Long> {}
