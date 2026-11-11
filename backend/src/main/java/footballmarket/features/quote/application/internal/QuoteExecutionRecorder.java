package footballmarket.features.quote.application.internal;

import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.quote.infraestructure.QuoteExecutionRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Confirma contexto y cierre de la fachada de recálculo en transacciones independientes. */
@Component
public class QuoteExecutionRecorder {
  private final QuoteExecutionRepository executions;

  public QuoteExecutionRecorder(QuoteExecutionRepository executions) {
    this.executions = executions;
  }

  /** Confirma la identidad antes de que una publicación la referencie por FK. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public QuoteExecution start(QuoteExecution context) {
    return this.executions.saveAndFlush(context);
  }

  /** Cierra una vez el contexto; no recupera ni compensa cierres interrumpidos. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public QuoteExecution complete(
      Long executionId,
      int population,
      int recomputed,
      int carried,
      int skipped,
      int failed,
      Instant finishedAt) {
    QuoteExecution execution = this.executions.findById(executionId).orElseThrow();
    execution.complete(population, recomputed, carried, skipped, failed, finishedAt);
    return this.executions.saveAndFlush(execution);
  }
}
