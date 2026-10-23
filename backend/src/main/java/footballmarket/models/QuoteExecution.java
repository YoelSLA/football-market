package footballmarket.models;

import footballmarket.models.enums.ValuationStrategy;
import footballmarket.models.exceptions.InvalidQuoteException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

/**
 * Corrida persistida del recálculo de cotizaciones.
 *
 * <p>Es un registro append-only: cada fecha de cotización admite como máximo una ejecución, de modo
 * que el resumen de una fecha es único e inequívoco y los parámetros aplicados quedan en un único
 * lugar, sin duplicarse en cada fila del historial.
 */
@Entity
@Table(name = "quote_executions")
@Getter
public class QuoteExecution {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quote_execution_ids")
  @SequenceGenerator(
      name = "quote_execution_ids",
      sequenceName = "quote_executions_id_seq",
      allocationSize = 1)
  private Long id;

  @Column(name = "quote_date", nullable = false, updatable = false)
  private LocalDate quoteDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "strategy_name", nullable = false, updatable = false)
  private ValuationStrategy strategyName;

  @Column(name = "strategy_version", nullable = false, updatable = false)
  private String strategyVersion;

  @Column(name = "adjustment_factor", nullable = false, updatable = false)
  private BigDecimal adjustmentFactor;

  @Column(name = "variation_cap", nullable = false, updatable = false)
  private BigDecimal variationCap;

  @Column(name = "min_price_floor", nullable = false, updatable = false)
  private long minPriceFloor;

  @Column(name = "priced_players", nullable = false)
  private int pricedPlayers;

  @Column(name = "recomputed_rows", nullable = false)
  private int recomputedRows;

  @Column(name = "carried_forward_rows", nullable = false)
  private int carriedForwardRows;

  @Column(name = "not_repriced_players", nullable = false)
  private int notRepricedPlayers;

  @Column(name = "failed_players", nullable = false)
  private int failedPlayers;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "finished_at", nullable = false)
  private Instant finishedAt;

  protected QuoteExecution() {}

  /**
   * Crea el resumen persistido de una corrida de cotización.
   *
   * @param quoteDate fecha de cotización de la corrida
   * @param strategyName estrategia aplicada por la corrida
   * @param strategyVersion versión de la estrategia aplicada
   * @param adjustmentFactor factor de ajuste aplicado
   * @param variationCap tope de variación aplicado
   * @param minPriceFloor piso mínimo aplicado, en centésimas
   * @param pricedPlayers población cotizable evaluada
   * @param recomputedRows filas recalculadas escritas
   * @param carriedForwardRows filas arrastradas escritas
   * @param notRepricedPlayers jugadores no repreciados
   * @param failedPlayers jugadores con fallo individual
   * @param startedAt inicio de la corrida
   * @param finishedAt fin de la corrida
   * @throws InvalidQuoteException si algún valor persistido es inválido
   */
  public QuoteExecution(
      LocalDate quoteDate,
      ValuationStrategy strategyName,
      String strategyVersion,
      BigDecimal adjustmentFactor,
      BigDecimal variationCap,
      long minPriceFloor,
      int pricedPlayers,
      int recomputedRows,
      int carriedForwardRows,
      int notRepricedPlayers,
      int failedPlayers,
      Instant startedAt,
      Instant finishedAt) {
    if (quoteDate == null
        || strategyName == null
        || strategyVersion == null
        || adjustmentFactor == null
        || variationCap == null
        || startedAt == null
        || finishedAt == null) {
      throw new InvalidQuoteException("La ejecución de cotización requiere contexto completo");
    }
    if (adjustmentFactor.compareTo(new BigDecimal("0.10")) < 0
        || adjustmentFactor.compareTo(BigDecimal.ONE) > 0) {
      throw new InvalidQuoteException("El factor de ajuste debe estar en el rango [0.10; 1.00]");
    }
    if (variationCap.compareTo(BigDecimal.ZERO) <= 0
        || variationCap.compareTo(BigDecimal.ONE) > 0) {
      throw new InvalidQuoteException("El tope de variación debe estar en el rango (0; 1]");
    }
    if (minPriceFloor <= 0) {
      throw new InvalidQuoteException("El piso mínimo de precio debe ser mayor que cero");
    }
    if (pricedPlayers < 0
        || recomputedRows < 0
        || carriedForwardRows < 0
        || notRepricedPlayers < 0
        || failedPlayers < 0) {
      throw new InvalidQuoteException("Los contadores de la ejecución no pueden ser negativos");
    }
    this.quoteDate = quoteDate;
    this.strategyName = strategyName;
    this.strategyVersion = strategyVersion;
    this.adjustmentFactor = adjustmentFactor;
    this.variationCap = variationCap;
    this.minPriceFloor = minPriceFloor;
    this.pricedPlayers = pricedPlayers;
    this.recomputedRows = recomputedRows;
    this.carriedForwardRows = carriedForwardRows;
    this.notRepricedPlayers = notRepricedPlayers;
    this.failedPlayers = failedPlayers;
    this.startedAt = startedAt;
    this.finishedAt = finishedAt;
  }

  /** Completa los resultados de una corrida previamente reservada para su fecha. */
  public void complete(
      int pricedPlayers,
      int recomputedRows,
      int carriedForwardRows,
      int notRepricedPlayers,
      int failedPlayers,
      Instant finishedAt) {
    if (pricedPlayers < 0
        || recomputedRows < 0
        || carriedForwardRows < 0
        || notRepricedPlayers < 0
        || failedPlayers < 0
        || finishedAt == null
        || finishedAt.isBefore(this.startedAt)) {
      throw new InvalidQuoteException("El resultado de la ejecución de cotización es inválido");
    }
    this.pricedPlayers = pricedPlayers;
    this.recomputedRows = recomputedRows;
    this.carriedForwardRows = carriedForwardRows;
    this.notRepricedPlayers = notRepricedPlayers;
    this.failedPlayers = failedPlayers;
    this.finishedAt = finishedAt;
  }
}
