package footballmarket.features.identity.infraestructure;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.identity.domain.enums.ReviewSubjectType;
import footballmarket.features.identity.domain.models.PendingReviewCase;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso interno a casos; no constituye una interfaz humana de revisión. */
@Repository
public interface PendingReviewCaseRepository extends JpaRepository<PendingReviewCase, Long> {
  Optional<PendingReviewCase> findByCaseKey(String caseKey);

  List<PendingReviewCase> findBySubjectTypeAndSubjectProviderAndSubjectExternalId(
      ReviewSubjectType subjectType, ExternalProvider provider, String externalId);
}
