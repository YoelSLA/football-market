package footballmarket.models;

import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.exceptions.InvalidCatalogEntityException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;

/** Liga con identidad interna estable y nombre actual independiente del proveedor. */
@Entity
@Table(name = "leagues")
@Getter
public class League {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "league_ids")
  @SequenceGenerator(name = "league_ids", sequenceName = "leagues_id_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false)
  private String name;

  @OneToMany(mappedBy = "league", cascade = CascadeType.ALL)
  @Getter(AccessLevel.NONE)
  private List<LeagueExternalReference> externalReferences = new ArrayList<>();

  protected League() {}

  public League(String name) {
    this.rename(name);
  }

  /** Actualiza el nombre sin modificar la identidad ni las referencias. */
  public void rename(String name) {
    if (name == null || name.isBlank() || name.length() > 255) {
      throw new InvalidCatalogEntityException(
          "El nombre de la liga es obligatorio y admite 255 caracteres");
    }
    this.name = name;
  }

  public List<LeagueExternalReference> getExternalReferences() {
    return Collections.unmodifiableList(this.externalReferences);
  }

  /** Una referencia repetida es idempotente; otra identidad del mismo proveedor se rechaza. */
  public void addExternalReference(ExternalProvider provider, String externalId) {
    LeagueExternalReference incoming = new LeagueExternalReference(this, provider, externalId);
    for (LeagueExternalReference reference : this.externalReferences) {
      if (reference.getProvider() == provider) {
        if (reference.getExternalId().equals(externalId)) {
          return;
        }
        throw new InvalidCatalogEntityException(
            "La liga ya tiene otra identidad para el proveedor");
      }
    }
    this.externalReferences.add(incoming);
  }
}
