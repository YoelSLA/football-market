package footballmarket.features.catalog.infraestructure;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.league.LeagueExternalReference;
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
