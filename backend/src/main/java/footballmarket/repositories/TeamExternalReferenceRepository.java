package footballmarket.repositories;

import footballmarket.models.TeamExternalReference;
import footballmarket.models.enums.ExternalProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Resolución de equipos por referencia sin matching textual de identidad. */
@Repository
public interface TeamExternalReferenceRepository
    extends JpaRepository<TeamExternalReference, Long> {
  @EntityGraph(attributePaths = {"team", "team.league"})
  Optional<TeamExternalReference> findByProviderAndExternalId(
      ExternalProvider provider, String externalId);
}
