package footballmarket.services.impl;

import footballmarket.integrations.QuoteSource;
import footballmarket.models.Player;
import footballmarket.models.PlayerToken;
import footballmarket.models.TokenListing;
import footballmarket.models.User;
import footballmarket.models.enums.TokenListingStatus;
import footballmarket.models.enums.UserRole;
import footballmarket.models.records.CurrentQuote;
import footballmarket.models.records.SellOrderRequest;
import footballmarket.models.records.TokenListingView;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.PlayerTokenRepository;
import footballmarket.repositories.TokenListingRepository;
import footballmarket.repositories.UserRepository;
import footballmarket.services.TokenListingService;
import footballmarket.services.exceptions.*;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachada de publicaciones; cada escritura comparte una única transacción y no modifica créditos.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TokenListingServiceImpl implements TokenListingService {
  private final UserRepository users;
  private final PlayerRepository players;
  private final PlayerTokenRepository holdings;
  private final TokenListingRepository listings;
  private final QuoteSource quotes;
  private final Clock marketClock;

  @Value("${market.listing.expiration-duration:PT24H}")
  private Duration expirationDuration;

  @Override
  public TokenListing publish(String sellerEmail, SellOrderRequest order) {
    User seller = this.resolveUser(sellerEmail);
    if (seller.getRole() == UserRole.SUPERUSER) {
      throw new SuperuserCannotSellException();
    }
    if (order.quantity() < 1 || order.quantity() > 100) {
      throw new InvalidQuantityException();
    }
    Player player =
        this.players
            .findById(order.playerId())
            .orElseThrow(() -> new PlayerNotFoundException("No encontramos el jugador solicitado"));
    if (this.players.findVisibleById(player.getId()).isEmpty()) {
      throw new PlayerNotVisibleException();
    }
    CurrentQuote quote =
        this.quotes.currentQuote(player.getId()).orElseThrow(QuoteUnavailableException::new);
    if (!player.getId().equals(quote.playerId())) {
      throw new QuoteContractViolationException();
    }
    if (quote.price() <= 0) {
      throw new InvalidQuotePriceException();
    }
    List<PlayerToken> locked =
        this.holdings.lockHoldingsInAscendingOwnerOrder(player.getId(), List.of(seller.getId()));
    if (locked.isEmpty()) {
      throw new InsufficientStockException();
    }
    OffsetDateTime now = OffsetDateTime.now(this.marketClock);
    long reserved = this.listings.reservedQuantity(seller.getId(), player.getId(), now);
    if ((long) locked.getFirst().getQuantity() - reserved < order.quantity()) {
      throw new InsufficientStockException();
    }
    try {
      return this.listings.save(
          TokenListing.publish(
              seller, player, order.quantity(), quote, now, now.plus(this.expirationDuration)));
    } catch (ArithmeticException overflow) {
      throw new ArithmeticOverflowException();
    }
  }

  private User resolveUser(String email) {
    return this.users
        .findByEmail(email.toLowerCase(Locale.ROOT))
        .orElseThrow(() -> new UserNotFoundException("El usuario de la sesión no existe."));
  }

  @Override
  public TokenListingView cancel(String ownerEmail, Long listingId) {
    User owner = this.resolveUser(ownerEmail);
    TokenListing listing =
        this.listings.lockById(listingId).orElseThrow(ListingNotFoundException::new);
    if (!listing.getSeller().getId().equals(owner.getId())) {
      throw new org.springframework.security.access.AccessDeniedException(
          "No podés cancelar una publicación ajena.");
    }
    OffsetDateTime now = OffsetDateTime.now(this.marketClock);
    if (!listing.isActiveAndValid(now)) {
      throw new ListingNotAvailableException();
    }
    listing.markCancelled(now);
    return listing.snapshot(now);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TokenListingView> market(String viewerEmail) {
    User viewer = this.resolveUser(viewerEmail);
    OffsetDateTime now = OffsetDateTime.now(this.marketClock);
    return this.listings.findMarket(viewer.getId(), now).stream()
        .map(listing -> listing.snapshot(now))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<TokenListingView> mine(String viewerEmail, String status) {
    User viewer = this.resolveUser(viewerEmail);
    TokenListingStatus filter = null;
    if (status != null) {
      filter =
          java.util.Arrays.stream(TokenListingStatus.values())
              .filter(candidate -> candidate.name().equals(status))
              .findFirst()
              .orElseThrow(InvalidListingStatusException::new);
    }
    OffsetDateTime now = OffsetDateTime.now(this.marketClock);
    TokenListingStatus selected = filter;
    return this.listings.findBySellerId(viewer.getId()).stream()
        .map(listing -> listing.snapshot(now))
        .filter(listing -> selected == null || listing.status() == selected)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public TokenListingView detail(String viewerEmail, Long listingId) {
    User viewer = this.resolveUser(viewerEmail);
    TokenListing listing =
        this.listings.findById(listingId).orElseThrow(ListingNotFoundException::new);
    OffsetDateTime now = OffsetDateTime.now(this.marketClock);
    if (!listing.getSeller().getId().equals(viewer.getId())
        && (!listing.isActiveAndValid(now)
            || this.players.findVisibleById(listing.getPlayer().getId()).isEmpty())) {
      throw new ListingNotFoundException();
    }
    return listing.snapshot(now);
  }
}
