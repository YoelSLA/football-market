package footballmarket.models;

import footballmarket.models.exceptions.InvalidPlayerException;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/** Incidencia saneada de proveedor sin almacenar contenido ni credenciales. */
@Entity
@Table(name = "player_statistics_incidents")
@Getter
public class ExtractionIncident {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "statistics_incident_ids")
  @SequenceGenerator(
      name = "statistics_incident_ids",
      sequenceName = "player_statistics_incidents_id_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "run_item_id", nullable = false, updatable = false)
  private PlayerStatisticsRunItem runItem;

  @Column(updatable = false)
  private String section;

  @Column(nullable = false, updatable = false)
  private String code;

  @Column(updatable = false)
  private Integer providerStatus;

  @Column(nullable = false, updatable = false)
  private int attempt;

  @Column(nullable = false, updatable = false)
  private Instant occurredAt;

  protected ExtractionIncident() {}

  public ExtractionIncident(
      PlayerStatisticsRunItem runItem,
      String section,
      String code,
      Integer providerStatus,
      int attempt,
      Instant occurredAt) {
    if (runItem == null
        || code == null
        || code.isBlank()
        || (attempt != 1 && attempt != 2)
        || occurredAt == null) {
      throw new InvalidPlayerException("La incidencia de extracción es inconsistente");
    }
    this.runItem = runItem;
    this.section = section == null || section.isBlank() ? null : section.strip();
    this.code = code.strip();
    this.providerStatus = providerStatus;
    this.attempt = attempt;
    this.occurredAt = occurredAt;
  }
}
