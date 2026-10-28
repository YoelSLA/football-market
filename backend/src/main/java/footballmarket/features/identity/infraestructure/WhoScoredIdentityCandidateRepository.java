package footballmarket.features.identity.infraestructure;

import footballmarket.features.identity.domain.models.WhoScoredIdentityCandidate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Identidades deduplicadas publicadas solo por ámbitos completos. */
public interface WhoScoredIdentityCandidateRepository
    extends JpaRepository<WhoScoredIdentityCandidate, Long> {
  List<WhoScoredIdentityCandidate> findByRunIdOrderById(Long runId);

  long countByRunId(Long runId);
}
