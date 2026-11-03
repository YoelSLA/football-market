package footballmarket.features.market.application.impl;

import footballmarket.features.auth.application.exceptions.UserNotFoundException;
import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.domain.enums.UserRole;
import footballmarket.features.auth.infraestructure.UserRepository;
import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.catalog.application.exceptions.PlayerNotVisibleException;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.market.application.BuyOrderService;
import footballmarket.features.market.application.exceptions.ArithmeticOverflowException;
import footballmarket.features.market.application.exceptions.CannotBuyOwnListingException;
import footballmarket.features.market.application.exceptions.InsufficientCreditsException;
import footballmarket.features.market.application.exceptions.InsufficientStockException;
import footballmarket.features.market.application.exceptions.InvalidOrderRequestException;
import footballmarket.features.market.application.exceptions.InvalidQuantityException;
import footballmarket.features.market.application.exceptions.ListingNotAvailableException;
import footballmarket.features.market.application.exceptions.ListingNotFoundException;
import footballmarket.features.market.application.exceptions.SellerUnavailableException;
import footballmarket.features.market.application.exceptions.SuperuserCannotBuyException;
import footballmarket.features.market.application.internal.MarketInitializationGuard;
import footballmarket.features.market.domain.enums.TokenListingStatus;
import footballmarket.features.market.domain.exceptions.InsufficientUserCreditsException;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.models.PlayerToken;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.domain.models.UserCredit;
import footballmarket.features.market.domain.records.BuyListingRequest;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.market.domain.records.ListingIdentity;
import footballmarket.features.market.infraestructure.MarketTransactionRepository;
import footballmarket.features.market.infraestructure.PlayerTokenRepository;
import footballmarket.features.market.infraestructure.TokenListingRepository;
import footballmarket.features.market.infraestructure.UserCreditRepository;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.application.exceptions.InvalidQuotePriceException;
import footballmarket.features.quote.application.exceptions.QuoteContractViolationException;
import footballmarket.features.quote.application.exceptions.QuoteUnavailableException;
import footballmarket.features.quote.domain.records.CurrentQuote;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación transaccional de la compra de tokens.
 *
 * <p>Ejecuta una única transacción de Spring que resuelve comprador, vendedor, elegibilidad,
 * cotización, bloqueos, transferencias y auditoría. Cualquier excepción revierte la operación
 * completa: no existen movimientos parciales ni registros de compras no confirmadas.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BuyOrderServiceImpl implements BuyOrderService {

  /**
   * Mensaje del rechazo por jugador inexistente.
   *
   * <p>Coincide con el de {@link PlayerNotVisibleException}: la compra no promete ocultación
   * absoluta de existencia, solo un código distinto para cada situación.
   */
  private static final String PLAYER_NOT_FOUND_MESSAGE = "No encontramos el jugador solicitado";

  private final UserRepository users;
  private final PlayerRepository players;
  private final UserCreditRepository credits;
  private final PlayerTokenRepository holdings;
  private final MarketTransactionRepository transactions;
  private final QuoteSource quotes;
  private final TokenListingRepository listings;
  private final MarketInitializationGuard marketInitialization;
  private final Clock marketClock;

  /** {@inheritDoc} */
  @Override
  public MarketTransaction buyListing(String buyerEmail, BuyListingRequest order) {
    User buyer = this.resolveBuyer(buyerEmail);
    if (buyer.getRole() == UserRole.SUPERUSER) {
      throw new SuperuserCannotBuyException();
    }
    if (order == null || order.listingId() == null) {
      throw new InvalidOrderRequestException();
    }
    // El mercado debe existir antes de operar: el rechazo ocurre antes de leer la publicación y
    // antes de cualquier movimiento de recursos.
    this.marketInitialization.requireInitialized();
    ListingIdentity identity =
        this.listings.readIdentity(order.listingId()).orElseThrow(ListingNotFoundException::new);
    if (!identity.sellerId().equals(buyer.getId())
        && this.players.findVisibleById(identity.playerId()).isEmpty()) {
      throw new ListingNotFoundException();
    }
    if (identity.sellerId().equals(buyer.getId())) {
      throw new CannotBuyOwnListingException();
    }
    // Identificadores ordenados en aplicación; cada consulta toma una sola cuenta por clave.
    Map<Long, UserCredit> accounts = new HashMap<>();
    for (Long userId : List.of(buyer.getId(), identity.sellerId()).stream().sorted().toList()) {
      List<UserCredit> locked = this.credits.lockAccountsInAscendingOrder(List.of(userId));
      if (locked.isEmpty()) {
        throw new ListingNotAvailableException();
      }
      accounts.put(userId, locked.getFirst());
    }
    TokenListing listing =
        this.listings.lockById(order.listingId()).orElseThrow(ListingNotFoundException::new);
    if (!listing.getSeller().getId().equals(identity.sellerId())
        || listing.getStatus() != TokenListingStatus.ACTIVE) {
      throw new ListingNotAvailableException();
    }
    if (listing.getSeller().getId().equals(buyer.getId())) {
      throw new CannotBuyOwnListingException();
    }
    if (this.players.findVisibleById(listing.getPlayer().getId()).isEmpty()) {
      throw new ListingNotFoundException();
    }
    User seller = listing.getSeller();
    this.holdings.insertHoldingIfAbsent(listing.getPlayer().getId(), buyer.getId());
    Map<Long, PlayerToken> tokens = new HashMap<>();
    for (Long userId : List.of(buyer.getId(), seller.getId()).stream().sorted().toList()) {
      List<PlayerToken> locked =
          this.holdings.lockHoldingsInAscendingOwnerOrder(
              listing.getPlayer().getId(), List.of(userId));
      if (locked.isEmpty()) {
        throw new InsufficientStockException();
      }
      tokens.put(userId, locked.getFirst());
    }
    // C1: el reloj se consulta solo después de adquirir ambas tenencias. Una espera puede
    // haber alcanzado expiresAt; nunca autorizar con un instante anterior a esa espera.
    OffsetDateTime validatedAt = OffsetDateTime.now(this.marketClock);
    if (!listing.isActiveAndValid(validatedAt)) {
      throw new ListingNotAvailableException();
    }
    this.revalidate(
        accounts, tokens, buyer, seller, listing.getTotalAmount(), listing.getQuantity());
    long buyerBefore = accounts.get(buyer.getId()).getAvailableCredits();
    long sellerBefore = accounts.get(seller.getId()).getAvailableCredits();
    int buyerTokensBefore = tokens.get(buyer.getId()).getQuantity();
    int sellerTokensBefore = tokens.get(seller.getId()).getQuantity();
    this.transfer(accounts, tokens, buyer, seller, listing.getTotalAmount(), listing.getQuantity());
    // Reutiliza el instante validado, aunque el reloj avance durante la liquidación.
    listing.markSold(validatedAt);
    return this.transactions.save(
        MarketTransaction.record(
            buyer,
            seller,
            listing.getPlayer(),
            listing.getQuantity(),
            listing.getUnitPrice(),
            listing.getTotalAmount(),
            buyerBefore,
            accounts.get(buyer.getId()).getAvailableCredits(),
            sellerBefore,
            accounts.get(seller.getId()).getAvailableCredits(),
            buyerTokensBefore,
            tokens.get(buyer.getId()).getQuantity(),
            sellerTokensBefore,
            tokens.get(seller.getId()).getQuantity(),
            listing.getQuoteCalculatedAt(),
            listing.getQuoteStrategyVersion(),
            validatedAt,
            listing));
  }

  /** {@inheritDoc} */
  @Override
  public MarketTransaction buy(String buyerEmail, BuyOrderRequest order) {

    // 1. El comprador es el sujeto autenticado: la solicitud no puede elegir otra identidad.
    User buyer = this.resolveBuyer(buyerEmail);

    // 2. El rol se lee de la persistencia, no de los claims del token.
    if (buyer.getRole() == UserRole.SUPERUSER) {
      throw new SuperuserCannotBuyException();
    }

    // 3. La cantidad tiene un rango de negocio propio, distinto del del identificador.
    if (order.quantity() < 1 || order.quantity() > 100) {
      throw new InvalidQuantityException();
    }

    // 4. Existencia y visibilidad del jugador, antes de bloquear o mover ningún recurso.
    Player player = this.resolvePlayer(order.playerId());

    // 5. El vendedor debe ser identificable antes de iniciar la transferencia.
    User seller = this.resolveSeller();

    // 6. El mercado debe existir antes de operar: el rechazo ocurre antes de leer la cotización y
    // antes de cualquier movimiento de recursos.
    this.marketInitialization.requireInitialized();

    // 7. La cotización se lee una sola vez y ese precio viaja a toda la operación.
    CurrentQuote quote = this.readQuote(player.getId());

    // 8. El importe se calcula una única vez con aritmética exacta.
    long totalAmount = this.calculateTotal(order.quantity(), quote.price());

    // 9. Cuentas de comprador y vendedor bloqueadas en orden ascendente de user_id.
    Map<Long, UserCredit> accounts = this.lockAccounts(buyer, seller);

    // 10. Tenencias bloqueadas en orden ascendente de propietario.
    Map<Long, PlayerToken> tokens = this.lockHoldings(player, buyer, seller);

    // 11. Revalidación con los valores obtenidos bajo bloqueo.
    this.revalidate(accounts, tokens, buyer, seller, totalAmount, order.quantity());

    long buyerCreditsBefore = accounts.get(buyer.getId()).getAvailableCredits();
    long sellerCreditsBefore = accounts.get(seller.getId()).getAvailableCredits();
    int buyerTokensBefore = tokens.get(buyer.getId()).getQuantity();
    int sellerTokensBefore = tokens.get(seller.getId()).getQuantity();

    // 12. Transferencias simétricas de créditos y tokens.
    this.transfer(accounts, tokens, buyer, seller, totalAmount, order.quantity());

    // 13. Auditoría en la misma transacción, con los estados ya observados bajo bloqueo.
    return this.record(
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
        tokens.get(seller.getId()).getQuantity());
  }

  private User resolveBuyer(String buyerEmail) {
    return this.users
        .findByEmail(buyerEmail.toLowerCase(Locale.ROOT))
        .orElseThrow(() -> new UserNotFoundException("El comprador de la sesión no existe."));
  }

  /**
   * Resuelve el jugador solicitado distinguiendo su inexistencia de su falta de visibilidad.
   *
   * <p>Una consulta de visibilidad por sí sola devuelve vacío en ambos casos, de modo que la
   * existencia se consulta primero y la visibilidad se comprueba solo cuando el jugador existe. Se
   * reutiliza el predicado vigente del catálogo en lugar de duplicarlo.
   *
   * @param playerId identificador del jugador solicitado
   * @return entidad del jugador visible
   */
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

  /**
   * Lee la cotización y valida su contrato antes de cualquier cálculo.
   *
   * <p>El orden importa: primero la correspondencia del jugador y después la validez del precio, de
   * modo que una cotización ajena no se evalúe como si fuera válida.
   *
   * @param playerId jugador solicitado
   * @return instantánea de la cotización vigente
   */
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

  /**
   * Bloquea las cuentas de comprador y vendedor en orden ascendente de identificador.
   *
   * <p>El orden se establece en la consulta y no en el orden de los argumentos, para que sea
   * determinista con independencia de quién llega primero. Una cuenta ausente no se crea durante la
   * compra: no está financiada.
   *
   * @return cuentas bloqueadas indexadas por identificador de usuario
   */
  private Map<Long, UserCredit> lockAccounts(User buyer, User seller) {
    List<UserCredit> locked =
        this.credits.lockAccountsInAscendingOrder(List.of(buyer.getId(), seller.getId()));

    Map<Long, UserCredit> byUser =
        locked.stream().collect(Collectors.toMap(UserCredit::getUserId, Function.identity()));

    if (!byUser.containsKey(buyer.getId())) {
      throw new InsufficientCreditsException();
    }
    if (!byUser.containsKey(seller.getId())) {
      throw new SellerUnavailableException();
    }
    return byUser;
  }

  /**
   * Inserta la primera tenencia del comprador si falta y bloquea ambas tenencias.
   *
   * <p>La inserción ignorando el conflicto serializa la creación concurrente de la primera
   * tenencia. No sustituye al bloqueo de lectura: cuando la fila ya existe la operación se descarta
   * y no bloquea esa fila, de modo que el bloqueo efectivo lo aporta la consulta siguiente.
   *
   * @return tenencias bloqueadas indexadas por propietario
   */
  private Map<Long, PlayerToken> lockHoldings(Player player, User buyer, User seller) {
    this.holdings.insertHoldingIfAbsent(player.getId(), buyer.getId());

    List<PlayerToken> locked =
        this.holdings.lockHoldingsInAscendingOwnerOrder(
            player.getId(), List.of(buyer.getId(), seller.getId()));

    Map<Long, PlayerToken> byOwner =
        locked.stream()
            .collect(Collectors.toMap(token -> token.getOwner().getId(), Function.identity()));

    if (!byOwner.containsKey(buyer.getId()) || !byOwner.containsKey(seller.getId())) {
      throw new InsufficientStockException();
    }
    return byOwner;
  }

  /** Revalida saldo y stock exclusivamente con los valores obtenidos bajo bloqueo. */
  private void revalidate(
      Map<Long, UserCredit> accounts,
      Map<Long, PlayerToken> tokens,
      User buyer,
      User seller,
      long totalAmount,
      int quantity) {

    if (!accounts.get(buyer.getId()).covers(totalAmount)) {
      throw new InsufficientCreditsException();
    }
    if (tokens.get(seller.getId()).getQuantity() < quantity) {
      throw new InsufficientStockException();
    }
  }

  /** Aplica las transferencias de créditos y tokens entre las dos partes. */
  private void transfer(
      Map<Long, UserCredit> accounts,
      Map<Long, PlayerToken> tokens,
      User buyer,
      User seller,
      long totalAmount,
      int quantity) {

    try {
      accounts.get(buyer.getId()).debit(totalAmount);
    } catch (InsufficientUserCreditsException insufficient) {
      throw new InsufficientCreditsException();
    }
    try {
      accounts.get(seller.getId()).credit(totalAmount);
    } catch (ArithmeticException overflow) {
      throw new ArithmeticOverflowException();
    }
    tokens.get(seller.getId()).transferTo(quantity, tokens.get(buyer.getId()));
  }

  private MarketTransaction record(
      User buyer,
      User seller,
      Player player,
      int quantity,
      CurrentQuote quote,
      long totalAmount,
      long buyerCreditsBefore,
      long buyerCreditsAfter,
      long sellerCreditsBefore,
      long sellerCreditsAfter,
      int buyerTokensBefore,
      int buyerTokensAfter,
      int sellerTokensBefore,
      int sellerTokensAfter) {

    return this.transactions.save(
        MarketTransaction.record(
            buyer,
            seller,
            player,
            quantity,
            quote.price(),
            totalAmount,
            buyerCreditsBefore,
            buyerCreditsAfter,
            sellerCreditsBefore,
            sellerCreditsAfter,
            buyerTokensBefore,
            buyerTokensAfter,
            sellerTokensBefore,
            sellerTokensAfter,
            quote.calculatedAt(),
            quote.strategyVersion(),
            OffsetDateTime.now(this.marketClock)));
  }
}
