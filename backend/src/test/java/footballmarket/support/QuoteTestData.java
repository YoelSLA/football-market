package footballmarket.support;

import footballmarket.models.MetricCatalog;
import footballmarket.models.Player;
import footballmarket.models.PlayerQuote;
import footballmarket.models.PlayerStatistics;
import footballmarket.models.PlayerStatisticsExtractionRun;
import footballmarket.models.PlayerStatisticsRunItem;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.enums.ExtractionRunStatus;
import footballmarket.models.enums.MetricSource;
import footballmarket.models.enums.PlayerExtractionResult;
import footballmarket.models.enums.QuoteOrigin;
import footballmarket.models.enums.ValuationStrategy;
import footballmarket.repositories.PlayerQuoteRepository;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.PlayerStatisticsExtractionRunRepository;
import footballmarket.repositories.PlayerStatisticsRepository;
import footballmarket.repositories.PlayerStatisticsRunItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixture compartido que prepara métricas y filas del historial de cotizaciones exclusivamente
 * durante Arrange.
 *
 * <p>Permite dejar a un jugador sin métricas y persistir filas con precios distintos, de modo que
 * el ranking pueda verificarse sin depender del resultado de un recálculo concreto.
 */
public class QuoteTestData {
  private final PlayerQuoteRepository quotes;
  private final PlayerStatisticsRepository statistics;
  private final PlayerStatisticsExtractionRunRepository runs;
  private final PlayerStatisticsRunItemRepository items;
  private final PlayerRepository players;

  public QuoteTestData(
      PlayerQuoteRepository quotes,
      PlayerStatisticsRepository statistics,
      PlayerStatisticsExtractionRunRepository runs,
      PlayerStatisticsRunItemRepository items,
      PlayerRepository players) {
    this.quotes = quotes;
    this.statistics = statistics;
    this.runs = runs;
    this.items = items;
    this.players = players;
  }

  /**
   * Deja al jugador con métricas informadas en la fecha indicada, por lo que es cotizable.
   *
   * <p>El jugador se vuelve a leer dentro de la transacción: la colección de referencias externas
   * es perezosa y el jugador recibido puede estar desconectado, de modo que añadirle una identidad
   * fuera de sesión fallaría.
   */
  @Transactional
  public void prepareMetrics(Player player, LocalDate date) {
    Player managed = this.players.findById(player.getId()).orElseThrow();
    PlayerStatisticsExtractionRun run = this.campaignRun(date);
    String externalId = externalId(managed);
    managed.addExternalReference(ExternalProvider.WHOSCORED, externalId);
    this.players.save(managed);
    PlayerStatisticsRunItem item =
        new PlayerStatisticsRunItem(run, managed, externalId, Instant.EPOCH);
    item.finish(PlayerExtractionResult.COMPLETED, Instant.EPOCH.plusSeconds(1), 1, 57, 0, null);
    this.items.save(item);
    PlayerStatistics row =
        new PlayerStatistics(
            managed,
            date,
            MetricSource.JSON,
            PlayerStatistics.RESOLVED,
            item,
            Instant.EPOCH.plusSeconds(1));
    row.resolveContext("2", "team", "2026", "2026/27");
    Map<String, BigDecimal> metrics = new LinkedHashMap<>();
    for (footballmarket.models.MetricDefinition definition : MetricCatalog.definitions()) {
      if (definition.sources().contains(MetricSource.JSON)) {
        metrics.put(definition.key(), BigDecimal.ONE);
      }
    }
    row.applyMetrics(metrics, metrics.size(), 60 - metrics.size());
    this.statistics.save(row);
  }

  /**
   * Campaña JSON de la fecha indicada, reutilizada entre jugadores.
   *
   * <p>Solo puede existir una ejecución en curso por canal, de modo que la campaña se crea una vez
   * por fecha y se reutiliza para los demás jugadores. La ejecución se cierra antes de persistirla:
   * dejarla abierta bloquearía cualquier campaña posterior del mismo canal.
   */
  private PlayerStatisticsExtractionRun campaignRun(LocalDate date) {
    List<PlayerStatisticsExtractionRun> existing =
        this.runs.findByExtractionDateAndSourceOrderByStartedAtDescIdDesc(date, MetricSource.JSON);
    if (!existing.isEmpty()) {
      return existing.getFirst();
    }
    PlayerStatisticsExtractionRun run =
        new PlayerStatisticsExtractionRun(date, MetricSource.JSON, Instant.EPOCH);
    run.finish(
        ExtractionRunStatus.COMPLETED,
        Instant.EPOCH.plusSeconds(2),
        1,
        0,
        1,
        1,
        0,
        0,
        0,
        0,
        0,
        0,
        1,
        null);
    return this.runs.save(run);
  }

  /** Persiste la fila inicial del ancla de un crédito. */
  @Transactional
  public void persistInitial(Player player, LocalDate effectiveDate, Instant calculatedAt) {
    this.quotes.save(
        new PlayerQuote(
            managed(player),
            effectiveDate,
            100L,
            null,
            BigDecimal.ONE,
            null,
            null,
            null,
            null,
            QuoteOrigin.INITIAL,
            calculatedAt));
  }

  /** Persiste la fila recalculada del jugador con el precio indicado. */
  @Transactional
  public void persistRecomputed(
      Player player, LocalDate effectiveDate, long price, Instant calculatedAt) {
    this.quotes.save(
        new PlayerQuote(
            managed(player),
            effectiveDate,
            price,
            100L,
            BigDecimal.ONE,
            new BigDecimal("0.6"),
            ValuationStrategy.POSITIONAL_PERFORMANCE,
            "v1",
            effectiveDate,
            QuoteOrigin.RECOMPUTED,
            calculatedAt));
  }

  /** Persiste la fila arrastrada del jugador, que repite su precio vigente. */
  @Transactional
  public void persistCarriedForward(
      Player player, LocalDate effectiveDate, long price, Instant calculatedAt) {
    this.quotes.save(
        new PlayerQuote(
            managed(player),
            effectiveDate,
            price,
            price,
            BigDecimal.ONE,
            null,
            ValuationStrategy.POSITIONAL_PERFORMANCE,
            "v1",
            null,
            QuoteOrigin.CARRIED_FORWARD,
            calculatedAt));
  }

  /**
   * Relee al jugador dentro de la transacción.
   *
   * <p>El jugador puede llegar desconectado de la sesión que lo leyó, de modo que se vuelve a leer
   * para asociarlo a la fila sin depender de la colección perezosa de referencias externas.
   */
  private Player managed(Player player) {
    return this.players.findById(player.getId()).orElseThrow();
  }

  private static String externalId(Player player) {
    return Long.toString(900_000L + player.getId());
  }
}
