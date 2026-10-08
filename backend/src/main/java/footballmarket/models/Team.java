package footballmarket.models;

import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.exceptions.InvalidCatalogEntityException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;

/** Equipo con liga actual obligatoria y vigencia explícita, sin eliminación histórica. */
@Entity
@Table(name = "teams")
@Getter
public class Team {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "team_ids")
  @SequenceGenerator(name = "team_ids", sequenceName = "teams_id_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "league_id", nullable = false)
  private League league;

  @Column(nullable = false)
  private boolean current;

  @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
  @Getter(AccessLevel.NONE)
  private List<TeamExternalReference> externalReferences = new ArrayList<>();

  protected Team() {}

  public Team(String name, League league, boolean current) {
    this.update(name, league);
    this.current = current;
  }

  /** Valida ambos atributos antes de aplicar un rename o cambio de liga. */
  public void update(String name, League league) {
    if (name == null || name.isBlank() || name.length() > 255 || league == null) {
      throw new InvalidCatalogEntityException("El equipo requiere nombre válido y liga");
    }
    this.name = name;
    this.league = league;
  }

  public void markCurrent() {
    this.current = true;
  }

  public void retire() {
    this.current = false;
  }

  public List<TeamExternalReference> getExternalReferences() {
    return Collections.unmodifiableList(this.externalReferences);
  }

  /** Conserva una única identidad por proveedor sin reemplazar referencias anteriores. */
  public void addExternalReference(ExternalProvider provider, String externalId) {
    TeamExternalReference incoming = new TeamExternalReference(this, provider, externalId);
    for (TeamExternalReference reference : this.externalReferences) {
      if (reference.getProvider() == provider) {
        if (reference.getExternalId().equals(externalId)) {
          return;
        }
        throw new InvalidCatalogEntityException(
            "El equipo ya tiene otra identidad para el proveedor");
      }
    }
    this.externalReferences.add(incoming);
  }
}
