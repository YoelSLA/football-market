package footballmarket.services.impl;

import footballmarket.exceptions.IntegrationException;
import footballmarket.integrations.QuoteSource;
import footballmarket.integrations.exceptions.QuoteBatchUnavailableException;
import footballmarket.models.FinancialPositionCalculator;
import footballmarket.models.PlayerFinancialPosition;
import footballmarket.models.exceptions.FinancialPositionIntegrityException;
import footballmarket.models.records.CurrentQuote;
import footballmarket.repositories.MarketTransactionRepository;
import footballmarket.repositories.PlayerTokenEmissionRepository;
import footballmarket.services.FinancialPositionService;
import footballmarket.services.exceptions.QuoteContractViolationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Reconstrucción consistente del historial con adquisición masiva sin fallback individual. */
@Service
public class FinancialPositionServiceImpl implements FinancialPositionService {
  private final MarketTransactionRepository transactions;
  private final PlayerTokenEmissionRepository emissions;
  private final QuoteSource quotes;

  public FinancialPositionServiceImpl(
      MarketTransactionRepository transactions,
      PlayerTokenEmissionRepository emissions,
      QuoteSource quotes) {
    this.transactions = transactions;
    this.emissions = emissions;
    this.quotes = quotes;
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public List<PlayerFinancialPosition> positionsOf(Long userId) {
    if (userId == null) {
      throw new FinancialPositionIntegrityException();
    }
    List<PlayerFinancialPosition> positions =
        FinancialPositionCalculator.reconstruct(
            userId,
            this.transactions.findFinancialHistory(userId),
            this.emissions.findFinancialEmissions(userId));
    Set<Long> players =
        positions.stream()
            .filter(PlayerFinancialPosition::isOpen)
            .map(PlayerFinancialPosition::playerId)
            .collect(Collectors.toUnmodifiableSet());
    if (players.isEmpty()) {
      return positions;
    }
    Map<Long, CurrentQuote> batch;
    try {
      batch = this.quotes.currentQuotes(players);
    } catch (RuntimeException failure) {
      if (failure instanceof IntegrationException) {
        // Un fallo de integración conserva su tipo y su causa: el detalle técnico no sale del
        // servidor, porque el manejador global responde con un error genérico.
        throw failure;
      }
      // No filtrar mensajes del proveedor ni convertir errores en cotizaciones ausentes.
      throw new QuoteBatchUnavailableException(failure);
    }
    if (batch == null) {
      throw new QuoteContractViolationException();
    }
    for (Map.Entry<Long, CurrentQuote> entry : batch.entrySet()) {
      CurrentQuote quote = entry.getValue();
      if (!players.contains(entry.getKey())
          || quote == null
          || !entry.getKey().equals(quote.playerId())
          || quote.price() <= 0
          || quote.calculatedAt() == null) {
        throw new QuoteContractViolationException();
      }
    }
    return positions.stream()
        .map(
            position ->
                position.isOpen() ? position.valuedAt(batch.get(position.playerId())) : position)
        .toList();
  }
}
