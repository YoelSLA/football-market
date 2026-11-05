package footballmarket.features.quote.application;

import footballmarket.features.quote.domain.models.QuoteExecution;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Casos de uso de recálculo manual y automático y consulta de sus resúmenes persistidos. */
public interface QuoteRecalculationService {
  QuoteExecution recalculate(
      String strategy, BigDecimal adjustmentFactor, BigDecimal variationCap, Long minPriceFloor);

  /** Ejecuta el mismo recálculo con la estrategia y los parámetros configurados del mercado. */
  QuoteExecution recalculateAutomatic();

  List<QuoteExecution> executions(LocalDate date);
}
