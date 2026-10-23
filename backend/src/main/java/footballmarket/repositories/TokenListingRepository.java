package footballmarket.repositories;

import footballmarket.models.TokenListing;
import footballmarket.models.records.ListingIdentity;
import footballmarket.repositories.projections.ReservedQuantity;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Reservas derivadas, lecturas de publicaciones y bloqueo exclusivo para compra y cancelación. */
@Repository
public interface TokenListingRepository extends JpaRepository<TokenListing, Long> {
  /**
   * Solo orienta los bloqueos; no incorpora una entidad potencialmente obsoleta al contexto JPA.
   */
  @Query(
      "select new footballmarket.models.records.ListingIdentity(l.seller.id, l.player.id) from TokenListing l where l.id = :id")
  Optional<ListingIdentity> readIdentity(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from TokenListing l where l.id = :id")
  Optional<TokenListing> lockById(Long id);

  @Query(
      "select coalesce(sum(l.quantity), 0) from TokenListing l where l.seller.id = :sellerId"
          + " and l.player.id = :playerId and l.status = footballmarket.models.enums.TokenListingStatus.ACTIVE"
          + " and l.expiresAt > :now")
  long reservedQuantity(Long sellerId, Long playerId, OffsetDateTime now);

  /**
   * Cantidad reservada por jugador en las publicaciones del vendedor.
   *
   * <p>Una sola consulta de conjunto: el índice por vendedor cubre el filtro y la agrupación por
   * jugador entrega el total reservado de cada posición.
   *
   * @param sellerId vendedor cuyas reservas se consultan
   * @param now instante que decide la vigencia de las publicaciones
   * @return reservas del vendedor agrupadas por jugador
   */
  @Query(
      "select l.player.id as playerId, sum(l.quantity) as quantity from TokenListing l"
          + " where l.seller.id = :sellerId"
          + " and l.status = footballmarket.models.enums.TokenListingStatus.ACTIVE"
          + " and l.expiresAt > :now group by l.player.id")
  List<ReservedQuantity> reservedQuantitiesByPlayer(
      @Param("sellerId") Long sellerId, @Param("now") OffsetDateTime now);

  @Query(
      "select l from TokenListing l join l.player p join p.team t join t.league league"
          + " where l.seller.id <> :viewerId and p.active = true and t.current = true"
          + " and l.status = footballmarket.models.enums.TokenListingStatus.ACTIVE and l.expiresAt > :now")
  List<TokenListing> findMarket(Long viewerId, OffsetDateTime now);

  List<TokenListing> findBySellerId(Long sellerId);
}
