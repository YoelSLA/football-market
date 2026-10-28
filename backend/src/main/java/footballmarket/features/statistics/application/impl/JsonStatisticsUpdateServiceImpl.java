package footballmarket.features.statistics.application.impl;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository;
import footballmarket.features.statistics.application.JsonStatisticsUpdateService;
import footballmarket.features.statistics.application.exceptions.PlayerStatisticsSynchronizationException;
import footballmarket.features.statistics.application.internal.ExtractionIncidentRecorder;
import footballmarket.features.statistics.application.internal.JsonMetricAssembler;
import footballmarket.features.statistics.application.internal.PlayerMetricsContext;
import footballmarket.features.statistics.application.internal.PlayerStatisticsWriter;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.features.statistics.domain.models.MetricCatalog;
import footballmarket.integrations.whoscored.WhoScoredRequestPacer;
import footballmarket.integrations.whoscored.WhoScoredStatisticsIntegration;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.whoscored.records.WhoScoredStatisticsResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Actualización JSON de un jugador confirmado por la fachada del canal.
 *
 * <p>El acceso externo ocurre sin transacción abierta y la confirmación es atómica. El canal no
 * consulta observaciones de ningún otro canal ni las contrasta: confirmar un jugador no depende de
 * qué haya confirmado el resto de canales. Una métrica ausente se persiste como ausencia y no borra
 * el histórico.
 */
@Service
public class JsonStatisticsUpdateServiceImpl implements JsonStatisticsUpdateService {
  private final WhoScoredStatisticsIntegration integration;
  private final PlayerExternalReferenceRepository references;
  private final JsonMetricAssembler assembler;
  private final PlayerStatisticsWriter writer;
  private final ExtractionIncidentRecorder incidents;

  public JsonStatisticsUpdateServiceImpl(
      WhoScoredStatisticsIntegration integration,
      PlayerExternalReferenceRepository references,
      JsonMetricAssembler assembler,
      PlayerStatisticsWriter writer,
      ExtractionIncidentRecorder incidents) {
    this.integration = integration;
    this.references = references;
    this.assembler = assembler;
    this.writer = writer;
    this.incidents = incidents;
  }

  @Override
  public Outcome update(Long runId, Long playerId, WhoScoredRequestPacer.StatisticsWorker worker) {
    this.requireOutsideTransaction();
    if (worker == null) {
      throw new PlayerStatisticsSynchronizationException();
    }
    String externalId =
        this.references
            .findByPlayerIdAndProvider(playerId, ExternalProvider.WHOSCORED)
            .orElseThrow(PlayerStatisticsSynchronizationException::new)
            .getExternalId();
    Instant start = Instant.now();
    try {
      WhoScoredStatisticsResponse response = this.integration.fetch(externalId, worker);
      List<PlayerMetricsContext> values = this.assembler.assemble(response);
      Long itemId = this.writer.confirm(runId, playerId, externalId, start, Instant.now(), values);
      boolean unknownColumns = this.unknownColumns(response);
      if (unknownColumns) {
        this.incidents.record(
            itemId, null, "PROVIDER_COLUMN_NOT_CATALOGED", null, 1, Instant.now());
      }
      return new Outcome(itemId, unknownColumns);
    } catch (WhoScoredStatisticsException failure) {
      PlayerExtractionResult result =
          switch (failure.getCode()) {
            case "PROVIDER_BLOCKED" -> PlayerExtractionResult.BLOCKED;
            case "PLAYER_NOT_FOUND" -> PlayerExtractionResult.NOT_FOUND;
            case "PROVIDER_STRUCTURE_CHANGED" -> PlayerExtractionResult.STRUCTURE_CHANGED;
            case "EXTRACTION_INTERRUPTED" -> PlayerExtractionResult.INTERRUPTED;
            default -> PlayerExtractionResult.PROVIDER_ERROR;
          };
      Long itemId =
          this.writer.recordFailure(
              runId, playerId, externalId, start, Instant.now(), result, failure.getCode());
      this.incidents.record(
          itemId, null, failure.getCode(), failure.getProviderStatus(), 1, Instant.now());
      throw failure;
    }
  }

  /** Columnas declaradas fuera del catálogo cerrado: se diagnostican sin crear métricas. */
  private boolean unknownColumns(WhoScoredStatisticsResponse response) {
    Set<String> known = new LinkedHashSet<>();
    for (var definition : MetricCatalog.definitions()) {
      definition
          .providerFields()
          .values()
          .forEach(
              fields -> {
                for (String field : fields) {
                  known.add(field.toLowerCase(java.util.Locale.ROOT));
                }
              });
    }
    List<String> unknown = new ArrayList<>();
    for (var table : response.tables()) {
      for (var cell : table.cells()) {
        String field = cell.providerField().strip().toLowerCase(java.util.Locale.ROOT);
        if (!known.contains(field) && !unknown.contains(field)) {
          try {
            MetricCatalog.canonicalKey(field);
          } catch (InvalidPlayerException absent) {
            unknown.add(field);
          }
        }
      }
    }
    return !unknown.isEmpty();
  }

  private void requireOutsideTransaction() {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new PlayerStatisticsSynchronizationException();
    }
  }
}
