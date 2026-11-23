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
import footballmarket.features.market.application.exceptions.InsufficientStockException;
import footballmarket.features.market.application.exceptions.InvalidQuantityException;
import footballmarket.features.market.application.exceptions.SellerUnavailableException;
import footballmarket.features.market.application.exceptions.SuperuserCannotSellException;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.models.PlayerToken;
import footballmarket.features.market.domain.models.UserCredit;
import footballmarket.features.market.domain.records.OfficialBuybackReceipt;
import footballmarket.features.market.domain.records.OfficialBuybackRequest;
import footballmarket.features.market.infraestructure.TokenListingRepository;
import footballmarket.features.market.infraestructure.config.OfficialBuybackProperties;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.application.exceptions.InvalidQuotePriceException;
import footballmarket.features.quote.application.exceptions.QuoteContractViolationException;
import footballmarket.features.quote.application.exceptions.QuoteUnavailableException;
import footballmarket.features.quote.domain.records.CurrentQuote;
import java.math.BigInteger;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ejecutor interno de la venta oficial inmediata.
 *
 * <p>Resuelve al vendedor autenticado y al superusuario comprador desde la persistencia, valida
 * elegibilidad, cantidad, cotización y tenencia libre, calcula el precio neto con aritmética entera
 * exacta y delega en {@link MarketSettlementExecutor} los bloqueos deterministas, las
 * transferencias y el registro financiero.
 */
@Component
@RequiredArgsConstructor
public class OfficialBuybackExecutor {

  private static final String PLAYER_NOT_FOUND_MESSAGE = "No encontramos el jugador solicitado";

  private final UserRepository users;
  private final PlayerRepository players;
  private final QuoteSource quotes;
  private final TokenListingRepository listings;
  private final MarketInitializationGuard marketInitialization;
  private final MarketSettlementExecutor settlement;
  private final OfficialBuybackProperties properties;
  private final Clock marketClock;

  /**
   * Ejecuta la venta oficial de tokens libres del usuario autenticado al superusuario.
   *
   * @param sellerEmail correo del vendedor autenticado
   * @param order solicitud con jugador y cantidad
   * @return comprobante de la venta confirmada
   */
  public OfficialBuybackReceipt execute(String sellerEmail, OfficialBuybackRequest order) {
    // 1. El vendedor es el sujeto autenticado: la solicitud no puede elegir otra identidad.
    User seller = this.resolveSeller(sellerEmail);

    // 2. El rol se lee de la persistencia, no de los claims del token.
    if (seller.getRole() == UserRole.SUPERUSER) {
      throw new SuperuserCannotSellException();
    }

    // 3. La cantidad tiene un rango de negocio propio (1 a 100).
    if (order.quantity() < 1 || order.quantity() > 100) {
      throw new InvalidQuantityException();
    }

    // 4. Existencia y visibilidad del jugador, antes de bloquear o mover recursos.
    Player player = this.resolvePlayer(order.playerId());

    // 5. El comprador debe ser identificable (Superusuario).
    User buyer = this.resolveBuyer();

    // 6. El mercado debe estar inicializado antes de operar.
    this.marketInitialization.requireInitialized();

    // 7. La cotización se lee una sola vez y viaja a toda la operación.
    CurrentQuote quote = this.readQuote(player.getId());

    // 8. El precio neto y el importe se calculan antes de bloquear o modificar recursos.
    int discountPercentage = this.properties.discountPercentage();
    long unitPrice = this.calculateNetUnitPrice(quote.price(), discountPercentage);
    long totalAmount = this.calculateTotal(order.quantity(), unitPrice);

    // 9. Cuentas bloqueadas en orden ascendente de user_id.
    Map<Long, UserCredit> accounts =
        this.settlement.lockOfficialAccounts(buyer.getId(), seller.getId());

    // 10. Tenencias bloqueadas en orden ascendente de propietario.
    Map<Long, PlayerToken> tokens =
        this.settlement.lockOfficialHoldings(player.getId(), buyer.getId(), seller.getId());

    // 11. Revalidación de saldo y tenencia con los valores obtenidos bajo bloqueo.
    this.settlement.revalidate(
        accounts, tokens, buyer.getId(), seller.getId(), totalAmount, order.quantity());

    // 12. La tenencia libre excluye los tokens reservados en publicaciones P2P vigentes.
    this.requireFreeStock(tokens, seller, player, order.quantity());

    long buyerCreditsBefore = accounts.get(buyer.getId()).getAvailableCredits();
    long sellerCreditsBefore = accounts.get(seller.getId()).getAvailableCredits();
    int buyerTokensBefore = tokens.get(buyer.getId()).getQuantity();
    int sellerTokensBefore = tokens.get(seller.getId()).getQuantity();

    // 13. Transferencias simétricas: el superusuario paga y el vendedor entrega tokens.
    this.settlement.transfer(
        accounts, tokens, buyer.getId(), seller.getId(), totalAmount, order.quantity());

    // 14. Auditoría inmutable en la misma transacción.
    MarketTransaction transaction =
        this.settlement.recordBuyback(
            buyer,
            seller,
            player,
            order.quantity(),
            quote,
            discountPercentage,
            unitPrice,
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

    return new OfficialBuybackReceipt(
        transaction.getId(),
        buyer.getId(),
        seller.getId(),
        player.getId(),
        transaction.getQuantity(),
        quote.price(),
        discountPercentage,
        transaction.getUnitPrice(),
        transaction.getTotalAmount(),
        transaction.getExecutedAt());
  }

  private User resolveSeller(String sellerEmail) {
    return this.users
        .findByEmail(sellerEmail.toLowerCase(Locale.ROOT))
        .orElseThrow(() -> new UserNotFoundException("El vendedor de la sesión no existe."));
  }

  private User resolveBuyer() {
    return this.users.findByRole(UserRole.SUPERUSER).orElseThrow(SellerUnavailableException::new);
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

  private long calculateNetUnitPrice(long quotePrice, int discountPercentage) {
    BigInteger numerator =
        BigInteger.valueOf(quotePrice).multiply(BigInteger.valueOf(100L - discountPercentage));
    BigInteger net = numerator.add(BigInteger.valueOf(50)).divide(BigInteger.valueOf(100));
    if (net.signum() <= 0) {
      throw new InvalidQuotePriceException();
    }
    try {
      return net.longValueExact();
    } catch (ArithmeticException overflow) {
      throw new ArithmeticOverflowException();
    }
  }

  private long calculateTotal(int quantity, long unitPrice) {
    try {
      return Math.multiplyExact((long) quantity, unitPrice);
    } catch (ArithmeticException overflow) {
      throw new ArithmeticOverflowException();
    }
  }

  private void requireFreeStock(
      Map<Long, PlayerToken> tokens, User seller, Player player, int quantity) {
    OffsetDateTime now = OffsetDateTime.now(this.marketClock);
    long reserved = this.listings.reservedQuantity(seller.getId(), player.getId(), now);
    long freeStock = (long) tokens.get(seller.getId()).getQuantity() - reserved;
    if (freeStock < quantity) {
      throw new InsufficientStockException();
    }
  }
}
