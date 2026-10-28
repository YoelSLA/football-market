package footballmarket.features.images.infraestructure;

import footballmarket.features.images.domain.enums.PlayerImageSyncRunStatus;
import footballmarket.features.images.domain.models.PlayerImageSyncRun;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistencia e historial de ejecuciones de sincronización de imágenes. */
@Repository
public interface PlayerImageSyncRunRepository extends JpaRepository<PlayerImageSyncRun, Long> {
  List<PlayerImageSyncRun> findByStatus(PlayerImageSyncRunStatus status);

  Page<PlayerImageSyncRun> findAllByOrderByStartedAtDescIdDesc(Pageable pageable);

  List<PlayerImageSyncRun> findByFinishedAtBefore(Instant cutoff);
}
