package footballmarket.repositories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.market.application.BuyOrderService;
import footballmarket.features.market.application.TokenListingService;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.market.domain.records.SellOrderRequest;
import footballmarket.features.market.infraestructure.MarketTransactionRepository;
import footballmarket.features.market.infraestructure.PlayerTokenRepository;
import footballmarket.features.market.infraestructure.TokenListingRepository;
import footballmarket.features.market.infraestructure.UserCreditRepository;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.domain.records.CurrentQuote;
import footballmarket.support.MarketTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Preparación compartida exclusiva de la categoría Repository; no contiene assertions. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
abstract class ListingRepositorySupport {
  protected static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-17T12:00:00Z");
  @Autowired protected TokenListingService listingService;
  @Autowired protected BuyOrderService buyService;
  @Autowired protected MarketTestData market;
  @Autowired protected TokenListingRepository listings;
  @Autowired protected PlayerTokenRepository holdings;
  @Autowired protected UserCreditRepository credits;
  @Autowired protected MarketTransactionRepository transactions;
  @Autowired protected PlayerRepository players;
  @Autowired protected PlatformTransactionManager manager;
  @Autowired protected JdbcTemplate jdbc;
  @MockitoBean protected QuoteSource quotes;
  @MockitoBean protected Clock marketClock;
  protected User seller;
  protected User buyer;
  protected Player player;

  @BeforeEach
  void prepareListingMarket() {
    when(marketClock.getZone()).thenReturn(ZoneOffset.UTC);
    when(marketClock.instant()).thenReturn(NOW.toInstant());
    when(quotes.currentQuote(any()))
        .thenAnswer(
            invocation -> {
              Long playerId = invocation.getArgument(0);
              return Optional.of(new CurrentQuote(playerId, 100L, NOW, "fixture-v1"));
            });
    seller = market.registerBuyer();
    buyer = market.registerBuyer();
    User superuser = market.registerSuperuser();
    player = market.createVisiblePlayer();
    market.emitInitialTokens(player, superuser);
    market.initializeMarket(player);
    buyService.buy(seller.getEmail(), new BuyOrderRequest(player.getId(), 20));
  }

  protected TokenListing publish(int quantity) {
    return listingService.publish(
        seller.getEmail(), new SellOrderRequest(player.getId(), quantity));
  }

  protected long reserved() {
    return listings.reservedQuantity(seller.getId(), player.getId(), NOW);
  }

  protected long balance(User user) {
    return credits.findById(user.getId()).orElseThrow().getAvailableCredits();
  }

  protected int quantity(User user) {
    return holdings
        .findByPlayerIdAndOwnerId(player.getId(), user.getId())
        .map(token -> token.getQuantity())
        .orElse(0);
  }

  protected TransactionTemplate transaction() {
    return new TransactionTemplate(manager);
  }
}
