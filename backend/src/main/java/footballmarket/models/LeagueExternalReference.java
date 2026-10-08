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

/** Referencia externa inmutable de una liga, independiente de su clave interna. */
@Entity
@Table(
    name = "league_external_references",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_league_external_references_provider_external_id",
          columnNames = {"provider", "external_id"}),
      @UniqueConstraint(
          name = "uk_league_external_references_owner_provider",
          columnNames = {"league_id", "provider"})
    })
@Getter
public class LeagueExternalReference {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "league_reference_ids")
  @SequenceGenerator(
      name = "league_reference_ids",
      sequenceName = "league_external_references_id_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "league_id", nullable = false, updatable = false)
  private League league;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 64)
  private ExternalProvider provider;

  @Column(name = "external_id", nullable = false, updatable = false)
  private String externalId;

  protected LeagueExternalReference() {}

  LeagueExternalReference(League league, ExternalProvider provider, String externalId) {
    if (league == null
        || provider == null
        || externalId == null
        || externalId.isBlank()
        || externalId.length() > 255) {
      throw new InvalidCatalogEntityException(
          "La referencia requiere liga, proveedor e identificador válido");
    }
    this.league = league;
    this.provider = provider;
    this.externalId = externalId;
  }
}
