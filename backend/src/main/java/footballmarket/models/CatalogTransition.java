package footballmarket.models;

import footballmarket.models.exceptions.InvalidCatalogEntityException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/** Marcador singleton de la transición V5; no sustituye la autorización humana de Release 2. */
@Entity
@Table(name = "catalog_transition")
@Getter
public class CatalogTransition {
  @Id private Integer id;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected CatalogTransition() {}

  /**
   * Marca una sola vez, después de verificar cobertura de asociaciones/casos en el mismo commit.
   */
  public void complete(Instant completedAt) {
    if (completedAt == null) {
      throw new InvalidCatalogEntityException("La transición requiere fecha de finalización");
    }
    if (this.completedAt == null) {
      this.completedAt = completedAt;
    }
  }
}
