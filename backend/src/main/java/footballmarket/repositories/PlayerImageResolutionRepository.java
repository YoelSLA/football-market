package footballmarket.repositories;

import footballmarket.models.PlayerImageResolution;
import footballmarket.models.enums.PlayerImageResolutionStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso al estado operativo de las imágenes de cada jugador. */
@Repository
public interface PlayerImageResolutionRepository
    extends JpaRepository<PlayerImageResolution, Long> {
  @Query(
      "select r from PlayerImageResolution r where r.status in :immediate or "
          + "(r.status = :notFound and r.lastAttemptAt < :retryBefore) order by r.playerId asc")
  List<PlayerImageResolution> findEligible(
      @Param("immediate") List<PlayerImageResolutionStatus> immediate,
      @Param("notFound") PlayerImageResolutionStatus notFound,
      @Param("retryBefore") Instant retryBefore);
}
