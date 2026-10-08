package footballmarket.models;

import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.exceptions.InvalidCatalogEntityException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

/** Referencia externa de un equipo; propietario, proveedor e identidad no se reasignan. */
@Entity
@Table(
    name = "team_external_references",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_team_external_references_provider_external_id",
          columnNames = {"provider", "external_id"}),
      @UniqueConstraint(
          name = "uk_team_external_references_owner_provider",
          columnNames = {"team_id", "provider"})
    })
@Getter
public class TeamExternalReference {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "team_reference_ids")
  @SequenceGenerator(
      name = "team_reference_ids",
      sequenceName = "team_external_references_id_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false, updatable = false)
  private Team team;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 64)
  private ExternalProvider provider;

  @Column(name = "external_id", nullable = false, updatable = false)
  private String externalId;

  protected TeamExternalReference() {}

  TeamExternalReference(Team team, ExternalProvider provider, String externalId) {
    if (team == null
        || provider == null
        || externalId == null
        || externalId.isBlank()
        || externalId.length() > 255) {
      throw new InvalidCatalogEntityException(
          "La referencia requiere equipo, proveedor e identificador válido");
    }
    this.team = team;
    this.provider = provider;
    this.externalId = externalId;
  }
}
