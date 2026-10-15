package footballmarket.models;

import footballmarket.models.exceptions.InvalidPlayerException;
import jakarta.persistence.*;
import lombok.Getter;

/** Descarte auditable sin cuerpos crudos ni identidades inventadas. */
@Entity
@Table(name = "player_identity_discarded_rows")
@Getter
public class PlayerIdentityDiscardedRow {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "identity_discard_ids")
  @SequenceGenerator(
      name = "identity_discard_ids",
      sequenceName = "player_identity_discarded_rows_id_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long runId;

  @Column(nullable = false, updatable = false)
  private Long discoveryId;

  @Column(nullable = false, updatable = false)
  private int page;

  @Column(nullable = false, updatable = false)
  private int rowPosition;

  @Column(nullable = false, updatable = false)
  private String reasonCode;

  protected PlayerIdentityDiscardedRow() {}

  public PlayerIdentityDiscardedRow(
      PlayerIdentityLeagueDiscovery discovery, int page, int rowPosition, String reasonCode) {
    if (discovery == null
        || discovery.getId() == null
        || page < 1
        || rowPosition < 1
        || reasonCode == null
        || !reasonCode.matches("[A-Z_]+")) {
      throw new InvalidPlayerException("El descarte requiere posición y motivo válidos");
    }
    this.runId = discovery.getRunId();
    this.discoveryId = discovery.getId();
    this.page = page;
    this.rowPosition = rowPosition;
    this.reasonCode = reasonCode;
  }
}
