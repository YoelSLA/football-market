package footballmarket.features.identity.domain.models;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.catalog.domain.models.player.PlayerExternalReference;
import footballmarket.features.identity.domain.records.WhoScoredIdentityContext;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Una identidad externa por ejecución con todas sus variantes de ligas completas. */
@Entity
@Table(
    name = "whoscored_identity_candidates",
    uniqueConstraints = @UniqueConstraint(columnNames = {"run_id", "external_id"}))
@Getter
public class WhoScoredIdentityCandidate {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "identity_candidate_ids")
  @SequenceGenerator(
      name = "identity_candidate_ids",
      sequenceName = "whoscored_identity_candidates_id_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long runId;

  @Column(nullable = false, updatable = false)
  private String externalId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private List<WhoScoredIdentityContext> contexts = new ArrayList<>();

  protected WhoScoredIdentityCandidate() {}

  public WhoScoredIdentityCandidate(Long runId, String externalId) {
    if (runId == null || runId < 1) {
      throw new InvalidPlayerException("El candidato requiere una ejecución persistida");
    }
    this.runId = runId;
    this.externalId =
        PlayerExternalReference.canonicalExternalId(ExternalProvider.WHOSCORED, externalId);
  }

  public List<WhoScoredIdentityContext> getContexts() {
    return List.copyOf(this.contexts);
  }

  /** Incorpora una observación completa, sin duplicar contextos iguales. */
  public void observe(WhoScoredIdentityContext context) {
    if (context == null || context.leagueId() == null || context.tournamentId() < 1) {
      throw new InvalidPlayerException("El contexto requiere un ámbito capturado");
    }
    if (!this.contexts.contains(context)) {
      this.contexts.add(context);
    }
  }
}
