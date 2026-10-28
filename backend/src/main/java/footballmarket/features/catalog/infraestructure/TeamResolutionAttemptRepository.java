package footballmarket.features.catalog.infraestructure;

import footballmarket.features.catalog.domain.models.team.TeamResolutionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistencia del último intento externo independiente por equipo. */
@Repository
public interface TeamResolutionAttemptRepository
    extends JpaRepository<TeamResolutionAttempt, Long> {}
