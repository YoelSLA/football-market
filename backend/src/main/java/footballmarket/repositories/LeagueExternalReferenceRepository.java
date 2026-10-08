package footballmarket.repositories;

import footballmarket.models.LeagueExternalReference;
import footballmarket.models.enums.ExternalProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Resolución de ligas exclusivamente por identidad de proveedor. */
@Repository
public interface LeagueExternalReferenceRepository
    extends JpaRepository<LeagueExternalReference, Long> {
  @EntityGraph(attributePaths = "league")
  Optional<LeagueExternalReference> findByProviderAndExternalId(
      ExternalProvider provider, String externalId);
}
