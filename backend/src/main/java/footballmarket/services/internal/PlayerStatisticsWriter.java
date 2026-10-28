package footballmarket.services.internal;

import footballmarket.models.MetricCatalog;
import footballmarket.models.Player;
import footballmarket.models.PlayerStatistics;
import footballmarket.models.PlayerStatisticsExtractionRun;
import footballmarket.models.PlayerStatisticsRunItem;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.enums.ExtractionRunStatus;
import footballmarket.models.enums.MetricSource;
import footballmarket.models.enums.PlayerExtractionResult;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.PlayerStatisticsExtractionRunRepository;
import footballmarket.repositories.PlayerStatisticsRepository;
import footballmarket.repositories.PlayerStatisticsRunItemRepository;
import footballmarket.services.exceptions.PlayerStatisticsConfirmationException;
import footballmarket.services.exceptions.PlayerStatisticsRunNotFoundException;
import footballmarket.services.exceptions.PlayerStatisticsSynchronizationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Confirma un jugador en una sola fila por campaña y canal.
 *
 * <p>No realiza llamadas externas; una colisión revierte también la fila de métricas. El contexto
 * se limita a la liga local del jugador y no se suman contextos ni temporadas.
 *
 * <p>Una fila cuya liga todavía no había publicado contexto puede reconfirmarse: el recorrido
 * vuelve a seleccionar a ese jugador porque solo una confirmación {@code COMPLETED} excluye de la
 * campaña, y su reconfirmación reemplaza la fila en lugar de competir con ella por la clave única.
 */
@Component
public class PlayerStatisticsWriter {
  private static final String RESOLVED = PlayerStatistics.RESOLVED;
  private static final String NO_LEAGUE_CONTEXT = PlayerStatistics.NO_LEAGUE_CONTEXT;

  private final PlayerStatisticsExtractionRunRepository runs;
  private final PlayerRepository players;
  private final PlayerStatisticsRunItemRepository items;
  private final PlayerStatisticsRepository statistics;
  private final LeagueContextSelection selection;
  private final TransactionTemplate transaction;

  public PlayerStatisticsWriter(
      PlayerStatisticsExtractionRunRepository runs,
      PlayerRepository players,
      PlayerStatisticsRunItemRepository items,
      PlayerStatisticsRepository statistics,
      LeagueContextSelection selection,
      PlatformTransactionManager manager) {
    this.runs = runs;
    this.players = players;
    this.items = items;
    this.statistics = statistics;
    this.selection = selection;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Persiste la fila del jugador con el contexto de su liga.
   *
   * @param contexts contextos candidatos publicados por el proveedor
   */
  public Long confirm(
      Long runId,
      Long playerId,
      String providerPlayerId,
      Instant startedAt,
      Instant finishedAt,
      java.util.List<PlayerMetricsContext> contexts) {
    try {
      return this.transaction.execute(
          status ->
              this.write(
                  runId,
                  playerId,
                  providerPlayerId,
                  startedAt,
                  finishedAt,
                  java.util.List.copyOf(contexts)));
    } catch (PlayerStatisticsRunNotFoundException
        | PlayerStatisticsSynchronizationException failure) {
      throw failure;
    } catch (PlayerStatisticsConfirmationException failure) {
      throw failure;
    } catch (RuntimeException failure) {
      throw new PlayerStatisticsConfirmationException(isCompletedCollision(failure));
    }
  }

  /**
   * Devuelve la fila de la campaña del jugador, creándola si todavía no existe.
   *
   * <p>Una fila confirmada no se sobrescribe ni se degrada: se rechaza como campaña repetida para
   * no mezclar sus métricas con las de otro contexto. Una fila sin contexto o, en scraping, sin
   * valores ni confirmación satisfactoria admite reconfirmación posterior.
   */
  private PlayerStatistics reconfirmOrCreate(
      PlayerStatisticsExtractionRun run,
      Player player,
      PlayerStatisticsRunItem item,
      String contextStatus,
      Instant finishedAt) {
    Optional<PlayerStatistics> existing =
        this.statistics.findCampaignRow(player.getId(), run.getExtractionDate(), run.getSource());
    if (existing.isEmpty()) {
      return new PlayerStatistics(
          player, run.getExtractionDate(), run.getSource(), contextStatus, item, finishedAt);
    }
    PlayerStatistics row = existing.get();
    if (this.items.existsByPlayerIdAndExtractionDateAndSourceAndResult(
            player.getId(),
            run.getExtractionDate(),
            run.getSource(),
            PlayerExtractionResult.COMPLETED)
        || (!NO_LEAGUE_CONTEXT.equals(row.getContextStatus())
            && !(run.getSource() == MetricSource.SCRAPING && row.getPresentMetrics() == 0))) {
      throw new PlayerStatisticsConfirmationException(true);
    }
    row.reconfirm(contextStatus, item, finishedAt);
    return row;
  }

  private Long write(
      Long runId,
      Long playerId,
      String providerPlayerId,
      Instant startedAt,
      Instant finishedAt,
      java.util.List<PlayerMetricsContext> contexts) {
    PlayerStatisticsExtractionRun run =
        this.runs.findById(runId).orElseThrow(PlayerStatisticsRunNotFoundException::new);
    Player player =
        this.players.findById(playerId).orElseThrow(PlayerStatisticsSynchronizationException::new);
    if (run.getStatus() != ExtractionRunStatus.RUNNING
        || player.getExternalReferences().stream()
            .noneMatch(
                reference ->
                    reference.getProvider() == ExternalProvider.WHOSCORED
                        && reference.getExternalId().equals(providerPlayerId))) {
      throw new PlayerStatisticsSynchronizationException();
    }
    PlayerStatisticsRunItem item =
        new PlayerStatisticsRunItem(run, player, providerPlayerId, startedAt);

    Optional<PlayerMetricsContext> resolved = this.selection.selectForLeague(player, contexts);
    PlayerStatistics row =
        this.reconfirmOrCreate(
            run, player, item, resolved.isPresent() ? RESOLVED : NO_LEAGUE_CONTEXT, finishedAt);
    Map<String, PlayerMetricsContext.MetricResult> metrics =
        resolved.map(PlayerMetricsContext::metrics).orElseGet(Map::of);
    int catalogSize =
        (int)
            MetricCatalog.definitions().stream()
                .filter(definition -> definition.sources().contains(run.getSource()))
                .count();
    if (resolved.isPresent()) {
      PlayerMetricsContext context = resolved.get();
      row.resolveContext(
          context.competitionId(), context.teamId(), context.seasonKey(), context.seasonLabel());
    }
    int present =
        run.getSource() == MetricSource.SCRAPING
            ? (int)
                MetricCatalog.definitions().stream()
                    .filter(definition -> definition.sources().contains(MetricSource.SCRAPING))
                    .filter(
                        definition ->
                            metrics.containsKey(definition.key())
                                && metrics.get(definition.key()).value() != null)
                    .count()
            : metrics.size();
    row.applyMetrics(this.values(metrics), present, Math.max(0, catalogSize - present));

    item.finish(
        resolved.isPresent() && (run.getSource() != MetricSource.SCRAPING || present > 0)
            ? PlayerExtractionResult.COMPLETED
            : PlayerExtractionResult.NO_DATA,
        finishedAt,
        resolved.isPresent() ? 1 : 0,
        present,
        Math.max(0, catalogSize - present),
        null);
    this.items.save(item);
    if (row.getId() == null) {
      this.statistics.saveAndFlush(row);
    } else {
      this.statistics.flush();
    }
    return item.getId();
  }

  private Map<String, java.math.BigDecimal> values(
      Map<String, PlayerMetricsContext.MetricResult> metrics) {
    Map<String, java.math.BigDecimal> values = new LinkedHashMap<>();
    metrics.forEach((key, result) -> values.put(key, result.value()));
    return values;
  }

  private static boolean isCompletedCollision(Throwable failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof org.postgresql.util.PSQLException postgres
          && "23505".equals(postgres.getSQLState())
          && postgres.getServerErrorMessage() != null
          && "uq_player_statistics".equals(postgres.getServerErrorMessage().getConstraint())) {
        return true;
      }
      if (cause.getCause() == cause) {
        break;
      }
    }
    return false;
  }

  /** Audita un intento fallido sin crear la fila de métricas. */
  public Long recordFailure(
      Long runId,
      Long playerId,
      String providerPlayerId,
      Instant startedAt,
      Instant finishedAt,
      PlayerExtractionResult result,
      String code) {
    return this.transaction.execute(
        status -> {
          PlayerStatisticsExtractionRun run =
              this.runs.findById(runId).orElseThrow(PlayerStatisticsRunNotFoundException::new);
          Player player =
              this.players
                  .findById(playerId)
                  .orElseThrow(PlayerStatisticsSynchronizationException::new);
          PlayerStatisticsRunItem item =
              new PlayerStatisticsRunItem(run, player, providerPlayerId, startedAt);
          item.finish(result, finishedAt, 0, 0, 0, code);
          return this.items.saveAndFlush(item).getId();
        });
  }
}
