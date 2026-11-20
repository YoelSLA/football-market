package footballmarket.features.market.application.internal;

import footballmarket.features.auth.application.exceptions.UserNotFoundException;
import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.domain.enums.UserRole;
import footballmarket.features.auth.infraestructure.UserRepository;
import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.catalog.application.exceptions.PlayerNotVisibleException;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.market.application.exceptions.ArithmeticOverflowException;
import footballmarket.features.market.application.exceptions.InvalidQuantityException;
import footballmarket.features.market.application.exceptions.SellerUnavailableException;
import footballmarket.features.market.application.exceptions.SuperuserCannotBuyException;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.models.PlayerToken;
import footballmarket.features.market.domain.models.UserCredit;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.application.exceptions.InvalidQuotePriceException;
import footballmarket.features.quote.application.exceptions.QuoteContractViolationException;
import footballmarket.features.quote.application.exceptions.QuoteUnavailableException;
import footballmarket.features.quote.domain.records.CurrentQuote;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ejecutor interno del caso de uso de compra de tokens en el Mercado Oficial.
 *
 * <p>Orquesta la compra de tokens de emisión primaria directamente contra el Superusuario a la
 * cotización vigente del sistema. Resuelve comprador, elegibilidad deportiva, cotización, cálculo
 * financiero exacto y delega en {@link MarketSettlementExecutor} el bloqueo determinista y la
 * transferencia de recursos.
 */
@Component
@RequiredArgsConstructor
public class OfficialBuyOrderExecutor {

  private static final String PLAYER_NOT_FOUND_MESSAGE = "No encontramos el jugador solicitado";

  private final UserRepository users;
  private final PlayerRepository players;
  private final QuoteSource quotes;
  private final MarketInitializationGuard marketInitialization;
  private final MarketSettlementExecutor settlement;
  private final Clock marketClock;

  /**
   * Ejecuta la orden de compra en el Mercado Oficial.
   *
   * @param buyerEmail correo del comprador autenticado
   * @param order solicitud de compra con jugador y cantidad
   * @return transacción inmutable registrada en auditoría
   */
  public MarketTransaction execute(String buyerEmail, BuyOrderRequest order) {
    // 1. El comprador es el sujeto autenticado: la solicitud no puede elegir otra identidad.
    User buyer = this.resolveBuyer(buyerEmail);

    // 2. El rol se lee de la persistencia, no de los claims del token.
    if (buyer.getRole() == UserRole.SUPERUSER) {
      throw new SuperuserCannotBuyException();
    }

    // 3. La cantidad tiene un rango de negocio propio (1 a 100).
    if (order.quantity() < 1 || order.quantity() > 100) {
      throw new InvalidQuantityException();
    }

    // 4. Existencia y visibilidad del jugador, antes de bloquear o mover recursos.
    Player player = this.resolvePlayer(order.playerId());

    // 5. El vendedor debe ser identificable (Superusuario).
    User seller = this.resolveSeller();

    // 6. El mercado debe estar inicializado antes de operar.
    this.marketInitialization.requireInitialized();

    // 7. La cotización se lee una sola vez y viaja a toda la operación.
    CurrentQuote quote = this.readQuote(player.getId());

    // 8. El importe se calcula una única vez con aritmética exacta.
    long totalAmount = this.calculateTotal(order.quantity(), quote.price());

    // 9. Cuentas de comprador y vendedor bloqueadas en orden ascendente de user_id.
    Map<Long, UserCredit> accounts =
        this.settlement.lockOfficialAccounts(buyer.getId(), seller.getId());

    // 10. Tenencias bloqueadas en orden ascendente de propietario.
    Map<Long, PlayerToken> tokens =
        this.settlement.lockOfficialHoldings(player.getId(), buyer.getId(), seller.getId());

    // 11. Revalidación con los valores obtenidos bajo bloqueo.
    this.settlement.revalidate(
        accounts, tokens, buyer.getId(), seller.getId(), totalAmount, order.quantity());

    long buyerCreditsBefore = accounts.get(buyer.getId()).getAvailableCredits();
    long sellerCreditsBefore = accounts.get(seller.getId()).getAvailableCredits();
    int buyerTokensBefore = tokens.get(buyer.getId()).getQuantity();
    int sellerTokensBefore = tokens.get(seller.getId()).getQuantity();

    // 12. Transferencias simétricas de créditos y tokens.
    this.settlement.transfer(
        accounts, tokens, buyer.getId(), seller.getId(), totalAmount, order.quantity());

    // 13. Auditoría inmutable en la misma transacción.
    return this.settlement.recordOfficial(
        buyer,
        seller,
        player,
        order.quantity(),
        quote,
        totalAmount,
        buyerCreditsBefore,
        accounts.get(buyer.getId()).getAvailableCredits(),
        sellerCreditsBefore,
        accounts.get(seller.getId()).getAvailableCredits(),
        buyerTokensBefore,
        tokens.get(buyer.getId()).getQuantity(),
        sellerTokensBefore,
        tokens.get(seller.getId()).getQuantity(),
        OffsetDateTime.now(this.marketClock));
  }

  private User resolveBuyer(String buyerEmail) {
    return this.users
        .findByEmail(buyerEmail.toLowerCase(Locale.ROOT))
        .orElseThrow(() -> new UserNotFoundException("El comprador de la sesión no existe."));
  }

  private Player resolvePlayer(Long playerId) {
    if (playerId == null) {
      throw new PlayerNotFoundException(PLAYER_NOT_FOUND_MESSAGE);
    }

    Player existing = this.players.findById(playerId).orElse(null);
    if (existing == null) {
      throw new PlayerNotFoundException(PLAYER_NOT_FOUND_MESSAGE);
    }
    if (this.players.findVisibleById(playerId).isEmpty()) {
      throw new PlayerNotVisibleException();
    }
    return existing;
  }

  private User resolveSeller() {
    return this.users.findByRole(UserRole.SUPERUSER).orElseThrow(SellerUnavailableException::new);
  }

  private CurrentQuote readQuote(Long playerId) {
    CurrentQuote quote =
        this.quotes.currentQuote(playerId).orElseThrow(QuoteUnavailableException::new);

    if (!playerId.equals(quote.playerId())) {
      throw new QuoteContractViolationException();
    }
    if (quote.price() <= 0) {
      throw new InvalidQuotePriceException();
    }
    return quote;
  }

  private long calculateTotal(int quantity, long unitPrice) {
    try {
      return Math.multiplyExact((long) quantity, unitPrice);
    } catch (ArithmeticException overflow) {
      throw new ArithmeticOverflowException();
    }
  }
}
