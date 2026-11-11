package footballmarket.features.quote.infraestructure.scheduling;

import footballmarket.features.market.application.exceptions.MarketNotInitializedException;
import footballmarket.features.quote.application.QuoteRecalculationService;
import footballmarket.features.quote.application.exceptions.ExecutionAlreadyExistsException;
import footballmarket.features.quote.infraestructure.config.QuoteProperties;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara el recálculo existente; no administra sus transacciones ni compensa cotizaciones. */
@Component
public class AutomaticQuoteRecalculationScheduler {
  private static final Logger LOG =
      LoggerFactory.getLogger(AutomaticQuoteRecalculationScheduler.class);
  private final QuoteRecalculationService service;
  private final QuoteProperties properties;

  public AutomaticQuoteRecalculationScheduler(
      QuoteRecalculationService service, QuoteProperties properties) {
    this.service = service;
    this.properties = properties;
  }

  /** Invoca el caso de uso si está habilitado y registra rechazos o fallos sin propagarlos. */
  @Scheduled(cron = "${market.quote.schedule.cron}", zone = "UTC")
  public void recalculate() {
    if (!this.properties.schedule().enabled()) {
      return;
    }
    LocalDate quoteDate = LocalDate.now(ZoneOffset.UTC);
    LOG.info("quote_automatic_recalculation_triggered quoteDate={}", quoteDate);
    try {
      this.service.recalculateAutomatic();
    } catch (ExecutionAlreadyExistsException exception) {
      LOG.warn(
          "quote_automatic_recalculation_skipped quoteDate={} reason=EXECUTION_ALREADY_EXISTS",
          quoteDate);
    } catch (MarketNotInitializedException exception) {
      LOG.warn(
          "quote_automatic_recalculation_rejected quoteDate={} reason=MARKET_NOT_INITIALIZED",
          quoteDate);
    } catch (RuntimeException exception) {
      LOG.error(
          "quote_automatic_recalculation_failed quoteDate={} cause={}",
          quoteDate,
          exception.getClass().getSimpleName());
    }
  }
}
