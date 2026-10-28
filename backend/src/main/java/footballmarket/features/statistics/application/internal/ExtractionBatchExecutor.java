package footballmarket.features.statistics.application.internal;

import footballmarket.features.statistics.application.PlayerStatisticsExtractionService;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.features.statistics.domain.models.PlayerStatisticsRunItem;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsRunItemRepository;
import footballmarket.integrations.whoscored.WhoScoredScrapingSectionNavigator;
import footballmarket.integrations.whoscored.exceptions.HeadedBrowserUnavailableException;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Selección acotada y ejecución del lote sobre el navegador del canal.
 *
 * <p>El alcance deriva del ritmo observado por jugador en la campaña, del presupuesto restante y
 * del máximo opcional solicitado entre todos los workers. No se selecciona un jugador cuya
 * estimación no cabe y no se abre una transacción durante el acceso externo.
 */
@Component
public class ExtractionBatchExecutor {
  private static final long DEFAULT_PACE_MILLIS = 60_000L;
  private static final int PACE_SAMPLES = 20;
  private static final Set<String> UNAVAILABLE_CODES =
      Set.of("PROVIDER_RATE_LIMITED", "PROVIDER_TIMEOUT", "PROVIDER_TRANSPORT_ERROR");

  private final PlayerStatisticsEligibilitySelector selector;
  private final PlayerStatisticsRunItemRepository items;

  public ExtractionBatchExecutor(
      PlayerStatisticsEligibilitySelector selector, PlayerStatisticsRunItemRepository items) {
    this.selector = selector;
    this.items = items;
  }

  /** Ritmo reconstruido de las últimas confirmaciones de igual fecha y canal. */
  public long paceMillis(LocalDate extractionDate, MetricSource source) {
    List<PlayerStatisticsRunItem> samples =
        this.items.findByExtractionDateAndSourceAndResultOrderByFinishedAtDescIdDesc(
            extractionDate,
            source,
            PlayerExtractionResult.COMPLETED,
            PageRequest.of(0, PACE_SAMPLES));
    if (samples.isEmpty()) {
      return DEFAULT_PACE_MILLIS;
    }
    long total = 0;
    int measured = 0;
    for (PlayerStatisticsRunItem sample : samples) {
      if (sample.getFinishedAt() == null || sample.getStartedAt() == null) {
        continue;
      }
      long elapsed = Duration.between(sample.getStartedAt(), sample.getFinishedAt()).toMillis();
      if (elapsed > 0) {
        total += elapsed;
        measured++;
      }
    }
    // Solo las muestras con duración conocida entran en la media: una confirmación instantánea no
    // puede reducir el ritmo a cero ni provocar una división por cero en el cálculo de capacidad.
    return measured == 0 ? DEFAULT_PACE_MILLIS : total / measured;
  }

  /** Ejecuta el lote y devuelve lo alcanzado, sin confirmar datos parciales. */
  public Outcome execute(
      PlayerStatisticsExtractionService.Batch batch,
      Long runId,
      LocalDate extractionDate,
      MetricSource source,
      int workers,
      Integer maxPlayers,
      Instant deadline) {
    long pace = this.paceMillis(extractionDate, source);
    int capacity = this.capacity(pace, deadline);
    int limit = maxPlayers == null ? capacity : Math.min(capacity, maxPlayers);
    List<Long> selected =
        limit < 1 ? List.of() : this.selector.pendingPlayerIds(extractionDate, source, limit);
    if (selected.isEmpty()) {
      return new Outcome(0, 0, false, null);
    }
    AtomicBoolean interrupted = new AtomicBoolean();
    AtomicBoolean unavailable = new AtomicBoolean();
    AtomicInteger attempted = new AtomicInteger();
    AtomicLong providerCalls = new AtomicLong();
    try (ExtractionWorkerPool pool = ExtractionWorkerPool.open(workers)) {
      pool.forEach(
          selected,
          playerId -> {
            if (interrupted.get() || unavailable.get() || !isBeforeDeadline(deadline)) {
              return;
            }
            attempted.incrementAndGet();
            providerCalls.addAndGet(WhoScoredScrapingSectionNavigator.SECTIONS.size());
            try {
              batch.extract(runId, playerId, deadline);
            } catch (WhoScoredStatisticsException failure) {
              if ("EXTRACTION_INTERRUPTED".equals(failure.getCode())) {
                interrupted.set(true);
              } else if (UNAVAILABLE_CODES.contains(failure.getCode())) {
                unavailable.set(true);
              }
            } catch (HeadedBrowserUnavailableException unavailableBrowser) {
              throw unavailableBrowser;
            }
          });
    }
    String failureCode = unavailable.get() ? "PROVIDER_UNAVAILABLE" : null;
    return new Outcome(attempted.get(), providerCalls.get(), interrupted.get(), failureCode);
  }

  /**
   * Jugadores que caben en el presupuesto restante según el ritmo de la campaña.
   *
   * <p>Mientras quede presupuesto se admite al menos un jugador. El ritmo estimado es una media
   * histórica y, sin mediciones, un valor por defecto; ejecutar cero jugadores porque la estimación
   * no cabe deja la campaña detenida sin trabajo hecho y sin nada que lo explique. El recorrido de
   * cada jugador respeta el plazo del lote y se interrumpe si lo alcanza, de modo que admitir uno
   * no puede desbordar el presupuesto.
   */
  private int capacity(long paceMillis, Instant deadline) {
    if (deadline == null) {
      return Integer.MAX_VALUE;
    }
    long remaining = Duration.between(Instant.now(), deadline).toMillis();
    if (remaining <= 0) {
      return 0;
    }
    return (int) Math.max(1, Math.min(Integer.MAX_VALUE, remaining / paceMillis));
  }

  private static boolean isBeforeDeadline(Instant deadline) {
    return deadline == null || Instant.now().isBefore(deadline);
  }

  /** Resultado alcanzado por el lote, incluidos los pendientes no intentados. */
  public record Outcome(
      long selected, long providerCalls, boolean interrupted, String failureCode) {}
}
