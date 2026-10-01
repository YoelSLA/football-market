package footballmarket.models;

import footballmarket.models.exceptions.InvalidPlayerException;
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

/** Asociación inmutable entre un jugador interno y una identidad de un proveedor. */
@Entity
@Table(
    name = "player_external_references",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_player_external_references_provider_external_id",
            columnNames = {"provider", "external_id"}))
@Getter
public class PlayerExternalReference {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_reference_ids")
  @SequenceGenerator(
      name = "player_reference_ids",
      sequenceName = "player_external_references_id_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false, updatable = false)
  private Player player;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 64)
  private PlayerProvider provider;

  @Column(name = "external_id", nullable = false, updatable = false)
  private String externalId;

  protected PlayerExternalReference() {}

  PlayerExternalReference(Player player, PlayerProvider provider, String externalId) {
    if (player == null
        || provider == null
        || externalId == null
        || externalId.isBlank()
        || externalId.length() > 255) {
      throw new InvalidPlayerException(
          "La referencia externa requiere jugador, proveedor e identificador");
    }
    this.player = player;
    this.provider = provider;
    this.externalId = externalId;
  }
}
