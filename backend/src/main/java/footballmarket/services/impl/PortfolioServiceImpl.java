package footballmarket.services.impl;

import footballmarket.controllers.exceptions.InvalidPortfolioPageException;
import footballmarket.controllers.exceptions.InvalidPortfolioStatusException;
import footballmarket.models.FinancialPositionCalculator;
import footballmarket.models.Player;
import footballmarket.models.PlayerFinancialPosition;
import footballmarket.models.User;
import footballmarket.models.exceptions.PortfolioIntegrityException;
import footballmarket.models.records.PortfolioView;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.PlayerTokenRepository;
import footballmarket.repositories.TokenListingRepository;
import footballmarket.repositories.UserRepository;
import footballmarket.repositories.projections.HoldingQuantity;
import footballmarket.repositories.projections.ReservedQuantity;
import footballmarket.services.FinancialPositionService;
import footballmarket.services.PortfolioService;
import footballmarket.services.exceptions.UserNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de solo lectura del portfolio personal.
 *
 * <p>Las cifras contables proceden del componente contable de REQ-S2-20 y no se recalculan aquí:
 * esta feature consume las posiciones ya calculadas y su agregación sobre el conjunto completo. Las
 * cifras de mercado —cantidad poseída y cantidad reservada— se leen del estado que las origina, y
 * su cruce con la reconstrucción contable detecta cualquier divergencia antes de devolver una
 * respuesta.
 */
@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
  private static final String STATUS_OPEN = "OPEN";
  private static final String STATUS_CLOSED = "CLOSED";
  private static final String VALUATION_VALUED = "VALUED";
  private static final String VALUATION_UNVALUED = "UNVALUED";
  private static final String VALUATION_NOT_APPLICABLE = "NOT_APPLICABLE";

  /**
   * Posición de mercado: la posición contable calculada junto con las cantidades de mercado que la
   * sostienen.
   *
   * @param calculated posición financiera calculada por el componente contable
   * @param quantity tokens poseídos por el usuario
   * @param reservedQuantity tokens reservados por publicaciones activas y vigentes
   */
  private record MarketPosition(
      PlayerFinancialPosition calculated, int quantity, int reservedQuantity) {
    boolean open() {
      return this.quantity > 0;
    }
  }

  /**
   * Orden de presentación: abiertas valoradas por valor actual descendente, abiertas sin cotización
   * y cerradas por nombre de jugador. El identificador del jugador desempata cualquier igualdad.
   */
  private static final Comparator<PortfolioView.PortfolioPosition> POSITION_ORDER =
      Comparator.comparingInt(PortfolioServiceImpl::groupRank)
          .thenComparing(
              (left, right) -> {
                int group = groupRank(left);
                if (group == 0) {
                  return Long.compare(currentValue(right), currentValue(left));
                }
                if (group == 2) {
                  return Comparator.nullsFirst(String::compareTo)
                      .compare(left.playerName(), right.playerName());
                }
                return Long.compare(left.playerId(), right.playerId());
              })
          .thenComparing(PortfolioView.PortfolioPosition::playerId);

  private final FinancialPositionService financialPositions;
  private final UserRepository users;
  private final PlayerTokenRepository holdings;
  private final TokenListingRepository listings;
  private final PlayerRepository players;
  private final Clock marketClock;

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PortfolioView portfolio(String userEmail, int page, int size, String status) {
    if (page < 0 || size < 1 || size > 100) {
      throw new InvalidPortfolioPageException();
    }
    String filter = parseStatus(status);
    User user = this.resolveUser(userEmail);

    List<PlayerFinancialPosition> calculated = this.financialPositions.positionsOf(user.getId());
    FinancialPositionCalculator.Summary aggregate =
        FinancialPositionCalculator.aggregate(calculated);

    OffsetDateTime now = OffsetDateTime.now(this.marketClock);
    Map<Long, Long> reservedByPlayer = this.reservedByPlayer(user.getId(), now);
    Map<Long, Integer> holdingByPlayer = this.holdingByPlayer(user.getId());

    List<MarketPosition> market = new ArrayList<>();
    for (PlayerFinancialPosition position : calculated) {
      int quantity = holdingByPlayer.getOrDefault(position.playerId(), 0);
      if (quantity != position.openQuantity()) {
        throw new PortfolioIntegrityException();
      }
      market.add(
          new MarketPosition(
              position,
              quantity,
              reservedByPlayer.getOrDefault(position.playerId(), 0L).intValue()));
    }

    PortfolioView.PortfolioSummary summary = this.toSummary(market, aggregate);

    Map<Long, Player> playersById =
        this.players
            .findAllByIdInWithTeam(
                market.stream()
                    .map(position -> position.calculated().playerId())
                    .distinct()
                    .toList())
            .stream()
            .collect(Collectors.toMap(Player::getId, Function.identity()));

    List<PortfolioView.PortfolioPosition> positions =
        market.stream()
            .map(position -> this.toPosition(position, playersById, aggregate.currentValue()))
            .sorted(POSITION_ORDER)
            .toList();

    List<PortfolioView.PortfolioPosition> filtered =
        positions.stream()
            .filter(position -> filter == null || filter.equals(position.status()))
            .toList();

    int totalElements = filtered.size();
    int totalPages = (int) Math.ceil((double) totalElements / size);
    List<PortfolioView.PortfolioPosition> pageItems = this.page(filtered, page, size);

    return new PortfolioView(
        summary,
        pageItems,
        new PortfolioView.PortfolioPagination(page, size, totalElements, totalPages));
  }

  private String parseStatus(String status) {
    if (status == null) {
      return null;
    }
    if (STATUS_OPEN.equals(status) || STATUS_CLOSED.equals(status)) {
      return status;
    }
    throw new InvalidPortfolioStatusException();
  }

  private User resolveUser(String email) {
    return this.users
        .findByEmail(email.toLowerCase(Locale.ROOT))
        .orElseThrow(() -> new UserNotFoundException("El usuario de la sesión no existe."));
  }

  private Map<Long, Integer> holdingByPlayer(Long ownerId) {
    return this.holdings.findHoldingsByOwner(ownerId).stream()
        .collect(
            Collectors.toMap(
                HoldingQuantity::getPlayerId,
                HoldingQuantity::getQuantity,
                (first, second) -> first));
  }

  private Map<Long, Long> reservedByPlayer(Long ownerId, OffsetDateTime now) {
    return this.listings.reservedQuantitiesByPlayer(ownerId, now).stream()
        .collect(Collectors.toMap(ReservedQuantity::getPlayerId, ReservedQuantity::getQuantity));
  }

  private List<PortfolioView.PortfolioPosition> page(
      List<PortfolioView.PortfolioPosition> filtered, int page, int size) {
    long from = (long) page * size;
    if (from >= filtered.size()) {
      return List.of();
    }
    int to = (int) Math.min(from + size, filtered.size());
    return filtered.subList((int) from, to);
  }

  private PortfolioView.PortfolioSummary toSummary(
      List<MarketPosition> market, FinancialPositionCalculator.Summary aggregate) {
    int totalPositions = 0;
    int totalTokens = 0;
    int totalReservedTokens = 0;
    long totalBuyTransactions = 0;
    long totalSellTransactions = 0;
    for (MarketPosition position : market) {
      if (position.open()) {
        totalPositions++;
        totalTokens += position.quantity();
        totalReservedTokens += position.reservedQuantity();
      }
      totalBuyTransactions += position.calculated().buyOperationCount();
      totalSellTransactions += position.calculated().sellOperationCount();
    }
    return new PortfolioView.PortfolioSummary(
        totalPositions,
        totalTokens,
        totalReservedTokens,
        totalTokens - totalReservedTokens,
        aggregate.investedCost(),
        aggregate.currentValue(),
        aggregate.realizedProfitLoss(),
        aggregate.unrealizedProfitLoss(),
        aggregate.totalProfitLoss(),
        aggregate.profitLossPercentage(),
        totalBuyTransactions,
        totalSellTransactions,
        aggregate.valuationComplete());
  }

  private PortfolioView.PortfolioPosition toPosition(
      MarketPosition market, Map<Long, Player> playersById, Long totalCurrentValue) {
    PlayerFinancialPosition calculated = market.calculated();
    boolean open = market.open();
    Player player = playersById.get(calculated.playerId());

    Long currentUnitPrice = open ? calculated.currentUnitPrice() : null;
    OffsetDateTime priceUpdatedAt = open ? calculated.quoteCalculatedAt() : null;
    Long currentValue = open ? calculated.currentValue() : null;
    Long unrealizedProfitLoss = open ? calculated.unrealizedProfitLoss() : null;
    // El total pertenece al componente contable: indefinido sin cotización y realizado acumulado en
    // una posición cerrada. No se desempaqueta, para conservar esa nulidad en la respuesta.
    Long totalProfitLoss = calculated.totalProfitLoss();

    BigDecimal unrealizedPercentage =
        percentage(unrealizedProfitLoss, calculated.acquisitionCost());
    BigDecimal weightPercentage = percentage(currentValue, totalCurrentValue);

    return new PortfolioView.PortfolioPosition(
        calculated.playerId(),
        player == null ? null : player.getName(),
        player == null ? null : player.getTeam().getName(),
        player == null ? null : player.getPosition(),
        player == null ? null : player.getImageUrl(),
        player != null && player.isActive(),
        open ? STATUS_OPEN : STATUS_CLOSED,
        open
            ? (currentUnitPrice == null ? VALUATION_UNVALUED : VALUATION_VALUED)
            : VALUATION_NOT_APPLICABLE,
        market.quantity(),
        market.reservedQuantity(),
        market.quantity() - market.reservedQuantity(),
        calculated.acquisitionCost(),
        open ? calculated.averageUnitPrice() : null,
        currentUnitPrice,
        priceUpdatedAt,
        currentValue,
        calculated.realizedProfitLoss(),
        unrealizedProfitLoss,
        totalProfitLoss,
        unrealizedPercentage,
        weightPercentage,
        calculated.openedAt(),
        calculated.closedAt(),
        calculated.lastTransactionAt(),
        calculated.buyOperationCount(),
        calculated.sellOperationCount());
  }

  /**
   * Porcentaje de dos decimales sobre un denominador entero. Un valor indefinido o un denominador
   * cero producen un porcentaje indefinido, nunca un cero.
   */
  private static BigDecimal percentage(Long amount, Long denominator) {
    if (amount == null || denominator == null || denominator == 0L) {
      return null;
    }
    return BigDecimal.valueOf(amount)
        .multiply(ONE_HUNDRED)
        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
  }

  private static int groupRank(PortfolioView.PortfolioPosition position) {
    if (!STATUS_OPEN.equals(position.status())) {
      return 2;
    }
    return position.currentUnitPrice() == null ? 1 : 0;
  }

  private static long currentValue(PortfolioView.PortfolioPosition position) {
    return position.currentValue() == null ? 0L : position.currentValue();
  }
}
