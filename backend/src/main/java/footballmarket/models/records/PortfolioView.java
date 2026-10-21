package footballmarket.models.records;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Vista de la consulta del portfolio de un usuario.
 *
 * <p>El resumen representa siempre el portfolio completo y es independiente del filtro y de la
 * página; las posiciones corresponden a la página solicitada y los metadatos describen esa
 * paginación.
 */
public record PortfolioView(
    PortfolioSummary summary, List<PortfolioPosition> positions, PortfolioPagination pagination) {

  /**
   * Resumen financiero global del portfolio.
   *
   * @param totalPositions cantidad global de posiciones abiertas
   * @param totalTokens tokens abiertos poseídos, en todas las posiciones
   * @param totalReservedTokens tokens reservados por publicaciones activas y vigentes
   * @param totalAvailableTokens tokens disponibles: poseídos menos reservados
   * @param totalInvested costo de adquisición de las posiciones abiertas
   * @param totalCurrentValue valor actual total; nulo si alguna posición abierta carece de
   *     cotización
   * @param totalRealizedProfitLoss resultado realizado acumulado, incluidos los ciclos cerrados
   * @param totalUnrealizedProfitLoss resultado no realizado total; nulo si falta alguna cotización
   * @param totalProfitLoss resultado total; nulo si falta alguna cotización
   * @param totalProfitLossPercentage resultado total sobre el costo histórico total pagado
   * @param totalBuyTransactions operaciones confirmadas en las que el usuario es comprador
   * @param totalSellTransactions operaciones confirmadas en las que el usuario es vendedor
   * @param valuationComplete ausencia de posiciones abiertas sin cotización
   */
  public record PortfolioSummary(
      int totalPositions,
      int totalTokens,
      int totalReservedTokens,
      int totalAvailableTokens,
      long totalInvested,
      Long totalCurrentValue,
      long totalRealizedProfitLoss,
      Long totalUnrealizedProfitLoss,
      Long totalProfitLoss,
      BigDecimal totalProfitLossPercentage,
      long totalBuyTransactions,
      long totalSellTransactions,
      boolean valuationComplete) {}

  /**
   * Posición del portfolio de un jugador.
   *
   * @param playerId identificador interno del jugador
   * @param playerName nombre del jugador; nulo si no es recuperable
   * @param teamName nombre del equipo; nulo si no es recuperable
   * @param position posición deportiva del jugador; nula si no es recuperable
   * @param imageUrl imagen persistida del jugador; nula si no existe
   * @param active estado activo del jugador en el catálogo
   * @param status estado de la posición: {@code OPEN} o {@code CLOSED}
   * @param valuationStatus estado de valoración: {@code VALUED}, {@code UNVALUED} o {@code
   *     NOT_APPLICABLE}
   * @param quantity tokens poseídos
   * @param reservedQuantity tokens reservados por publicaciones activas y vigentes
   * @param availableQuantity tokens disponibles: poseídos menos reservados
   * @param acquisitionCost costo de adquisición de la posición abierta
   * @param averageUnitPrice precio medio de adquisición; nulo en posición cerrada
   * @param currentUnitPrice cotización vigente por token; nula sin cotización o en posición cerrada
   * @param priceUpdatedAt instante de cálculo de la cotización; nulo con la cotización
   * @param currentValue valor actual de la posición; nulo sin cotización o en posición cerrada
   * @param realizedProfitLoss resultado realizado acumulado del jugador
   * @param unrealizedProfitLoss resultado no realizado; nulo sin cotización o en posición cerrada
   * @param totalProfitLoss resultado total; nulo sin cotización; en posición cerrada, el realizado
   *     acumulado
   * @param unrealizedProfitLossPercentage rendimiento no realizado; nulo sin cotización o con costo
   *     cero
   * @param portfolioWeightPercentage peso sobre el valor total del portfolio; nulo si no puede
   *     calcularse
   * @param openedAt inicio del ciclo de tenencia vigente
   * @param closedAt venta que llevó la tenencia a cero; nulo mientras la posición esté abierta
   * @param lastTransactionAt última operación confirmada del usuario sobre el jugador; nula si no
   *     tiene ninguna
   * @param totalBuyTransactions operaciones confirmadas en las que el usuario es comprador
   * @param totalSellTransactions operaciones confirmadas en las que el usuario es vendedor
   */
  public record PortfolioPosition(
      Long playerId,
      String playerName,
      String teamName,
      String position,
      String imageUrl,
      boolean active,
      String status,
      String valuationStatus,
      int quantity,
      int reservedQuantity,
      int availableQuantity,
      long acquisitionCost,
      Long averageUnitPrice,
      Long currentUnitPrice,
      OffsetDateTime priceUpdatedAt,
      Long currentValue,
      long realizedProfitLoss,
      Long unrealizedProfitLoss,
      Long totalProfitLoss,
      BigDecimal unrealizedProfitLossPercentage,
      BigDecimal portfolioWeightPercentage,
      OffsetDateTime openedAt,
      OffsetDateTime closedAt,
      OffsetDateTime lastTransactionAt,
      long totalBuyTransactions,
      long totalSellTransactions) {}

  /**
   * Metadatos de la página solicitada.
   *
   * @param page número de página desde cero
   * @param size tamaño de página solicitado
   * @param totalElements cantidad de posiciones del conjunto filtrado
   * @param totalPages cantidad de páginas del conjunto filtrado
   */
  public record PortfolioPagination(int page, int size, long totalElements, int totalPages) {}
}
