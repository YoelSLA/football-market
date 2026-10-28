package footballmarket.services.impl;

import footballmarket.integrations.WhoScoredStatisticsScrapingIntegration;
import footballmarket.integrations.records.WhoScoredScrapedPlayer;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.repositories.PlayerExternalReferenceRepository;
import footballmarket.services.PlayerStatisticsExtractionService;
import footballmarket.services.exceptions.PlayerStatisticsSynchronizationException;
import footballmarket.services.internal.*;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Caso de uso de extracción individual sin transacción durante el acceso externo. */
@Service
public class PlayerStatisticsExtractionServiceImpl implements PlayerStatisticsExtractionService {
  private final HeadedBrowserAvailabilityChecker browser;
  private final WhoScoredStatisticsScrapingIntegration integration;
  private final PlayerExternalReferenceRepository references;
  private final ScrapingMetricAssembler assembler;
  private final PlayerStatisticsWriter writer;
  private final ExtractionIncidentRecorder incidents;

  public PlayerStatisticsExtractionServiceImpl(
      HeadedBrowserAvailabilityChecker browser,
      WhoScoredStatisticsScrapingIntegration integration,
      PlayerExternalReferenceRepository references,
      ScrapingMetricAssembler assembler,
      PlayerStatisticsWriter writer,
      ExtractionIncidentRecorder incidents) {
    this.browser = browser;
    this.integration = integration;
    this.references = references;
    this.assembler = assembler;
    this.writer = writer;
    this.incidents = incidents;
  }

  @Override
  public Batch openBatch() {
    this.requireOutsideTransaction();
    WhoScoredStatisticsScrapingIntegration.Session session = this.browser.open();
    return new Batch() {
      private final java.util.concurrent.atomic.AtomicLong calls =
          new java.util.concurrent.atomic.AtomicLong();

      @Override
      public Long extract(Long runId, Long playerId) {
        return PlayerStatisticsExtractionServiceImpl.this.extract(
            session, runId, playerId, this.calls);
      }

      @Override
      public long providerCalls() {
        return this.calls.get();
      }

      @Override
      public void releaseWorker() {
        session.releaseWorker();
      }

      @Override
      public void close() {
        session.close();
      }
    };
  }

  private Long extract(
      WhoScoredStatisticsScrapingIntegration.Session session,
      Long runId,
      Long playerId,
      java.util.concurrent.atomic.AtomicLong calls) {
    this.requireOutsideTransaction();
    String externalId =
        this.references
            .findByPlayerIdAndProvider(playerId, ExternalProvider.WHOSCORED)
            .orElseThrow(PlayerStatisticsSynchronizationException::new)
            .getExternalId();
    Instant start = Instant.now();
    try {
      WhoScoredScrapedPlayer scraped =
          this.integration.scrape(session, runId, playerId, externalId);
      calls.addAndGet(scraped.providerCalls());
      java.util.List<PlayerMetricsContext> values =
          this.assembler.assemble(scraped, runId, playerId);
      Long itemId = this.writer.confirm(runId, playerId, externalId, start, Instant.now(), values);
      if (this.assembler.hasUnknownColumns(scraped)) {
        this.incidents.record(itemId, null, "PROVIDER_STRUCTURE_CHANGED", null, 1, Instant.now());
      }
      return itemId;
    } catch (footballmarket.integrations.exceptions.WhoScoredStatisticsException failure) {
      calls.addAndGet(failure.getProviderCalls());
      footballmarket.models.enums.PlayerExtractionResult result =
          switch (failure.getCode()) {
            case "PROVIDER_BLOCKED" -> footballmarket.models.enums.PlayerExtractionResult.BLOCKED;
            case "PLAYER_NOT_FOUND" -> footballmarket.models.enums.PlayerExtractionResult.NOT_FOUND;
            case "PROVIDER_STRUCTURE_CHANGED" ->
                footballmarket.models.enums.PlayerExtractionResult.STRUCTURE_CHANGED;
            case "EXTRACTION_INTERRUPTED" ->
                footballmarket.models.enums.PlayerExtractionResult.INTERRUPTED;
            default -> footballmarket.models.enums.PlayerExtractionResult.PROVIDER_ERROR;
          };
      Long itemId =
          this.writer.recordFailure(
              runId, playerId, externalId, start, Instant.now(), result, failure.getCode());
      this.incidents.record(
          itemId,
          failure.getSection(),
          failure.getCode(),
          failure.getProviderStatus(),
          1,
          Instant.now());
      throw failure;
    }
  }

  private void requireOutsideTransaction() {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new PlayerStatisticsSynchronizationException();
    }
  }
}
