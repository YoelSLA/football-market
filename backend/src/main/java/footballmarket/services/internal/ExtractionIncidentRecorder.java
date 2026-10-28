package footballmarket.services.internal;

import footballmarket.models.ExtractionIncident;
import footballmarket.repositories.ExtractionIncidentRepository;
import footballmarket.repositories.PlayerStatisticsRunItemRepository;
import footballmarket.services.exceptions.PlayerStatisticsSynchronizationException;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Persiste únicamente códigos y secciones admitidas, sin contenido externo ni secretos. */
@Component
public class ExtractionIncidentRecorder {
  private static final Set<String> CODES =
      Set.of(
          "PROVIDER_BLOCKED",
          "PLAYER_NOT_FOUND",
          "PROVIDER_TIMEOUT",
          "PROVIDER_TRANSPORT_ERROR",
          "PROVIDER_STRUCTURE_CHANGED",
          "PROVIDER_COLUMN_NOT_CATALOGED",
          "PROVIDER_DATA_INCONSISTENT",
          "HEADED_BROWSER_UNAVAILABLE",
          "EXTRACTION_INTERRUPTED");

  /**
   * Secciones que el recorrido puede declarar.
   *
   * <p>El scraping recorre la carga inicial y la tabla de detalle completa, y nombra la categoría
   * que falló. El canal JSON nombra su combinación de categoría y subcategoría. No se admiten
   * otras: una sección no prevista indicaría un recorrido distinto del implementado.
   */
  private static final Set<String> SECTIONS =
      Set.of(
          "initial",
          "detailed",
          "aerial",
          "assists",
          "blocks",
          "cards",
          "clearances",
          "dribbles",
          "fouls",
          "goals",
          "interception",
          "key-passes",
          "offsides",
          "passes",
          "possession-loss",
          "saves",
          "shots",
          "tackles",
          "summary/all",
          "summary/defensive",
          "summary/offensive",
          "summary/passing",
          "aerial/success",
          "assists/type",
          "blocks/type",
          "cards/type",
          "clearances/success",
          "dribbles/success",
          "fouls/type",
          "goals/zones",
          "interception/success",
          "key-passes/length",
          "offsides/type",
          "passes/length",
          "possession-loss/type",
          "saves/shotzone",
          "shots/zones",
          "tackles/success");

  private final PlayerStatisticsRunItemRepository items;
  private final ExtractionIncidentRepository incidents;
  private final TransactionTemplate transaction;

  public ExtractionIncidentRecorder(
      PlayerStatisticsRunItemRepository items,
      ExtractionIncidentRepository incidents,
      PlatformTransactionManager manager) {
    this.items = items;
    this.incidents = incidents;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public void record(
      Long itemId,
      String section,
      String code,
      Integer providerStatus,
      int attempt,
      Instant occurredAt) {
    if (code == null
        || !CODES.contains(code)
        || (section != null && !SECTIONS.contains(section))
        || (providerStatus != null && (providerStatus < 100 || providerStatus > 599))) {
      throw new PlayerStatisticsSynchronizationException();
    }
    this.transaction.executeWithoutResult(
        status ->
            this.incidents.saveAndFlush(
                new ExtractionIncident(
                    this.items
                        .findById(itemId)
                        .orElseThrow(PlayerStatisticsSynchronizationException::new),
                    section,
                    code,
                    providerStatus,
                    attempt,
                    occurredAt)));
  }
}
