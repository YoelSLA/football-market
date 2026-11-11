package footballmarket.features.quote.domain.models;

import footballmarket.features.quote.domain.enums.QuoteExecutionFailureReason;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.enums.QuoteExecutionResult;
import footballmarket.features.quote.domain.exceptions.InvalidQuoteException;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
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
 * <p>Cada corrida tiene identidad propia y admite un único cierre explícito. Sus parámetros
 * permanecen inmutables; un contexto sin cierre no constituye un resultado finalizado.
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
  @Column(name = "origin", nullable = false, updatable = false, length = 16)
  private QuoteExecutionOrigin origin;

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

  @Column(name = "finished_at")
  private Instant finishedAt;

  protected QuoteExecution() {}

  /**
   * Crea un contexto abierto con contadores en cero o un resumen ya finalizado y consistente.
   *
   * @param quoteDate fecha de cotización de la corrida
   * @param origin origen manual o automático de la corrida
   * @param strategyName estrategia aplicada por la corrida
   * @param strategyVersion versión de la estrategia aplicada
   * @param adjustmentFactor factor de ajuste aplicado
   * @param variationCap tope de variación aplicado
   * @param minPriceFloor piso mínimo aplicado, en centésimas
   * @param pricedPlayers población cotizable evaluada
   * @param recomputedRows filas recalculadas escritas
   * @param carriedForwardRows filas arrastradas escritas
   * @param notRepricedPlayers omitidos por cotización vigente del mismo día o posterior
   * @param failedPlayers jugadores con fallo individual
   * @param startedAt inicio de la corrida
   * @param finishedAt fin de la corrida, nulo mientras el contexto está abierto
   * @throws InvalidQuoteException si algún valor persistido es inválido
   */
  public QuoteExecution(
      LocalDate quoteDate,
      QuoteExecutionOrigin origin,
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
        || origin == null
        || strategyName == null
        || strategyVersion == null
        || adjustmentFactor == null
        || variationCap == null
        || startedAt == null) {
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
    if ((finishedAt != null
            && (finishedAt.isBefore(startedAt)
                || pricedPlayers
                    != (long) recomputedRows
                        + carriedForwardRows
                        + notRepricedPlayers
                        + failedPlayers))
        || (finishedAt == null
            && (pricedPlayers != 0
                || recomputedRows != 0
                || carriedForwardRows != 0
                || notRepricedPlayers != 0
                || failedPlayers != 0))) {
      throw new InvalidQuoteException(
          "Los contadores y el cierre de la ejecución son inconsistentes");
    }
    this.quoteDate = quoteDate;
    this.origin = origin;
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

  /** Deriva el resultado exclusivamente de los contadores persistidos. */
  public QuoteExecutionResult result() {
    this.requireFinished();
    return this.failedPlayers == 0 ? QuoteExecutionResult.SUCCESS : QuoteExecutionResult.FAILED;
  }

  /** Devuelve un motivo cerrado solo cuando hubo fallos individuales. */
  public QuoteExecutionFailureReason failureReason() {
    this.requireFinished();
    return this.failedPlayers == 0 ? null : QuoteExecutionFailureReason.INDIVIDUAL_PLAYER_FAILURES;
  }

  /** Completa una sola vez los resultados disjuntos de una corrida en curso. */
  public void complete(
      int pricedPlayers,
      int recomputedRows,
      int carriedForwardRows,
      int notRepricedPlayers,
      int failedPlayers,
      Instant finishedAt) {
    if (this.finishedAt != null
        || pricedPlayers
            != (long) recomputedRows + carriedForwardRows + notRepricedPlayers + failedPlayers
        || pricedPlayers < 0
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

  private void requireFinished() {
    if (this.finishedAt == null) {
      throw new InvalidQuoteException("La ejecución todavía no tiene un resumen finalizado");
    }
  }
}
