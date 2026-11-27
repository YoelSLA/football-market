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
   * <p>Sirve tanto a la compra oficial como a la venta oficial: en esta última el superusuario es
   * el comprador y el usuario autenticado el vendedor. La ausencia de la cuenta del comprador se
   * traduce como saldo insuficiente y la del vendedor como contraparte no disponible.
   *
   * @param buyerId identificador del comprador
   * @param sellerId identificador del vendedor
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
   * ascendente de propietario para una operación del mercado oficial.
   *
   * <p>Se usa en la compra oficial y en la venta oficial; en esta última los tokens se transfieren
   * del vendedor al superusuario comprador.
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
   * Registra y persiste la transacción inmutable de auditoría para una venta oficial inmediata.
   *
   * <p>El superusuario compra y el usuario autenticado vende. El registro conserva la cotización de
   * referencia y la comisión aplicada, además del precio neto realmente pagado.
   *
   * @param buyer superusuario comprador
   * @param seller usuario vendedor
   * @param player jugador vendido
   * @param quantity cantidad de tokens transferida
   * @param quote cotización vigente aplicada
   * @param discountPercentage comisión aplicada (se conserva el nombre por compatibilidad)
   * @param unitPrice precio neto por token
   * @param totalAmount importe total en centésimas
   * @param buyerCreditsBefore créditos del comprador previos
   * @param buyerCreditsAfter créditos del comprador posteriores
   * @param sellerCreditsBefore créditos del vendedor previos
   * @param sellerCreditsAfter créditos del vendedor posteriores
   * @param buyerTokensBefore tokens del comprador previos
   * @param buyerTokensAfter tokens del comprador posteriores
   * @param sellerTokensBefore tokens del vendedor previos
   * @param sellerTokensAfter tokens del vendedor posteriores
   * @param confirmedAt instante de confirmación
   * @return transacción inmutable persistida
   */
  public MarketTransaction recordBuyback(
      User buyer,
      User seller,
      Player player,
      int quantity,
      CurrentQuote quote,
      int discountPercentage,
      long unitPrice,
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
        MarketTransaction.recordBuyback(
            buyer,
            seller,
            player,
            quantity,
            quote.price(),
            discountPercentage,
            unitPrice,
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
