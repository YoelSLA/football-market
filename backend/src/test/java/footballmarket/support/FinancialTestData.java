package footballmarket.support;

import footballmarket.models.MarketTransaction;
import footballmarket.models.Player;
import footballmarket.models.PlayerToken;
import footballmarket.models.PlayerTokenEmission;
import footballmarket.models.User;
import footballmarket.models.enums.UserRole;
import footballmarket.repositories.MarketTransactionRepository;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.PlayerTokenEmissionRepository;
import footballmarket.repositories.PlayerTokenRepository;
import footballmarket.repositories.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixtures compartidos de historial, utilizados exclusivamente durante Arrange y sin assertions.
 *
 * <p>Antes de registrar una operación confirma que el jugador tenga su emisión inicial en poder del
 * superusuario, como exige el mercado: sin ella el vendedor no tendría tokens que vender y la
 * reconstrucción rechazaría la posición por inconsistencia, no por un defecto del cálculo. La
 * emisión se fecha antes de la primera operación para que el ciclo de tenencia sea coherente.
 */
@Component
public class FinancialTestData {
  private final MarketTransactionRepository transactions;
  private final PlayerRepository players;
  private final PlayerTokenEmissionRepository emissions;
  private final PlayerTokenRepository holdings;
  private final UserRepository users;

  public FinancialTestData(
      MarketTransactionRepository transactionRepository,
      PlayerRepository playerRepository,
      PlayerTokenEmissionRepository emissionRepository,
      PlayerTokenRepository holdingRepository,
      UserRepository userRepository) {
    transactions = transactionRepository;
    players = playerRepository;
    emissions = emissionRepository;
    holdings = holdingRepository;
    users = userRepository;
  }

  /** Prepara una auditoría confirmada sin ejercer el caso de uso de reconstrucción bajo prueba. */
  @Transactional
  public MarketTransaction record(
      User buyer, User seller, Player player, int quantity, long price, OffsetDateTime executedAt) {
    ensureInitialEmission(player, executedAt);
    long amount = Math.multiplyExact(quantity, price);
    return transactions.saveAndFlush(
        MarketTransaction.record(
            buyer,
            seller,
            player,
            quantity,
            price,
            amount,
            amount,
            0,
            0,
            amount,
            0,
            quantity,
            100,
            100 - quantity,
            executedAt,
            "fixture",
            executedAt));
  }

  /** Prepara la desactivación lógica sin eliminar la historia financiera. */
  @Transactional
  public void deactivate(Player player) {
    players.findById(player.getId()).orElseThrow().deactivate();
  }

  /**
   * Aprovisiona la emisión inicial del jugador en poder del superusuario, una sola vez por jugador.
   *
   * <p>La emisión pertenece siempre al superusuario, que es el propietario inicial del inventario,
   * con independencia de quién sea el vendedor de una operación posterior.
   */
  private void ensureInitialEmission(Player player, OffsetDateTime executedAt) {
    if (emissions.existsById(player.getId())) {
      return;
    }
    User superuser = users.findByRole(UserRole.SUPERUSER).orElseThrow();
    emissions.save(
        PlayerTokenEmission.restore(
            player.getId(),
            superuser,
            PlayerTokenEmission.EMITTED_TOKENS,
            executedAt.minusSeconds(1)));
    holdings.save(PlayerToken.issued(player, superuser, PlayerTokenEmission.EMITTED_TOKENS));
  }
}
