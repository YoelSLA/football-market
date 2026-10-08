package footballmarket.models;

import footballmarket.models.enums.TeamResolutionFailure;
import footballmarket.models.enums.TeamResolutionResult;
import footballmarket.models.exceptions.InvalidCatalogEntityException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/** Separa última llamada técnica de última evaluación válida y espera del proveedor. */
@Entity
@Table(name = "team_resolution_attempts")
@Getter
public class TeamResolutionAttempt {
  @Id
  @Column(name = "team_id")
  private Long teamId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  @Column(name = "last_call_at", nullable = false)
  private Instant lastCallAt;

  @Column(name = "last_call_name", nullable = false)
  private String lastCallName;

  @Enumerated(EnumType.STRING)
  @Column(name = "technical_result", length = 64)
  private TeamResolutionFailure technicalResult;

  @Column(name = "last_valid_evaluation_at")
  private Instant lastValidEvaluationAt;

  @Column(name = "last_valid_evaluation_name")
  private String lastValidEvaluationName;

  @Enumerated(EnumType.STRING)
  @Column(name = "valid_result", length = 64)
  private TeamResolutionResult validResult;

  @Column(name = "retry_not_before")
  private Instant retryNotBefore;

  protected TeamResolutionAttempt() {}

  public TeamResolutionAttempt(Team team, Instant calledAt, String calledName) {
    if (team == null || calledAt == null || calledName == null || calledName.isBlank()) {
      throw new InvalidCatalogEntityException(
          "El intento requiere equipo, fecha y nombre consultado");
    }
    this.team = team;
    this.teamId = team.getId();
    this.lastCallAt = calledAt;
    this.lastCallName = calledName;
  }

  /** La comparación de elegibilidad usa la misma normalización estricta que el matching. */
  public boolean eligible(String name, Instant now) {
    return (this.retryNotBefore == null || !now.isBefore(this.retryNotBefore))
        && (this.lastValidEvaluationName == null
            || !TeamNameNormalizer.normalize(this.lastValidEvaluationName)
                .equals(TeamNameNormalizer.normalize(name)));
  }

  public void evaluated(Instant calledAt, String calledName, TeamResolutionResult result) {
    this.validateCall(calledAt, calledName);
    if (result == null) {
      throw new InvalidCatalogEntityException("La evaluación requiere un resultado válido");
    }
    this.lastCallAt = calledAt;
    this.lastCallName = calledName;
    this.technicalResult = null;
    this.lastValidEvaluationAt = calledAt;
    this.lastValidEvaluationName = calledName;
    this.validResult = result;
    this.retryNotBefore = null;
  }

  /** Un fallo no reemplaza la evaluación válida ni consume un nombre nuevo. */
  public void failed(
      Instant calledAt, String calledName, TeamResolutionFailure failure, Instant retryAt) {
    this.validateCall(calledAt, calledName);
    if (failure == null) {
      throw new InvalidCatalogEntityException("El fallo requiere una causa técnica");
    }
    this.lastCallAt = calledAt;
    this.lastCallName = calledName;
    this.technicalResult = failure;
    this.retryNotBefore = retryAt;
  }

  private void validateCall(Instant calledAt, String calledName) {
    if (calledAt == null
        || calledName == null
        || calledName.isBlank()
        || calledName.length() > 255
        || calledAt.isBefore(this.lastCallAt)) {
      throw new InvalidCatalogEntityException("La llamada requiere fecha y nombre válidos");
    }
  }
}
