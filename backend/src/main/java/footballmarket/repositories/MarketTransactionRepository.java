package footballmarket.repositories;

import footballmarket.models.MarketTransaction;
import java.util.List;
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
}
