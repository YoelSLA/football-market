package footballmarket.features.market.application.impl;

import footballmarket.features.auth.application.exceptions.UserNotFoundException;
import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.domain.enums.UserRole;
import footballmarket.features.auth.infraestructure.UserRepository;
import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.catalog.application.exceptions.PlayerNotVisibleException;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.market.application.TokenListingService;
import footballmarket.features.market.application.exceptions.*;
import footballmarket.features.market.application.internal.MarketInitializationGuard;
import footballmarket.features.market.domain.enums.TokenListingStatus;
import footballmarket.features.market.domain.models.PlayerToken;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.domain.records.SellOrderRequest;
import footballmarket.features.market.domain.records.TokenListingView;
import footballmarket.features.market.domain.records.UserOrdersPage;
import footballmarket.features.market.domain.records.UserOrdersSummary;
import footballmarket.features.market.infraestructure.PlayerTokenRepository;
import footballmarket.features.market.infraestructure.TokenListingRepository;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.application.exceptions.InvalidQuotePriceException;
import footballmarket.features.quote.application.exceptions.QuoteContractViolationException;
import footballmarket.features.quote.application.exceptions.QuoteUnavailableException;
import footballmarket.features.quote.domain.records.CurrentQuote;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
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
  private final PlayerRepository playerRepository;
  private final PlayerTokenRepository holdings;
  private final TokenListingRepository listings;
  private final QuoteSource quoteSource;
  private final MarketInitializationGuard marketInitialization;
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
        this.playerRepository
            .findById(order.playerId())
            .orElseThrow(() -> new PlayerNotFoundException("No encontramos el jugador solicitado"));
    if (this.playerRepository.findVisibleById(player.getId()).isEmpty()) {
      throw new PlayerNotVisibleException();
    }
    // El mercado debe existir antes de operar: el rechazo ocurre antes de leer la cotización y
    // antes de cualquier movimiento de recursos.
    this.marketInitialization.requireInitialized();
    CurrentQuote quote =
        this.quoteSource.currentQuote(player.getId()).orElseThrow(QuoteUnavailableException::new);
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
              seller,
              player,
              order.quantity(),
              order.unitPrice(),
              quote,
              now,
              now.plus(this.expirationDuration)));
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
    return this.mine(viewerEmail, status, 0, 100, "NEWEST").content();
  }

  @Override
  @Transactional(readOnly = true)
  public UserOrdersPage mine(String viewerEmail, String status, int page, int size, String sort) {
    if (page < 0 || size < 1 || size > 100) {
      throw new InvalidListingStatusException();
    }
    User viewer = this.resolveUser(viewerEmail);
    TokenListingStatus filter = null;
    if (status != null) {
      filter =
          java.util.Arrays.stream(TokenListingStatus.values())
              .filter(candidate -> candidate.name().equals(status))
              .findFirst()
              .orElseThrow(InvalidListingStatusException::new);
    }

    String effectiveSort = (sort == null || sort.isBlank()) ? "NEWEST" : sort;
    Comparator<TokenListingView> comparator;
    switch (effectiveSort) {
      case "NEWEST" ->
          comparator =
              Comparator.comparing(TokenListingView::createdAt)
                  .reversed()
                  .thenComparing(Comparator.comparing(TokenListingView::listingId).reversed());
      case "OLDEST" ->
          comparator =
              Comparator.comparing(TokenListingView::createdAt)
                  .thenComparing(TokenListingView::listingId);
      case "PRICE_DESC" ->
          comparator =
              Comparator.comparing(TokenListingView::unitPrice)
                  .reversed()
                  .thenComparing(Comparator.comparing(TokenListingView::createdAt).reversed());
      case "PRICE_ASC" ->
          comparator =
              Comparator.comparing(TokenListingView::unitPrice)
                  .thenComparing(Comparator.comparing(TokenListingView::createdAt).reversed());
      default -> throw new InvalidListingStatusException();
    }

    OffsetDateTime now = OffsetDateTime.now(this.marketClock);
    List<TokenListingView> allUserListings =
        this.listings.findBySellerId(viewer.getId()).stream()
            .map(listing -> listing.snapshot(now))
            .toList();

    int activeOrders = 0;
    int activeTokens = 0;
    long activeValue = 0;
    int soldOrders = 0;
    int soldTokens = 0;
    long soldValue = 0;
    int cancelledOrders = 0;
    int expiredOrders = 0;

    for (TokenListingView view : allUserListings) {
      switch (view.status()) {
        case ACTIVE -> {
          activeOrders++;
          activeTokens += view.quantity();
          activeValue += view.totalAmount();
        }
        case SOLD -> {
          soldOrders++;
          soldTokens += view.quantity();
          soldValue += view.totalAmount();
        }
        case CANCELLED -> cancelledOrders++;
        case EXPIRED -> expiredOrders++;
      }
    }

    UserOrdersSummary summary =
        new UserOrdersSummary(
            activeOrders,
            activeTokens,
            activeValue,
            soldOrders,
            soldTokens,
            soldValue,
            cancelledOrders,
            expiredOrders,
            allUserListings.size());

    TokenListingStatus selected = filter;
    List<TokenListingView> filtered =
        allUserListings.stream()
            .filter(listing -> selected == null || listing.status() == selected)
            .sorted(comparator)
            .toList();

    long totalElements = filtered.size();
    int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    int fromIndex = Math.min(page * size, filtered.size());
    int toIndex = Math.min(fromIndex + size, filtered.size());
    List<TokenListingView> pageContent = filtered.subList(fromIndex, toIndex);

    return new UserOrdersPage(summary, pageContent, page, size, totalElements, totalPages);
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
            || this.playerRepository.findVisibleById(listing.getPlayer().getId()).isEmpty())) {
      throw new ListingNotFoundException();
    }
    return listing.snapshot(now);
  }
}
