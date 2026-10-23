package footballmarket.support;

import footballmarket.models.AttributeValuation;
import footballmarket.models.Player;
import footballmarket.models.PlayerQuote;
import footballmarket.models.PositionalValuation;
import footballmarket.models.QuoteExecution;
import footballmarket.models.enums.GeneralPosition;
import footballmarket.models.enums.MetricSourceSummary;
import footballmarket.models.enums.QuoteOrigin;
import footballmarket.models.enums.ValuationStrategy;
import footballmarket.repositories.AttributeValuationRepository;
import footballmarket.repositories.PlayerQuoteRepository;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.PositionalValuationRepository;
import footballmarket.repositories.QuoteExecutionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.transaction.annotation.Transactional;

/** Prepara datos persistidos del ranking por score exclusivamente durante Arrange. */
public class ScoreRankingTestData {
  private final PlayerRepository players;
  private final PlayerQuoteRepository quotes;
  private final PositionalValuationRepository valuations;
  private final AttributeValuationRepository attributeValuations;
  private final QuoteExecutionRepository executions;

  public ScoreRankingTestData(
      PlayerRepository players,
      PlayerQuoteRepository quotes,
      PositionalValuationRepository valuations,
      AttributeValuationRepository attributeValuations,
      QuoteExecutionRepository executions) {
    this.players = players;
    this.quotes = quotes;
    this.valuations = valuations;
    this.attributeValuations = attributeValuations;
    this.executions = executions;
  }

  /** Persiste una valuación posicional adicional para comprobar la selección por fecha. */
  @Transactional
  public void preparePositionalValuation(Player player, LocalDate valuationDate, BigDecimal score) {
    Player managed = this.players.findById(player.getId()).orElseThrow();
    this.valuations.save(positionalValuation(managed, valuationDate, score));
  }

  /** Persiste cotización y valuación por atributos para un jugador. */
  @Transactional
  public void prepareAttributePlayer(
      Player player, LocalDate quoteDate, LocalDate valuationDate, BigDecimal score) {
    prepareAttributePlayer(player, quoteDate, valuationDate, score, score);
  }

  /** Persiste cotización y valuación por atributos con resultado bruto distinto del final. */
  @Transactional
  public void prepareAttributePlayer(
      Player player,
      LocalDate quoteDate,
      LocalDate valuationDate,
      BigDecimal rawScore,
      BigDecimal finalScore) {
    Player managed = this.players.findById(player.getId()).orElseThrow();
    this.quotes.save(
        new PlayerQuote(
            managed,
            quoteDate,
            100L,
            null,
            BigDecimal.ONE,
            null,
            null,
            null,
            null,
            QuoteOrigin.INITIAL,
            quoteDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));
    this.attributeValuations.save(attributeValuation(managed, valuationDate, rawScore, finalScore));
  }

  /** Persiste una valuación por atributos sin crear cotización para el jugador. */
  @Transactional
  public void prepareAttributeValuation(Player player, LocalDate valuationDate, BigDecimal score) {
    Player managed = this.players.findById(player.getId()).orElseThrow();
    this.attributeValuations.save(attributeValuation(managed, valuationDate, score, score));
  }

  /** Persiste cotización y valuación posicional para un jugador. */
  @Transactional
  public void preparePlayer(
      Player player, LocalDate quoteDate, LocalDate valuationDate, BigDecimal score) {
    Player managed = this.players.findById(player.getId()).orElseThrow();
    this.quotes.save(
        new PlayerQuote(
            managed,
            quoteDate,
            100L,
            null,
            BigDecimal.ONE,
            null,
            null,
            null,
            null,
            QuoteOrigin.INITIAL,
            quoteDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));
    this.valuations.save(positionalValuation(managed, valuationDate, score));
  }

  /** Persiste el encabezado de una ejecución posicional. */
  @Transactional
  public void prepareExecution(LocalDate quoteDate) {
    this.prepareExecution(quoteDate, ValuationStrategy.POSITIONAL_PERFORMANCE);
  }

  /** Persiste el encabezado de una ejecución para la estrategia indicada. */
  @Transactional
  public void prepareExecution(LocalDate quoteDate, ValuationStrategy strategy) {
    Instant startedAt = quoteDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
    this.executions.save(
        new QuoteExecution(
            quoteDate,
            strategy,
            "v1",
            new BigDecimal("0.10"),
            new BigDecimal("0.10"),
            1L,
            0,
            0,
            0,
            0,
            0,
            startedAt,
            startedAt.plusSeconds(1)));
  }

  private static PositionalValuation positionalValuation(
      Player player, LocalDate valuationDate, BigDecimal score) {
    return new PositionalValuation(
        player,
        valuationDate,
        GeneralPosition.FORWARD,
        MetricSourceSummary.JSON,
        valuationDate,
        score,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        score,
        BigDecimal.ONE,
        score,
        new String[0],
        valuationDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
  }

  private static AttributeValuation attributeValuation(
      Player player, LocalDate valuationDate, BigDecimal rawScore, BigDecimal finalScore) {
    return new AttributeValuation(
        player,
        valuationDate,
        GeneralPosition.FORWARD,
        MetricSourceSummary.JSON,
        valuationDate,
        rawScore,
        rawScore,
        rawScore,
        rawScore,
        rawScore,
        null,
        null,
        null,
        rawScore,
        BigDecimal.ZERO,
        rawScore,
        BigDecimal.ONE,
        finalScore,
        new String[0],
        valuationDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
  }
}
