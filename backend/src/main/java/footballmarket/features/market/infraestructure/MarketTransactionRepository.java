package footballmarket.features.market.infraestructure;

import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.portfolio.domain.records.OperationHistoryAggregate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Acceso a la auditoría de compras.
 *
 * <p>Solo expone la persistencia de registros confirmados. La aplicación no ofrece operaciones de
 * modificación o eliminación sobre ellos, de modo que la auditoría es append-only.
 */
@Repository
public interface MarketTransactionRepository extends JpaRepository<MarketTransaction, Long> {

  /**
   * Lista los registros financieros de un jugador, del más reciente al más antiguo.
   *
   * @param playerId jugador cuyos registros se consultan
   * @return registros del jugador ordenados por fecha de ejecución descendente
   */
  List<MarketTransaction> findByPlayerIdOrderByExecutedAtDescIdDesc(Long playerId);

  /**
   * Carga el historial completo de una parte, sin lecturas por posición.
   *
   * <p>Recupera en la misma sentencia las relaciones que el plegado necesita —comprador, vendedor,
   * jugador y publicación—, porque son {@code EAGER} por el mapeo de JPA y resolverlas por separado
   * costaría una sentencia por operación. No arrastra el grafo de equipos y ligas del catálogo, que
   * es perezoso y el plegado no consulta. El número de sentencias no crece con la cantidad de
   * operaciones; la combinación «comprador o vendedor» puede seguir degradando el uso de los
   * índices, como registra R-08.
   *
   * @param userId usuario cuyas operaciones se recuperan como comprador o vendedor
   * @return operaciones de la parte ordenadas por instante de confirmación e identidad
   */
  @Query(
      "select t from MarketTransaction t join fetch t.buyer join fetch t.seller"
          + " join fetch t.player left join fetch t.listing"
          + " where t.buyer.id = :userId or t.seller.id = :userId"
          + " order by t.executedAt asc, t.id asc")
  List<MarketTransaction> findFinancialHistory(@Param("userId") Long userId);

  /** Carga una página filtrada con todas las relaciones to-one necesarias para la respuesta. */
  @EntityGraph(attributePaths = {"buyer", "seller", "player", "player.team", "player.team.league"})
  @Query(
      value =
          "select t from MarketTransaction t join t.player p"
              + " where (t.buyer.id = :userId or t.seller.id = :userId)"
              + " and (:type is null or (:type = 'BUY' and t.buyer.id = :userId)"
              + " or (:type = 'SELL' and t.seller.id = :userId))"
              + " and (:search is null or cast(function('catalog_normalize_name', cast(:search as string)) as string) = ''"
              + " or cast(function('catalog_normalize_name', p.name) as string) like"
              + " concat('%', replace(replace(replace(cast(function('catalog_normalize_name', cast(:search as string)) as string),"
              + " '!', '!!'), '%', '!%'), '_', '!_'), '%') escape '!')"
              + " and (:filterExecutedFrom = false or t.executedAt >= :executedFrom)"
              + " and (:filterExecutedBefore = false or t.executedAt < :executedBefore)",
      countQuery =
          "select count(t) from MarketTransaction t join t.player p"
              + " where (t.buyer.id = :userId or t.seller.id = :userId)"
              + " and (:type is null or (:type = 'BUY' and t.buyer.id = :userId)"
              + " or (:type = 'SELL' and t.seller.id = :userId))"
              + " and (:search is null or cast(function('catalog_normalize_name', cast(:search as string)) as string) = ''"
              + " or cast(function('catalog_normalize_name', p.name) as string) like"
              + " concat('%', replace(replace(replace(cast(function('catalog_normalize_name', cast(:search as string)) as string),"
              + " '!', '!!'), '%', '!%'), '_', '!_'), '%') escape '!')"
              + " and (:filterExecutedFrom = false or t.executedAt >= :executedFrom)"
              + " and (:filterExecutedBefore = false or t.executedAt < :executedBefore)")
  Page<MarketTransaction> findOperationHistory(
      @Param("userId") Long userId,
      @Param("search") String search,
      @Param("type") String type,
      @Param("filterExecutedFrom") boolean filterExecutedFrom,
      @Param("executedFrom") OffsetDateTime executedFrom,
      @Param("filterExecutedBefore") boolean filterExecutedBefore,
      @Param("executedBefore") OffsetDateTime executedBefore,
      Pageable pageable);

  /** Calcula indicadores sobre el mismo conjunto filtrado, con independencia de la página. */
  @Query(
      "select new footballmarket.features.portfolio.domain.records.OperationHistoryAggregate("
          + " count(t),"
          + " coalesce(sum(case when t.buyer.id = :userId then 1 else 0 end), 0),"
          + " coalesce(sum(case when t.seller.id = :userId then 1 else 0 end), 0),"
          + " coalesce(sum(case when t.buyer.id = :userId then t.quantity else 0 end), 0),"
          + " coalesce(sum(case when t.seller.id = :userId then t.quantity else 0 end), 0),"
          + " coalesce(sum(case when t.buyer.id = :userId then t.totalAmount else 0 end), 0),"
          + " coalesce(sum(case when t.seller.id = :userId then t.totalAmount else 0 end), 0))"
          + " from MarketTransaction t join t.player p"
          + " where (t.buyer.id = :userId or t.seller.id = :userId)"
          + " and (:type is null or (:type = 'BUY' and t.buyer.id = :userId)"
          + " or (:type = 'SELL' and t.seller.id = :userId))"
          + " and (:search is null or cast(function('catalog_normalize_name', cast(:search as string)) as string) = ''"
          + " or cast(function('catalog_normalize_name', p.name) as string) like"
          + " concat('%', replace(replace(replace(cast(function('catalog_normalize_name', cast(:search as string)) as string),"
          + " '!', '!!'), '%', '!%'), '_', '!_'), '%') escape '!')"
          + " and (:filterExecutedFrom = false or t.executedAt >= :executedFrom)"
          + " and (:filterExecutedBefore = false or t.executedAt < :executedBefore)")
  OperationHistoryAggregate aggregateOperationHistory(
      @Param("userId") Long userId,
      @Param("search") String search,
      @Param("type") String type,
      @Param("filterExecutedFrom") boolean filterExecutedFrom,
      @Param("executedFrom") OffsetDateTime executedFrom,
      @Param("filterExecutedBefore") boolean filterExecutedBefore,
      @Param("executedBefore") OffsetDateTime executedBefore);
}
