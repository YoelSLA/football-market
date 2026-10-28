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
 * vigencia máxima y, a igualdad de fecha, la fila {@code RECOMPUTED}, escrita después que la
 * arrastrada. Cada fila conserva la trazabilidad de lo que produjo su precio: estrategia y versión,
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

  @Column(name = "price", nullable = false, updatable = false)
  private long price;

  @Column(name = "previous_price", updatable = false)
  private Long previousPrice;

  @Column(name = "multiplier", nullable = false, updatable = false)
  private BigDecimal multiplier;

  @Column(name = "score", updatable = false)
  private BigDecimal score;

  @Column(name = "strategy_name", updatable = false)
  @Enumerated(EnumType.STRING)
  private ValuationStrategy strategyName;

  @Column(name = "strategy_version", updatable = false)
  private String strategyVersion;

  @Column(name = "source_valuation_date", updatable = false)
  private LocalDate sourceValuationDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "origin", nullable = false, updatable = false)
  private QuoteOrigin origin;

  @Column(name = "calculated_at", nullable = false, updatable = false)
  private Instant calculatedAt;

  protected PlayerQuote() {}

  /**
   * Crea una fila del historial con los invariantes de su origen.
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
