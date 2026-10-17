package footballmarket.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Emisión inicial de tokens de un jugador.
 *
 * <p>Existe exactamente una por jugador y no se modifica: su función es probar que la emisión ya
 * ocurrió, de modo que reejecutar la habilitación no repone inventario vendido. La cantidad es
 * siempre {@code 100}, tanto por la invariante del modelo como por la restricción de la base de
 * datos.
 */
@Entity
@Table(name = "player_token_emissions")
@Getter
@NoArgsConstructor
public class PlayerTokenEmission {

  /** Cantidad emitida por jugador. No es un parámetro: la fija el dominio del mercado. */
  public static final int EMITTED_TOKENS = 100;

  @Id
  @Column(name = "player_id")
  private Long playerId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "owner_user_id", nullable = false)
  private User owner;

  @Column(name = "initial_quantity", nullable = false)
  private int initialQuantity;

  @Column(name = "emitted_at", nullable = false)
  private OffsetDateTime emittedAt;

  private PlayerTokenEmission(Long playerId, User owner, int initialQuantity) {
    this.playerId = playerId;
    this.owner = owner;
    this.initialQuantity = initialQuantity;
    this.emittedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  /**
   * Registra la emisión inicial de un jugador en poder del superusuario.
   *
   * @param playerId jugador emitido
   * @param owner propietario inicial, que es el superusuario
   * @return emisión registrada con la cantidad vigente
   */
  public static PlayerTokenEmission issue(Long playerId, User owner) {
    return new PlayerTokenEmission(playerId, owner, EMITTED_TOKENS);
  }

  /**
   * Restaura una emisión ya persistida.
   *
   * @param playerId jugador emitido
   * @param owner propietario inicial
   * @param initialQuantity cantidad persistida
   * @param emittedAt marca de tiempo persistida
   * @return emisión con el estado indicado
   */
  public static PlayerTokenEmission restore(
      Long playerId, User owner, int initialQuantity, OffsetDateTime emittedAt) {
    PlayerTokenEmission emission = new PlayerTokenEmission(playerId, owner, initialQuantity);
    if (emittedAt != null) {
      emission.emittedAt = emittedAt;
    }
    return emission;
  }
}
