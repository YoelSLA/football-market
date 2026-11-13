package footballmarket.features.portfolio.application.impl;

import footballmarket.features.auth.application.exceptions.CurrentUserNotFoundException;
import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.infraestructure.UserRepository;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.infraestructure.MarketTransactionRepository;
import footballmarket.features.portfolio.application.OperationHistoryService;
import footballmarket.features.portfolio.domain.records.OperationHistoryAggregate;
import footballmarket.features.portfolio.domain.records.OperationHistoryCriteria;
import footballmarket.features.portfolio.domain.records.OperationHistoryCriteria.Sort;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry.OperationType;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry.PlayerData;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry.RelatedData;
import footballmarket.features.portfolio.domain.records.OperationHistoryView;
import footballmarket.features.portfolio.presentation.exceptions.InvalidOperationHistoryCriteriaException;
import footballmarket.features.portfolio.presentation.exceptions.InvalidOperationHistoryDateRangeException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación transaccional de la consulta paginada del historial personal. */
@Service
@RequiredArgsConstructor
public class OperationHistoryServiceImpl implements OperationHistoryService {

  private static final int MAX_PAGE_SIZE = 100;
  private static final OffsetDateTime UNUSED_DATE_FILTER =
      OffsetDateTime.parse("1970-01-01T00:00:00Z");

  private final UserRepository users;
  private final MarketTransactionRepository transactions;

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public OperationHistoryView history(
      String subjectEmail,
      int page,
      int size,
      String search,
      String type,
      OffsetDateTime executedFrom,
      OffsetDateTime executedBefore,
      String sort) {
    User user = this.resolveUser(subjectEmail);
    OperationHistoryCriteria criteria =
        this.criteria(page, size, search, type, executedFrom, executedBefore, sort);
    String normalizedType = criteria.type() == null ? null : criteria.type().name();
    Page<MarketTransaction> result =
        this.transactions.findOperationHistory(
            user.getId(),
            criteria.search(),
            normalizedType,
            criteria.executedFrom() != null,
            this.boundDate(criteria.executedFrom()),
            criteria.executedBefore() != null,
            this.boundDate(criteria.executedBefore()),
            this.pageRequest(criteria));
    OperationHistoryAggregate aggregate =
        this.transactions.aggregateOperationHistory(
            user.getId(),
            criteria.search(),
            normalizedType,
            criteria.executedFrom() != null,
            this.boundDate(criteria.executedFrom()),
            criteria.executedBefore() != null,
            this.boundDate(criteria.executedBefore()));

    List<OperationHistoryEntry> entries =
        result.stream().map(transaction -> this.toEntry(transaction, user.getId())).toList();
    return new OperationHistoryView(
        this.toSummary(aggregate),
        entries,
        new OperationHistoryView.Pagination(
            criteria.page(), criteria.size(), result.getTotalElements(), result.getTotalPages()));
  }

  private OperationHistoryCriteria criteria(
      int page,
      int size,
      String search,
      String type,
      OffsetDateTime executedFrom,
      OffsetDateTime executedBefore,
      String sort) {
    if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
      throw new InvalidOperationHistoryCriteriaException();
    }
    if (executedFrom != null && executedBefore != null && !executedFrom.isBefore(executedBefore)) {
      throw new InvalidOperationHistoryDateRangeException();
    }
    try {
      OperationType parsedType =
          type == null || type.isBlank() ? null : OperationType.valueOf(type);
      Sort parsedSort = sort == null || sort.isBlank() ? Sort.NEWEST : Sort.valueOf(sort);
      String parsedSearch = search == null || search.isBlank() ? null : search.strip();
      return new OperationHistoryCriteria(
          page, size, parsedSearch, parsedType, executedFrom, executedBefore, parsedSort);
    } catch (IllegalArgumentException exception) {
      throw new InvalidOperationHistoryCriteriaException();
    }
  }

  private PageRequest pageRequest(OperationHistoryCriteria criteria) {
    return switch (criteria.sort()) {
      case NEWEST ->
          PageRequest.of(
              criteria.page(),
              criteria.size(),
              org.springframework.data.domain.Sort.by(Direction.DESC, "executedAt", "id"));
      case OLDEST ->
          PageRequest.of(
              criteria.page(),
              criteria.size(),
              org.springframework.data.domain.Sort.by(Direction.ASC, "executedAt", "id"));
      case AMOUNT_DESC ->
          PageRequest.of(
              criteria.page(),
              criteria.size(),
              org.springframework.data.domain.Sort.by(Direction.DESC, "totalAmount")
                  .and(
                      org.springframework.data.domain.Sort.by(Direction.DESC, "executedAt", "id")));
      case AMOUNT_ASC ->
          PageRequest.of(
              criteria.page(),
              criteria.size(),
              org.springframework.data.domain.Sort.by(Direction.ASC, "totalAmount")
                  .and(
                      org.springframework.data.domain.Sort.by(Direction.DESC, "executedAt", "id")));
    };
  }

  private OffsetDateTime boundDate(OffsetDateTime date) {
    return date == null ? UNUSED_DATE_FILTER : date;
  }

  private User resolveUser(String subjectEmail) {
    if (subjectEmail == null || subjectEmail.isBlank()) {
      throw new CurrentUserNotFoundException();
    }
    return this.users
        .findByEmail(subjectEmail.toLowerCase(Locale.ROOT))
        .orElseThrow(CurrentUserNotFoundException::new);
  }

  private OperationHistoryEntry toEntry(MarketTransaction transaction, Long userId) {
    OperationType operationType =
        transaction.getBuyer().getId().equals(userId) ? OperationType.BUY : OperationType.SELL;
    Player player = transaction.getPlayer();
    Team team = player.getTeam();
    League league = team.getLeague();
    return new OperationHistoryEntry(
        transaction.getId(),
        operationType,
        new PlayerData(
            player.getId(),
            player.getName(),
            player.getImageUrl(),
            player.getFallbackImageUrl(),
            new RelatedData(team.getId(), team.getName()),
            new RelatedData(league.getId(), league.getName())),
        transaction.getQuantity(),
        transaction.getUnitPrice(),
        transaction.getTotalAmount(),
        transaction.getExecutedAt());
  }

  private OperationHistoryView.Summary toSummary(OperationHistoryAggregate aggregate) {
    return new OperationHistoryView.Summary(
        aggregate.totalTransactions(),
        aggregate.buyTransactions(),
        aggregate.sellTransactions(),
        aggregate.boughtTokens(),
        aggregate.soldTokens(),
        aggregate.purchasedVolume(),
        aggregate.soldVolume());
  }
}
