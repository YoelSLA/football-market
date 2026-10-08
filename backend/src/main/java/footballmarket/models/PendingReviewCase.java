package footballmarket.models;

import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.enums.ReviewCategory;
import footballmarket.models.enums.ReviewCause;
import footballmarket.models.enums.ReviewSubjectType;
import footballmarket.models.exceptions.InvalidCatalogEntityException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Observación deduplicada para revisión humana; no certifica vigencia del problema. */
@Entity
@Table(
    name = "pending_review_cases",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_pending_review_cases_case_key", columnNames = "case_key"))
@Getter
public class PendingReviewCase {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "review_case_ids")
  @SequenceGenerator(
      name = "review_case_ids",
      sequenceName = "pending_review_cases_id_seq",
      allocationSize = 1)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 64, updatable = false)
  private ReviewCategory category;

  @Enumerated(EnumType.STRING)
  @Column(name = "cause_code", nullable = false, length = 64)
  private ReviewCause causeCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "subject_type", nullable = false, length = 16, updatable = false)
  private ReviewSubjectType subjectType;

  @Column(name = "subject_id")
  private Long subjectId;

  @Enumerated(EnumType.STRING)
  @Column(name = "subject_provider", length = 64, updatable = false)
  private ExternalProvider subjectProvider;

  @Column(name = "subject_external_id", updatable = false)
  private String subjectExternalId;

  @Column(name = "case_key", nullable = false, length = 2048, updatable = false)
  private String caseKey;

  @Column(name = "first_detected_at", nullable = false, updatable = false)
  private Instant firstDetectedAt;

  @Column(name = "last_detected_at", nullable = false)
  private Instant lastDetectedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> evidence;

  protected PendingReviewCase() {}

  /** Crea un caso con clave canónica calculada por la aplicación, nunca a partir del nombre. */
  public PendingReviewCase(
      ReviewCategory category,
      ReviewCause cause,
      ReviewSubjectType subjectType,
      Long subjectId,
      ExternalProvider provider,
      String externalId,
      String caseKey,
      Instant detectedAt,
      Map<String, Object> evidence) {
    if (category == null
        || cause == null
        || subjectType == null
        || caseKey == null
        || caseKey.isBlank()
        || caseKey.length() > 2048
        || detectedAt == null
        || evidence == null
        || (subjectId != null && subjectId <= 0)
        || (provider == null) != (externalId == null)
        || (externalId != null && (externalId.isBlank() || externalId.length() > 255))) {
      throw new InvalidCatalogEntityException(
          "El caso requiere sujeto, causa, clave y evidencia válidos");
    }
    this.category = category;
    this.causeCode = cause;
    this.subjectType = subjectType;
    this.subjectId = subjectId;
    this.subjectProvider = provider;
    this.subjectExternalId = externalId;
    this.caseKey = caseKey;
    this.firstDetectedAt = detectedAt;
    this.lastDetectedAt = detectedAt;
    this.evidence = Map.copyOf(evidence);
  }

  /** Actualiza la observación conservando primera detección y texto original de transición. */
  public void observe(ReviewCause cause, Instant detectedAt, Map<String, Object> evidence) {
    if (cause == null
        || detectedAt == null
        || detectedAt.isBefore(this.lastDetectedAt)
        || evidence == null) {
      throw new InvalidCatalogEntityException(
          "La observación no puede retroceder la fecha de detección");
    }
    Map<String, Object> updated = new LinkedHashMap<>(evidence);
    if (this.evidence.containsKey("originalTeamName")) {
      updated.put("originalTeamName", this.evidence.get("originalTeamName"));
    }
    this.evidence = Map.copyOf(updated);
    this.causeCode = cause;
    this.lastDetectedAt = detectedAt;
  }

  /** Reconcilia un sujeto externo identificado posteriormente, sin cambiar su clave de origen. */
  public void recognizeSubject(Long subjectId) {
    if (subjectId == null
        || subjectId <= 0
        || (this.subjectId != null && !this.subjectId.equals(subjectId))) {
      throw new InvalidCatalogEntityException("El caso no puede reasignarse a otro sujeto");
    }
    this.subjectId = subjectId;
  }
}
