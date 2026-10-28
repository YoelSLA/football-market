package footballmarket.features.statistics.application.internal;

import footballmarket.features.statistics.application.exceptions.PlayerStatisticsRunNotFoundException;
import footballmarket.features.statistics.application.exceptions.PlayerStatisticsSyncInProgressException;
import footballmarket.features.statistics.domain.enums.ExtractionRunStatus;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.features.statistics.domain.models.PlayerStatisticsExtractionRun;
import footballmarket.features.statistics.domain.models.PlayerStatisticsRunItem;
import footballmarket.features.statistics.domain.records.PlayerStatisticsCoverage;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncCounters;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncSummary;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsExtractionRunRepository;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsRunItemRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Apertura y cierre de ejecuciones de un canal con sus invariantes de contadores.
 *
 * <p>Una ejecución activa del mismo canal impide abrir otra. Solo una ejecución anterior cuyo
 * inicio excede el presupuesto del lote se considera interrumpida y se recupera como tal, porque
 * ninguna petición viva puede lasting ese tiempo; sus datos confirmados se conservan.
 */
@Component
public class ExtractionRunManager {
  private final PlayerStatisticsExtractionRunRepository runs;
  private final PlayerStatisticsRunItemRepository items;
  private final PlayerStatisticsCoverageQuery coverage;
  private final TransactionTemplate transaction;

  public ExtractionRunManager(
      PlayerStatisticsExtractionRunRepository runs,
      PlayerStatisticsRunItemRepository items,
      PlayerStatisticsCoverageQuery coverage,
      PlatformTransactionManager manager) {
    this.runs = runs;
    this.items = items;
    this.coverage = coverage;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /** Abre una ejecución del canal, recuperando únicamente ejecuciones ya inviables. */
  public PlayerStatisticsExtractionRun open(
      LocalDate extractionDate, MetricSource source, Integer maxPlayers, long budgetMillis) {
    Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    return this.transaction.execute(
        status -> {
          this.recover(
              this.runs.findBySourceAndStatus(source, ExtractionRunStatus.RUNNING).orElse(null),
              now.minusMillis(budgetMillis));
          PlayerStatisticsExtractionRun run =
              this.runs.saveAndFlush(
                  new PlayerStatisticsExtractionRun(extractionDate, source, now, maxPlayers));
          return run;
        });
  }

  private void recover(PlayerStatisticsExtractionRun active, Instant recoverableSince) {
    if (active == null) {
      return;
    }
    if (active.getStartedAt().isAfter(recoverableSince)) {
      throw new PlayerStatisticsSyncInProgressException();
    }
    this.finish(active, "EXECUTION_INTERRUPTED", true, 0);
  }

  /** Cierra la ejecución con los contadores observados y devuelve su resumen síncrono. */
  public PlayerStatisticsSyncSummary close(
      Long runId, ExtractionRunStatus requested, String failureCode, long providerCalls) {
    return this.transaction.execute(
        status ->
            this.finish(
                this.runs.findById(runId).orElseThrow(PlayerStatisticsRunNotFoundException::new),
                failureCode,
                requested == ExtractionRunStatus.INTERRUPTED,
                providerCalls));
  }

  private PlayerStatisticsSyncSummary finish(
      PlayerStatisticsExtractionRun run,
      String failureCode,
      boolean interrupted,
      long providerCalls) {
    List<PlayerStatisticsRunItem> items = this.items.findByRunIdOrderByPlayerIdAsc(run.getId());
    long completed =
        items.stream().filter(item -> item.getResult() == PlayerExtractionResult.COMPLETED).count();
    long withoutData =
        items.stream().filter(item -> item.getResult() == PlayerExtractionResult.NO_DATA).count();
    long failed = items.size() - completed - withoutData;
    long selected = completed + withoutData + failed;
    long present = items.stream().mapToLong(PlayerStatisticsRunItem::getPresentMetrics).sum();
    long absent = items.stream().mapToLong(PlayerStatisticsRunItem::getAbsentMetrics).sum();
    // El modelo por fila solo admite valor publicado o ausencia: nada queda por derivar.
    long notDerivable = 0L;
    PlayerStatisticsCoverage current =
        this.coverage.coverage(run.getExtractionDate(), run.getSource());
    ExtractionRunStatus status =
        selected == 0 && current.pendingPlayers() > 0
            ? ExtractionRunStatus.INTERRUPTED
            : resolve(selected, completed, withoutData, failed, interrupted);
    run.finish(
        status,
        Instant.now().truncatedTo(ChronoUnit.MICROS),
        current.eligiblePlayers(),
        current.excludedWithoutReference(),
        selected,
        completed,
        withoutData,
        failed,
        current.pendingPlayers(),
        present,
        absent,
        notDerivable,
        providerCalls,
        selected == 0 && current.pendingPlayers() > 0 ? "BUDGET_EXHAUSTED" : failureCode);
    this.runs.flush();
    return new PlayerStatisticsSyncSummary(
        run.getId(),
        run.getSource(),
        run.getExtractionDate(),
        run.getStatus(),
        run.getStartedAt(),
        run.getFinishedAt(),
        new PlayerStatisticsSyncCounters(
            current.eligiblePlayers(),
            current.excludedWithoutReference(),
            selected,
            completed,
            withoutData,
            failed,
            current.pendingPlayers(),
            present,
            absent,
            notDerivable),
        providerCalls,
        current.coverageComplete());
  }

  /**
   * El resultado refleja lo alcanzado por el trabajo, no la causa que cortó el recorrido.
   *
   * <p>Una ejecución que no llegó a seleccionar ningún jugador mientras quedaban pendientes no está
   * completa: se declara interrumpida antes de aplicar esta regla, porque reportarla como
   * completada presentaría una campaña terminada que en realidad no procesó a nadie.
   *
   * <p>Un corte de disponibilidad del proveedor solo se declara interrumpido cuando impidió
   * confirmar algún jugador. Si el recorrido ya había confirmado jugadores, el corte es un fallo
   * parcial de esos jugadores concretos y corresponde a {@code PARTIAL}: declararlo interrumpido
   * ocultaría el trabajo realizado y su confirmación ya persistida.
   */
  private static ExtractionRunStatus resolve(
      long selected, long completed, long withoutData, long failed, boolean interrupted) {
    if (selected == 0) {
      return ExtractionRunStatus.COMPLETED;
    }
    if (completed + withoutData == 0) {
      return interrupted ? ExtractionRunStatus.INTERRUPTED : ExtractionRunStatus.FAILED;
    }
    if (failed > 0) {
      return ExtractionRunStatus.PARTIAL;
    }
    return ExtractionRunStatus.COMPLETED;
  }
}
