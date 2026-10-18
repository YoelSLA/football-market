package footballmarket.services.internal;

import footballmarket.models.Player;
import footballmarket.models.PlayerToken;
import footballmarket.models.PlayerTokenEmission;
import footballmarket.models.User;
import footballmarket.models.enums.UserRole;
import footballmarket.repositories.PlayerTokenEmissionRepository;
import footballmarket.repositories.PlayerTokenRepository;
import footballmarket.repositories.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra la emisión inicial de tokens de un jugador recién creado.
 *
 * <p>Es un colaborador interno del catálogo, no un Service: no expone interfaz de aplicación ni lo
 * consumen otros Services, y comparte la transacción que crea al jugador. Escribir la emisión en el
 * mismo momento del alta es lo que garantiza que todo jugador operable tenga inventario antes de
 * aceptar compras.
 *
 * <p>La emisión es idempotente: si el jugador ya fue emitido, no se vuelve a emitir ni se repone el
 * inventario vendido. Tampoco actualiza un jugador existente, de modo que repetir la sincronización
 * nunca restituye tokens.
 */
@Component
@RequiredArgsConstructor
public class PlayerTokenEmissionWriter {

  private final PlayerTokenEmissionRepository emissions;
  private final PlayerTokenRepository holdings;
  private final UserRepository users;

  /**
   * Emite los tokens iniciales de un jugador en poder del superusuario.
   *
   * <p>Sin superusuario no hay propietario posible del inventario inicial, de modo que no se emite
   * nada y la compra de ese jugador no podrá confirmarse por falta de vendedor.
   *
   * @param jugador jugador recién creado
   */
  @Transactional
  public void emitFor(Player player) {
    if (this.emissions.existsById(player.getId())) {
      return;
    }

    Optional<User> superuser = this.users.findByRole(UserRole.SUPERUSER);
    if (superuser.isEmpty()) {
      return;
    }

    this.emissions.save(PlayerTokenEmission.issue(player.getId(), superuser.get()));

    if (!this.holdings
        .findByPlayerIdAndOwnerId(player.getId(), superuser.get().getId())
        .isPresent()) {
      this.holdings.save(
          PlayerToken.issued(player, superuser.get(), PlayerTokenEmission.EMITTED_TOKENS));
    }
  }
}
