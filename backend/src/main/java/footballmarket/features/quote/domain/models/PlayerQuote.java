package footballmarket.features.quote.domain.models;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.exceptions.InvalidQuoteException;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

/**
 * Fila del historial de cotizaciones de un jugador.
 *
 * <p>La tabla es el historial completo: la cotización vigente del jugador es su fila de fecha de
 * vigencia máxima. Hay una única fila por jugador y fecha, actualizada por un cálculo posterior.
 * Cada fila conserva la trazabilidad de lo que produjo su último precio: estrategia y versión,
 * score, precio anterior, multiplicador, fecha de la valuación de origen e instante de cálculo.
 */
@Entity
@Table(name = "player_quotes")
@Getter
public class PlayerQuote {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_quote_ids")
  @SequenceGenerator(
      name = "player_quote_ids",
      sequenceName = "player_quotes_id_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false, updatable = false)
  private Player player;

  @Column(name = "effective_date", nullable = false, updatable = false)
  private LocalDate effectiveDate;

  @Column(name = "price", nullable = false)
  private long price;

  @Column(name = "previous_price")
  private Long previousPrice;

  @Column(name = "multiplier", nullable = false)
  private BigDecimal multiplier;

  @Column(name = "score")
  private BigDecimal score;

  @Column(name = "strategy_name")
  @Enumerated(EnumType.STRING)
  private ValuationStrategy strategyName;

  @Column(name = "strategy_version")
  private String strategyVersion;

  @Column(name = "source_valuation_date")
  private LocalDate sourceValuationDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "origin", nullable = false)
  private QuoteOrigin origin;

  @Column(name = "calculated_at", nullable = false)
  private Instant calculatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "execution_id")
  private QuoteExecution execution;

  protected PlayerQuote() {}

  /**
   * Crea exclusivamente una fila INITIAL; los otros orígenes requieren ejecución auditada.
   *
   * @param player jugador cotizable de la fila
   * @param effectiveDate fecha de vigencia de la fila
   * @param price precio resultante en centésimas de crédito, siempre mayor que cero
   * @param previousPrice precio anterior, nulo solo en la fila inicial
   * @param multiplier multiplicador aplicado a seis decimales
   * @param score score utilizado, nulo en las filas inicial y arrastrada
   * @param strategyName estrategia que produjo el precio, nula en la fila inicial
   * @param strategyVersion versión de la estrategia, nula en la fila inicial
   * @param sourceValuationDate fecha de la valuación de origen, nula sin valuación
   * @param origin origen de la fila
   * @param calculatedAt instante de cálculo de la fila
   * @throws InvalidQuoteException si los invariantes del origen no se cumplen
   */
  public PlayerQuote(
      Player player,
      LocalDate effectiveDate,
      long price,
      Long previousPrice,
      BigDecimal multiplier,
      BigDecimal score,
      ValuationStrategy strategyName,
      String strategyVersion,
      LocalDate sourceValuationDate,
      QuoteOrigin origin,
      Instant calculatedAt) {
    this(
        player,
        effectiveDate,
        price,
        previousPrice,
        multiplier,
        score,
        strategyName,
        strategyVersion,
        sourceValuationDate,
        origin,
        calculatedAt,
        null);
  }

  /** Crea una fila auditada; solo INITIAL carece de referencia a una corrida. */
  public PlayerQuote(
      Player player,
      LocalDate effectiveDate,
      long price,
      Long previousPrice,
      BigDecimal multiplier,
      BigDecimal score,
      ValuationStrategy strategyName,
      String strategyVersion,
      LocalDate sourceValuationDate,
      QuoteOrigin origin,
      Instant calculatedAt,
      QuoteExecution execution) {
    if (player == null
        || effectiveDate == null
        || multiplier == null
        || origin == null
        || calculatedAt == null) {
      throw new InvalidQuoteException("La fila de cotización requiere contexto completo");
    }
    if (price <= 0) {
      throw new InvalidQuoteException("El precio de la cotización debe ser mayor que cero");
    }
    if ((origin == QuoteOrigin.INITIAL) != (execution == null)) {
      throw new InvalidQuoteException("Solo la cotización inicial carece de ejecución de origen");
    }
    this.assertOriginInvariants(
        origin,
        previousPrice,
        multiplier,
        score,
        strategyName,
        strategyVersion,
        sourceValuationDate);
    this.player = player;
    this.effectiveDate = effectiveDate;
    this.price = price;
    this.previousPrice = previousPrice;
    this.multiplier = multiplier;
    this.score = score;
    this.strategyName = strategyName;
    this.strategyVersion = strategyVersion;
    this.sourceValuationDate = sourceValuationDate;
    this.origin = origin;
    this.calculatedAt = calculatedAt;
    this.execution = execution;
  }

  /** Sustituye el cálculo diario completo sin cambiar la identidad, el jugador ni la fecha. */
  public void replaceCalculation(PlayerQuote calculation) {
    if (calculation == null
        || !this.player.getId().equals(calculation.getPlayer().getId())
        || !this.effectiveDate.equals(calculation.getEffectiveDate())) {
      throw new InvalidQuoteException("El cálculo debe corresponder al mismo jugador y fecha");
    }
    this.price = calculation.getPrice();
    this.previousPrice = calculation.getPreviousPrice();
    this.multiplier = calculation.getMultiplier();
    this.score = calculation.getScore();
    this.strategyName = calculation.getStrategyName();
    this.strategyVersion = calculation.getStrategyVersion();
    this.sourceValuationDate = calculation.getSourceValuationDate();
    this.origin = calculation.getOrigin();
    this.calculatedAt = calculation.getCalculatedAt();
    this.execution = calculation.getExecution();
  }

  private void assertOriginInvariants(
      QuoteOrigin origin,
      Long previousPrice,
      BigDecimal multiplier,
      BigDecimal score,
      ValuationStrategy strategyName,
      String strategyVersion,
      LocalDate sourceValuationDate) {
    boolean hasPreviousPrice = previousPrice != null && previousPrice > 0;
    if (origin == QuoteOrigin.INITIAL) {
      if (previousPrice != null
          || !this.isUnitMultiplier(multiplier)
          || score != null
          || strategyName != null
          || strategyVersion != null
          || sourceValuationDate != null) {
        throw new InvalidQuoteException(
            "La fila inicial solo admite el ancla sin estrategia ni score");
      }
      return;
    }
    if (!hasPreviousPrice) {
      throw new InvalidQuoteException("Una cotización no inicial exige el precio anterior");
    }
    if (origin == QuoteOrigin.CARRIED_FORWARD) {
      if (score != null || sourceValuationDate != null || !this.isUnitMultiplier(multiplier)) {
        throw new InvalidQuoteException(
            "La fila arrastrada repite el precio sin score, sin valuación y con multiplicador 1");
      }
      return;
    }
    if (score == null
        || strategyName == null
        || strategyVersion == null
        || sourceValuationDate == null) {
      throw new InvalidQuoteException(
          "La cotización recalculada exige score, estrategia, versión y fecha de valuación");
    }
  }

  private static boolean isUnitMultiplier(BigDecimal multiplier) {
    return multiplier.compareTo(BigDecimal.ONE) == 0;
  }
}
