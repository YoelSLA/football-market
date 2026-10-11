package footballmarket.services.internal;

import footballmarket.models.PendingReviewCase;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.enums.ReviewCategory;
import footballmarket.models.enums.ReviewCause;
import footballmarket.models.enums.ReviewSubjectType;
import footballmarket.repositories.PendingReviewCaseRepository;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Registro deduplicado de asociaciones que la foto no pudo resolver.
 *
 * <p>Un caso identifica el sujeto afectado, guarda una causa estable, la clave que lo deduplica y
 * la evidencia recibida. Una detección posterior actualiza la última observación sin sobrescribir
 * la evidencia original ni la primera detección. No certifica que el problema siga vigente ni
 * materializa revisiones.
 */
@Component
public class PendingReviewRecorder {
  private final PendingReviewCaseRepository cases;

  public PendingReviewRecorder(PendingReviewCaseRepository cases) {
    this.cases = cases;
  }

  /**
   * Registra un caso con clave derivada del sujeto y de la relación observada.
   *
   * @param category categoría funcional del problema
   * @param cause causa estable de esta detección
   * @param type tipo de sujeto afectado
   * @param subjectId identificador interno cuando el sujeto ya existe
   * @param externalId identidad externa del sujeto, o {@code null} si es interno
   * @param relation relación que origina el problema; forma parte de la clave
   * @param evidence evidencia recibida en esta detección
   */
  public void record(
      ReviewCategory category,
      ReviewCause cause,
      ReviewSubjectType type,
      Long subjectId,
      String externalId,
      String relation,
      Map<String, Object> evidence) {
    String key =
        externalId == null
            ? category + ":" + type + ":" + subjectId + ":" + relation.length() + ":" + relation
            : category
                + ":"
                + type
                + ":FOOTBALL_DATA:"
                + externalId.length()
                + ":"
                + externalId
                + ":"
                + relation.length()
                + ":"
                + relation;
    this.observe(key, category, cause, type, subjectId, externalId, evidence);
  }

  /**
   * Registra o actualiza el caso de una clave explícita, usada cuando el problema ya se detectó
   * antes con otra relación y no debe duplicarse.
   */
  public void observe(
      String key,
      ReviewCategory category,
      ReviewCause cause,
      ReviewSubjectType type,
      Long subjectId,
      String externalId,
      Map<String, Object> evidence) {
    Instant now = Instant.now();
    PendingReviewCase review = this.cases.findByCaseKey(key).orElse(null);
    if (review == null) {
      this.cases.save(
          new PendingReviewCase(
              category,
              cause,
              type,
              subjectId,
              externalId == null ? null : ExternalProvider.FOOTBALL_DATA,
              externalId,
              key,
              now,
              evidence));
      return;
    }
    if (subjectId != null) {
      review.recognizeSubject(subjectId);
    }
    review.observe(cause, now, evidence);
  }

  /**
   * Asocia al sujeto interno ya reconocido los casos que solo lo conocían por identidad externa.
   *
   * @param type tipo de sujeto
   * @param externalId identidad externa observada en esta foto
   * @param subjectId identificador interno resultante de la resolución
   */
  public void reconcile(ReviewSubjectType type, String externalId, Long subjectId) {
    for (PendingReviewCase review :
        this.cases.findBySubjectTypeAndSubjectProviderAndSubjectExternalId(
            type, ExternalProvider.FOOTBALL_DATA, externalId)) {
      if (review.getSubjectId() == null) {
        review.recognizeSubject(subjectId);
      }
    }
  }
}
