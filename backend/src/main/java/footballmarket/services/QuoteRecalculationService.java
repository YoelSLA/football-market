package footballmarket.services;

import footballmarket.models.QuoteExecution;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Caso de uso de recálculo manual y consulta de sus resúmenes persistidos. */
public interface QuoteRecalculationService {
  QuoteExecution recalculate(
      String strategy, BigDecimal adjustmentFactor, BigDecimal variationCap, Long minPriceFloor);

  List<QuoteExecution> executions(LocalDate date);
}
