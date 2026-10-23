package footballmarket.models;

import footballmarket.models.exceptions.InsufficientPlayerTokensException;
import footballmarket.models.exceptions.InvalidTokenQuantityException;
import footballmarket.models.exceptions.TokenPlayerMismatchException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tenencia de tokens de un jugador en poder de un usuario.
 *
 * <p>Una tenencia con cantidad cero es válida: existe para distinguir una primera compra aún no
 * realizada y para simplificar la adquisición de bloqueos. La cantidad nunca es negativa y la suma
 * de tenencias de un jugador se conserva porque toda transferencia es simétrica y exige que emisor
 * y receptor pertenezcan a ese mismo jugador.
 */
@Entity
@Table(name = "player_tokens")
@Getter
@NoArgsConstructor
public class PlayerToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @ManyToOne(optional = false)
  @JoinColumn(name = "owner_user_id", nullable = false)
  private User owner;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  private PlayerToken(Player player, User owner, int quantity) {
    this.player = player;
    this.owner = owner;
    this.quantity = requireNotNegative(quantity);
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  /**
   * Crea la tenencia inicial de un jugador en poder del superusuario.
   *
   * @param player jugador emitido
   * @param owner propietario inicial, que es el superusuario
   * @param quantity cantidad emitida
   * @return tenencia con la cantidad indicada
   */
  public static PlayerToken issued(Player player, User owner, int quantity) {
    return new PlayerToken(player, owner, quantity);
  }

  /**
   * Crea una tenencia en cero para el comprador que todavía no posee ese jugador.
   *
   * @param player jugador comprado
   * @param owner comprador
   * @return tenencia vacía
   */
  public static PlayerToken emptyHolding(Player player, User owner) {
    return new PlayerToken(player, owner, 0);
  }

  /**
   * Restaura una tenencia ya persistida.
   *
   * @param id identificador persistido
   * @param player jugador de la tenencia
   * @param owner propietario
   * @param quantity cantidad persistida
   * @param updatedAt marca de tiempo persistida
   * @return tenencia con el estado indicado
   */
  public static PlayerToken restore(
      Long id, Player player, User owner, int quantity, OffsetDateTime updatedAt) {
    PlayerToken token = new PlayerToken(player, owner, quantity);
    token.id = id;
    if (updatedAt != null) {
      token.updatedAt = updatedAt;
    }
    return token;
  }

  /**
   * Transfiere tokens desde esta tenencia hacia la del comprador.
   *
   * <p>La operación es simétrica: lo que sale de una tenencia entra en la otra, de modo que la suma
   * del jugador se conserva. Esa suma solo tiene sentido si ambas tenencias son del mismo jugador,
   * y por eso el modelo lo exige antes de mover cantidad. Dos tenencias cuyo jugador aún no tiene
   * identificador se consideran del mismo jugador: el modelo no puede afirmar una contradicción que
   * el persistido aún no permite comprobar.
   *
   * @param amount cantidad a transferir
   * @param receiver tenencia que recibe los tokens
   * @throws InvalidTokenQuantityException si la cantidad no es positiva
   * @throws TokenPlayerMismatchException si las dos tenencias son de jugadores distintos
   * @throws InsufficientPlayerTokensException si esta tenencia no alcanza
   */
  public void transferTo(int amount, PlayerToken receiver) {
    if (amount <= 0) {
      throw new InvalidTokenQuantityException();
    }
    if (this.player.getId() != null
        && receiver.player.getId() != null
        && !this.player.getId().equals(receiver.player.getId())) {
      throw new TokenPlayerMismatchException();
    }
    if (amount > this.quantity) {
      throw new InsufficientPlayerTokensException();
    }
    this.quantity -= amount;
    receiver.quantity += amount;
    this.touch();
    receiver.touch();
  }

  private void touch() {
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  private static int requireNotNegative(int value) {
    if (value < 0) {
      throw new InvalidTokenQuantityException();
    }
    return value;
  }
}
