package footballmarket.features.portfolio.application.impl;

import footballmarket.features.auth.application.exceptions.CurrentUserNotFoundException;
import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.infraestructure.UserRepository;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.infraestructure.MarketTransactionRepository;
import footballmarket.features.portfolio.application.OperationHistoryService;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry.OperationType;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación transaccional de la consulta del historial personal. */
@Service
@RequiredArgsConstructor
public class OperationHistoryServiceImpl implements OperationHistoryService {

  private final UserRepository users;
  private final MarketTransactionRepository transactions;

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public List<OperationHistoryEntry> history(String subjectEmail) {
    User user = this.resolveUser(subjectEmail);
    return this.transactions.findOperationHistory(user.getId()).stream()
        .map(transaction -> this.toEntry(transaction, user.getId()))
        .toList();
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
    OperationType type =
        transaction.getBuyer().getId().equals(userId) ? OperationType.BUY : OperationType.SELL;
    return new OperationHistoryEntry(
        transaction.getId(),
        type,
        transaction.getPlayer().getId(),
        transaction.getPlayer().getName(),
        transaction.getQuantity(),
        transaction.getUnitPrice(),
        transaction.getTotalAmount(),
        transaction.getExecutedAt());
  }
}
