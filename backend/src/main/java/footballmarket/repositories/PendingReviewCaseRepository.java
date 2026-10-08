package footballmarket.repositories;

import footballmarket.models.PendingReviewCase;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.enums.ReviewCategory;
import footballmarket.models.enums.ReviewSubjectType;
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

  List<PendingReviewCase> findBySubjectTypeAndSubjectIdAndCategory(
      ReviewSubjectType subjectType, Long subjectId, ReviewCategory category);
}
