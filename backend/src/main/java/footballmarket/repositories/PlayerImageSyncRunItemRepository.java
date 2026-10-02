package footballmarket.repositories;

import footballmarket.models.PlayerImageSyncRunItem;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso paginado a los resultados individuales y a los items inconclusos. */
@Repository
public interface PlayerImageSyncRunItemRepository
    extends JpaRepository<PlayerImageSyncRunItem, Long> {
  Page<PlayerImageSyncRunItem> findByRunIdOrderByIdAsc(Long runId, Pageable pageable);

  List<PlayerImageSyncRunItem> findByRunIdAndFinishedAtIsNull(Long runId);

  void deleteByRunId(Long runId);
}
