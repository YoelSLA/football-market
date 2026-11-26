package footballmarket.features.businessmetrics.application.impl;

import footballmarket.features.businessmetrics.application.BusinessMetricListingCancelledEvent;
import footballmarket.features.businessmetrics.application.BusinessMetricTransactionEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Registra métricas de negocio solo después de confirmar la transacción que las produjo. */
@Component
public class BusinessMetricsRecorder {
  private final MeterRegistry meterRegistry;

  public BusinessMetricsRecorder(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /** Añade los volúmenes y el evento de una operación financiera confirmada. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void recordTransaction(BusinessMetricTransactionEvent event) {
    if (event.kind() == BusinessMetricTransactionEvent.TransactionKind.PURCHASE) {
      this.meterRegistry.counter(BusinessMetricsServiceImpl.PURCHASES).increment();
    } else {
      this.meterRegistry.counter(BusinessMetricsServiceImpl.SALES).increment();
    }
    this.meterRegistry
        .counter(BusinessMetricsServiceImpl.TOKENS_TRANSFERRED)
        .increment(event.tokensTransferred());
    this.meterRegistry
        .counter(BusinessMetricsServiceImpl.CREDITS_TRADED_IN_CENTS)
        .increment(event.creditsTradedInCents());
  }

  /** Añade una publicación cancelada confirmada. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void recordCancelledListing(BusinessMetricListingCancelledEvent event) {
    this.meterRegistry.counter(BusinessMetricsServiceImpl.CANCELLED_LISTINGS).increment();
  }
}
