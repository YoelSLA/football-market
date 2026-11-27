package footballmarket.features.market.application.internal;

import footballmarket.features.auth.application.exceptions.UserNotFoundException;
import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.domain.enums.UserRole;
import footballmarket.features.auth.infraestructure.UserRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.market.application.exceptions.CannotBuyOwnListingException;
import footballmarket.features.market.application.exceptions.InvalidOrderRequestException;
import footballmarket.features.market.application.exceptions.ListingNotAvailableException;
import footballmarket.features.market.application.exceptions.ListingNotFoundException;
import footballmarket.features.market.application.exceptions.SuperuserCannotBuyException;
import footballmarket.features.market.domain.enums.TokenListingStatus;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.models.PlayerToken;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.domain.models.UserCredit;
import footballmarket.features.market.domain.records.BuyListingRequest;
import footballmarket.features.market.domain.records.ListingIdentity;
import footballmarket.features.market.infraestructure.TokenListingRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ejecutor interno del caso de uso de liquidación de ofertas comunitarias en el Mercado P2P.
 *
 * <p>Orquesta la compra atómica de un lote completo publicado por otro usuario. Resuelve comprador,
 * garantiza que el comprador no sea el propio vendedor ni superusuario, adquiere bloqueos
 * deterministas sobre cuentas, publicación y tenencias, revalida la vigencia temporal contra el
 * reloj de mercado, realiza la transferencia y marca la publicación como vendida.
 */
@Component
@RequiredArgsConstructor
public class P2PBuyOrderExecutor {

  private final UserRepository users;
  private final PlayerRepository players;
  private final TokenListingRepository listings;
  private final MarketInitializationGuard marketInitialization;
  private final MarketSettlementExecutor settlement;
  private final Clock marketClock;

  /**
   * Liquida una publicación comunitaria P2P.
   *
   * @param buyerEmail correo del comprador autenticado
   * @param order solicitud de liquidación con identificador de publicación
   * @return registro inmutable de auditoría
   */
  public MarketTransaction execute(String buyerEmail, BuyListingRequest order) {
    User buyer = this.resolveBuyer(buyerEmail);
    if (buyer.getRole() == UserRole.SUPERUSER) {
      throw new SuperuserCannotBuyException();
    }
    if (order == null || order.listingId() == null) {
      throw new InvalidOrderRequestException();
    }

    // El mercado debe existir antes de operar: el rechazo ocurre antes de leer la publicación.
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

    // Bloqueo determinista de cuentas en orden ascendente por identificador.
    Map<Long, UserCredit> accounts =
        this.settlement.lockP2PAccounts(buyer.getId(), identity.sellerId());

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

    // Bloqueo determinista de tenencias en orden ascendente de propietario.
    Map<Long, PlayerToken> tokens =
        this.settlement.lockP2PHoldings(listing.getPlayer().getId(), buyer.getId(), seller.getId());

    // C1: el reloj se consulta solo después de adquirir ambas tenencias. Una espera puede
    // haber alcanzado expiresAt; nunca autorizar con un instante anterior a esa espera.
    OffsetDateTime validatedAt = OffsetDateTime.now(this.marketClock);
    if (!listing.isActiveAndValid(validatedAt)) {
      throw new ListingNotAvailableException();
    }

    this.settlement.revalidate(
        accounts,
        tokens,
        buyer.getId(),
        seller.getId(),
        listing.getTotalAmount(),
        listing.getQuantity());

    long buyerBefore = accounts.get(buyer.getId()).getAvailableCredits();
    long sellerBefore = accounts.get(seller.getId()).getAvailableCredits();
    int buyerTokensBefore = tokens.get(buyer.getId()).getQuantity();
    int sellerTokensBefore = tokens.get(seller.getId()).getQuantity();

    this.settlement.transfer(
        accounts,
        tokens,
        buyer.getId(),
        seller.getId(),
        listing.getTotalAmount(),
        listing.getQuantity());

    // Reutiliza el instante validado, aunque el reloj avance durante la liquidación.
    listing.markSold(validatedAt);

    return this.settlement.recordP2P(
        buyer,
        seller,
        listing,
        buyerBefore,
        accounts.get(buyer.getId()).getAvailableCredits(),
        sellerBefore,
        accounts.get(seller.getId()).getAvailableCredits(),
        buyerTokensBefore,
        tokens.get(buyer.getId()).getQuantity(),
        sellerTokensBefore,
        tokens.get(seller.getId()).getQuantity(),
        validatedAt);
  }

  private User resolveBuyer(String buyerEmail) {
    return this.users
        .findByEmail(buyerEmail.toLowerCase(Locale.ROOT))
        .orElseThrow(() -> new UserNotFoundException("El comprador de la sesión no existe."));
  }
}
