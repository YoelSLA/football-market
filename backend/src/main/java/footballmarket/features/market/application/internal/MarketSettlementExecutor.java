package footballmarket.features.market.application.internal;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.exceptions.ArithmeticOverflowException;
import footballmarket.features.market.application.exceptions.InsufficientCreditsException;
import footballmarket.features.market.application.exceptions.InsufficientStockException;
import footballmarket.features.market.application.exceptions.ListingNotAvailableException;
import footballmarket.features.market.application.exceptions.SellerUnavailableException;
import footballmarket.features.market.domain.exceptions.InsufficientUserCreditsException;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.models.PlayerToken;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.domain.models.UserCredit;
import footballmarket.features.market.infraestructure.MarketTransactionRepository;
import footballmarket.features.market.infraestructure.PlayerTokenRepository;
import footballmarket.features.market.infraestructure.UserCreditRepository;
import footballmarket.features.quote.domain.records.CurrentQuote;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Componente interno responsable de la ejecución técnica y liquidación atómica en el mercado.
 *
 * <p>Encapsula los bloqueos deterministas para evitar deadlocks (adquisición ordenada en orden
 * ascendente por identificador de usuario y propietario), las transferencias simétricas de crédito
 * y tokens, y la persistencia inmutable del registro de auditoría {@link MarketTransaction}.
 *
 * <p>Forma parte del rol Service (arquitectura §1.4) y comparte la frontera transaccional de los
 * casos de uso que lo componen.
 */
@Component
@RequiredArgsConstructor
public class MarketSettlementExecutor {

  private final UserCreditRepository credits;
  private final PlayerTokenRepository holdings;
  private final MarketTransactionRepository transactions;

  /**
   * Bloquea las cuentas de crédito de comprador y vendedor para el mercado oficial en orden
   * ascendente de identificador.
   *
   * @param buyerId identificador del comprador
   * @param sellerId identificador del vendedor (superusuario)
   * @return mapa de cuentas bloqueadas indexadas por identificador de usuario
   */
  public Map<Long, UserCredit> lockOfficialAccounts(Long buyerId, Long sellerId) {
    List<UserCredit> locked = this.credits.lockAccountsInAscendingOrder(List.of(buyerId, sellerId));

    Map<Long, UserCredit> byUser =
        locked.stream().collect(Collectors.toMap(UserCredit::getUserId, Function.identity()));

    if (!byUser.containsKey(buyerId)) {
      throw new InsufficientCreditsException();
    }
    if (!byUser.containsKey(sellerId)) {
      throw new SellerUnavailableException();
    }
    return byUser;
  }

  /**
   * Bloquea las cuentas de crédito de comprador y vendedor para una publicación P2P en orden
   * ascendente estricto de identificador.
   *
   * <p>Realiza bloqueos individuales ordenados para garantizar que cualquier cuenta ausente se
   * traduzca como publicación no disponible.
   *
   * @param buyerId identificador del comprador
   * @param sellerId identificador del vendedor particular
   * @return mapa de cuentas bloqueadas indexadas por identificador de usuario
   */
  public Map<Long, UserCredit> lockP2PAccounts(Long buyerId, Long sellerId) {
    Map<Long, UserCredit> accounts = new HashMap<>();
    for (Long userId : List.of(buyerId, sellerId).stream().sorted().toList()) {
      List<UserCredit> locked = this.credits.lockAccountsInAscendingOrder(List.of(userId));
      if (locked.isEmpty()) {
        throw new ListingNotAvailableException();
      }
      accounts.put(userId, locked.getFirst());
    }
    return accounts;
  }

  /**
   * Inserta la primera tenencia del comprador si está ausente y bloquea ambas tenencias en orden
   * ascendente de propietario para la compra oficial.
   *
   * @param playerId identificador del jugador
   * @param buyerId identificador del comprador
   * @param sellerId identificador del vendedor
   * @return mapa de tenencias bloqueadas indexadas por propietario
   */
  public Map<Long, PlayerToken> lockOfficialHoldings(Long playerId, Long buyerId, Long sellerId) {
    this.holdings.insertHoldingIfAbsent(playerId, buyerId);

    List<PlayerToken> locked =
        this.holdings.lockHoldingsInAscendingOwnerOrder(playerId, List.of(buyerId, sellerId));

    Map<Long, PlayerToken> byOwner =
        locked.stream()
            .collect(Collectors.toMap(token -> token.getOwner().getId(), Function.identity()));

    if (!byOwner.containsKey(buyerId) || !byOwner.containsKey(sellerId)) {
      throw new InsufficientStockException();
    }
    return byOwner;
  }

  /**
   * Inserta la primera tenencia del comprador si está ausente y bloquea ambas tenencias de forma
   * individual en orden ascendente estricto para una compra P2P.
   *
   * @param playerId identificador del jugador
   * @param buyerId identificador del comprador
   * @param sellerId identificador del vendedor
   * @return mapa de tenencias bloqueadas indexadas por propietario
   */
  public Map<Long, PlayerToken> lockP2PHoldings(Long playerId, Long buyerId, Long sellerId) {
    this.holdings.insertHoldingIfAbsent(playerId, buyerId);
    Map<Long, PlayerToken> tokens = new HashMap<>();
    for (Long userId : List.of(buyerId, sellerId).stream().sorted().toList()) {
      List<PlayerToken> locked =
          this.holdings.lockHoldingsInAscendingOwnerOrder(playerId, List.of(userId));
      if (locked.isEmpty()) {
        throw new InsufficientStockException();
      }
      tokens.put(userId, locked.getFirst());
    }
    return tokens;
  }

  /**
   * Revalida saldo y stock exclusivamente con los valores obtenidos bajo bloqueo.
   *
   * @param accounts cuentas bloqueadas
   * @param tokens tenencias bloqueadas
   * @param buyerId comprador
   * @param sellerId vendedor
   * @param totalAmount importe total requerido
   * @param quantity cantidad de tokens requerida
   */
  public void revalidate(
      Map<Long, UserCredit> accounts,
      Map<Long, PlayerToken> tokens,
      Long buyerId,
      Long sellerId,
      long totalAmount,
      int quantity) {

    if (!accounts.get(buyerId).covers(totalAmount)) {
      throw new InsufficientCreditsException();
    }
    if (tokens.get(sellerId).getQuantity() < quantity) {
      throw new InsufficientStockException();
    }
  }

  /**
   * Aplica las transferencias simétricas de créditos y tokens entre comprador y vendedor.
   *
   * @param accounts cuentas bloqueadas
   * @param tokens tenencias bloqueadas
   * @param buyerId comprador
   * @param sellerId vendedor
   * @param totalAmount importe total en centésimas
   * @param quantity cantidad de tokens transferidos
   */
  public void transfer(
      Map<Long, UserCredit> accounts,
      Map<Long, PlayerToken> tokens,
      Long buyerId,
      Long sellerId,
      long totalAmount,
      int quantity) {

    try {
      accounts.get(buyerId).debit(totalAmount);
    } catch (InsufficientUserCreditsException insufficient) {
      throw new InsufficientCreditsException();
    }
    try {
      accounts.get(sellerId).credit(totalAmount);
    } catch (ArithmeticException overflow) {
      throw new ArithmeticOverflowException();
    }
    tokens.get(sellerId).transferTo(quantity, tokens.get(buyerId));
  }

  /**
   * Registra y persiste la transacción inmutable de auditoría para una compra en el Mercado
   * Oficial.
   *
   * @param buyer comprador
   * @param seller superusuario emisor
   * @param player jugador comprado
   * @param quantity cantidad de tokens
   * @param quote cotización vigente aplicada
   * @param totalAmount importe total en centésimas
   * @param buyerCreditsBefore créditos comprador previos
   * @param buyerCreditsAfter créditos comprador posteriores
   * @param sellerCreditsBefore créditos vendedor previos
   * @param sellerCreditsAfter créditos vendedor posteriores
   * @param buyerTokensBefore tokens comprador previos
   * @param buyerTokensAfter tokens comprador posteriores
   * @param sellerTokensBefore tokens vendedor previos
   * @param sellerTokensAfter tokens vendedor posteriores
   * @param confirmedAt instante de confirmación
   * @return transacción inmutable persistida
   */
  public MarketTransaction recordOfficial(
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
      int sellerTokensAfter,
      OffsetDateTime confirmedAt) {

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
            confirmedAt));
  }

  /**
   * Registra y persiste la transacción inmutable de auditoría para la liquidación de una
   * publicación P2P.
   *
   * @param buyer comprador
   * @param seller vendedor comunitario
   * @param listing publicación liquidada
   * @param buyerCreditsBefore créditos comprador previos
   * @param buyerCreditsAfter créditos comprador posteriores
   * @param sellerCreditsBefore créditos vendedor previos
   * @param sellerCreditsAfter créditos vendedor posteriores
   * @param buyerTokensBefore tokens comprador previos
   * @param buyerTokensAfter tokens comprador posteriores
   * @param sellerTokensBefore tokens vendedor previos
   * @param sellerTokensAfter tokens vendedor posteriores
   * @param confirmedAt instante de confirmación validado
   * @return transacción inmutable persistida
   */
  public MarketTransaction recordP2P(
      User buyer,
      User seller,
      TokenListing listing,
      long buyerCreditsBefore,
      long buyerCreditsAfter,
      long sellerCreditsBefore,
      long sellerCreditsAfter,
      int buyerTokensBefore,
      int buyerTokensAfter,
      int sellerTokensBefore,
      int sellerTokensAfter,
      OffsetDateTime confirmedAt) {

    return this.transactions.save(
        MarketTransaction.record(
            buyer,
            seller,
            listing.getPlayer(),
            listing.getQuantity(),
            listing.getUnitPrice(),
            listing.getTotalAmount(),
            buyerCreditsBefore,
            buyerCreditsAfter,
            sellerCreditsBefore,
            sellerCreditsAfter,
            buyerTokensBefore,
            buyerTokensAfter,
            sellerTokensBefore,
            sellerTokensAfter,
            listing.getQuoteCalculatedAt(),
            listing.getQuoteStrategyVersion(),
            confirmedAt,
            listing));
  }
}
