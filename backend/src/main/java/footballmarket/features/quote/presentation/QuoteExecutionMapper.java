package footballmarket.features.quote.presentation;

import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.quote.presentation.dtos.response.QuoteExecutionResponseDTO;
import footballmarket.features.quote.presentation.dtos.response.QuoteExecutionsResponseDTO;
import java.util.List;

/** Transformaciones HTTP del resumen persistido de una ejecución de cotizaciones. */
public final class QuoteExecutionMapper {
  private QuoteExecutionMapper() {}

  public static QuoteExecutionResponseDTO toResponse(QuoteExecution execution) {
    return new QuoteExecutionResponseDTO(
        execution.getQuoteDate(),
        execution.getStrategyName(),
        execution.getStrategyVersion(),
        execution.getAdjustmentFactor(),
        execution.getVariationCap(),
        execution.getMinPriceFloor(),
        execution.getPricedPlayers(),
        execution.getRecomputedRows(),
        execution.getCarriedForwardRows(),
        execution.getNotRepricedPlayers(),
        execution.getFailedPlayers(),
        execution.getStartedAt(),
        execution.getFinishedAt());
  }

  public static QuoteExecutionsResponseDTO toResponse(List<QuoteExecution> executions) {
    return new QuoteExecutionsResponseDTO(
        executions.stream().map(QuoteExecutionMapper::toResponse).toList());
  }
}
