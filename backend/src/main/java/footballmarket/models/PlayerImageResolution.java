package footballmarket.models;

import footballmarket.models.enums.PlayerImageResolutionStatus;
import footballmarket.models.exceptions.InvalidPlayerException;
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

/** Estado operativo de enriquecimiento, independiente de las imágenes y de la identidad externa. */
@Entity
@Table(name = "player_image_resolutions")
@Getter
public class PlayerImageResolution {

  @Id
  @Column(name = "player_id")
  private Long playerId;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PlayerImageResolutionStatus status;

  private Instant lastAttemptAt;

  protected PlayerImageResolution() {}

  /**
   * Crea el estado inicial de resolución para un jugador ya persistido.
   *
   * <p>El identificador se deriva automáticamente desde el jugador mediante {@link MapsId}.
   *
   * @param player jugador persistido al que pertenece la resolución
   */
  public PlayerImageResolution(Player player) {
    validatePlayer(player);

    this.player = player;
    this.status = PlayerImageResolutionStatus.PENDING;
  }

  /** Registra un intento real y su resultado; las omisiones no alteran la fecha. */
  public void recordAttempt(PlayerImageResolutionStatus result, Instant at) {
    if (result == null || result == PlayerImageResolutionStatus.PENDING || at == null) {
      throw new InvalidPlayerException("El resultado y la fecha del intento son obligatorios");
    }

    validateAttemptDate(at);

    this.status = result;
    this.lastAttemptAt = at;
  }

  /** Registra por separado cada intento real de esta ejecución, incluidos los reintentos. */
  public void markAttempt(Instant at) {
    if (at == null) {
      throw new InvalidPlayerException("La fecha del intento es obligatoria");
    }

    validateAttemptDate(at);

    this.lastAttemptAt = at;
  }

  private static void validatePlayer(Player player) {
    if (player == null || player.getId() == null) {
      throw new InvalidPlayerException("La resolución requiere un jugador persistido");
    }
  }

  private void validateAttemptDate(Instant at) {
    if (this.lastAttemptAt != null && at.isBefore(this.lastAttemptAt)) {
      throw new InvalidPlayerException("La fecha de un intento no puede retroceder");
    }
  }
}
