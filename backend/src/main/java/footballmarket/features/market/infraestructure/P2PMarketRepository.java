package footballmarket.features.market.infraestructure;

import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.infraestructure.projections.P2PMarketPlayerProjection;
import footballmarket.features.market.infraestructure.projections.P2PMarketPriceRangeProjection;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de infraestructura para la consulta del catálogo del Mercado P2P con publicaciones
 * activas agrupadas por jugador.
 */
@Repository
public interface P2PMarketRepository extends JpaRepository<TokenListing, Long> {

  /**
   * Calcula el rango global de precios mínimos y máximos sobre todas las publicaciones P2P activas
   * y vigentes.
   */
  @Query(
      value =
          """
          SELECT MIN(tl.unit_price) AS min, MAX(tl.unit_price) AS max
          FROM token_listings tl
          JOIN players p ON p.id = tl.player_id
          JOIN teams t ON t.id = p.team_id
          WHERE tl.status = 'ACTIVE'
            AND tl.expires_at > :now
            AND (:viewerId IS NULL OR tl.seller_user_id <> :viewerId)
            AND p.active = true
            AND t.current = true
            AND EXISTS (
                SELECT 1 FROM player_statistics s
                WHERE s.player_id = p.id
                  AND s.present_metrics > 0
                  AND s.extraction_date = (
                      SELECT MAX(latest.extraction_date)
                      FROM player_statistics latest
                      WHERE latest.player_id = p.id
                  )
            )
          """,
      nativeQuery = true)
  P2PMarketPriceRangeProjection findGlobalP2PMarketPriceRange(
      @Param("viewerId") Long viewerId, @Param("now") OffsetDateTime now);

  /**
   * Obtiene la lista paginada de jugadores del Mercado P2P con datos agregados de publicaciones
   * activas.
   */
  @Query(
      value =
          """
          SELECT
              p.id AS id,
              p.name AS name,
              p.position AS position,
              p.nationality AS nationality,
              t.id AS "teamId",
              t.name AS "teamName",
              CAST(NULL AS text) AS "teamBadgeUrl",
              l.id AS "leagueId",
              l.name AS "leagueName",
              p.image_url AS "imageUrl",
              COUNT(tl.id) AS "listingCount",
              SUM(tl.quantity) AS "availableQuantity",
              MIN(tl.unit_price) AS "minimumUnitPrice"
          FROM token_listings tl
          JOIN players p ON p.id = tl.player_id
          JOIN teams t ON t.id = p.team_id
          JOIN leagues l ON l.id = t.league_id
          WHERE tl.status = 'ACTIVE'
            AND tl.expires_at > :now
            AND (:viewerId IS NULL OR tl.seller_user_id <> :viewerId)
            AND p.active = true
            AND t.current = true
            AND EXISTS (
                SELECT 1 FROM player_statistics s
                WHERE s.player_id = p.id
                  AND s.present_metrics > 0
                  AND s.extraction_date = (
                      SELECT MAX(latest.extraction_date)
                      FROM player_statistics latest
                      WHERE latest.player_id = p.id
                  )
            )
            AND (:leagueId IS NULL OR l.id = :leagueId)
            AND (:teamId IS NULL OR t.id = :teamId)
            AND (:position IS NULL OR p.position = :position)
            AND (CAST(:search AS text) IS NULL OR catalog_normalize_name(p.name) LIKE concat('%', catalog_normalize_name(CAST(:search AS text)), '%') OR catalog_normalize_name(t.name) LIKE concat('%', catalog_normalize_name(CAST(:search AS text)), '%'))
          GROUP BY p.id, p.name, p.position, p.nationality, t.id, t.name, l.id, l.name, p.image_url
          HAVING (:minPrice IS NULL OR MIN(tl.unit_price) >= :minPrice)
             AND (:maxPrice IS NULL OR MIN(tl.unit_price) <= :maxPrice)
          ORDER BY
            CASE WHEN :sortProperty = 'price' AND :sortDirection = 'asc' THEN MIN(tl.unit_price) END ASC NULLS LAST,
            CASE WHEN :sortProperty = 'price' AND :sortDirection = 'desc' THEN MIN(tl.unit_price) END DESC NULLS LAST,
            CASE WHEN :sortProperty = 'quantity' AND :sortDirection = 'asc' THEN SUM(tl.quantity) END ASC NULLS LAST,
            CASE WHEN :sortProperty = 'quantity' AND :sortDirection = 'desc' THEN SUM(tl.quantity) END DESC NULLS LAST,
            CASE WHEN :sortProperty = 'name' AND :sortDirection = 'asc' THEN catalog_normalize_name(p.name) END ASC,
            CASE WHEN :sortProperty = 'name' AND :sortDirection = 'desc' THEN catalog_normalize_name(p.name) END DESC,
            p.id ASC
          LIMIT :limit OFFSET :offset
          """,
      nativeQuery = true)
  List<P2PMarketPlayerProjection> findP2PMarketPlayers(
      @Param("viewerId") Long viewerId,
      @Param("now") OffsetDateTime now,
      @Param("search") String search,
      @Param("leagueId") Long leagueId,
      @Param("teamId") Long teamId,
      @Param("position") String position,
      @Param("minPrice") Long minPrice,
      @Param("maxPrice") Long maxPrice,
      @Param("sortProperty") String sortProperty,
      @Param("sortDirection") String sortDirection,
      @Param("limit") int limit,
      @Param("offset") int offset);

  /** Conteo total de jugadores únicos que cumplen los criterios de filtrado en el Mercado P2P. */
  @Query(
      value =
          """
          SELECT COUNT(*) FROM (
            SELECT p.id
            FROM token_listings tl
            JOIN players p ON p.id = tl.player_id
            JOIN teams t ON t.id = p.team_id
            JOIN leagues l ON l.id = t.league_id
            WHERE tl.status = 'ACTIVE'
              AND tl.expires_at > :now
              AND (:viewerId IS NULL OR tl.seller_user_id <> :viewerId)
              AND p.active = true
              AND t.current = true
              AND EXISTS (
                  SELECT 1 FROM player_statistics s
                  WHERE s.player_id = p.id
                    AND s.present_metrics > 0
                    AND s.extraction_date = (
                        SELECT MAX(latest.extraction_date)
                        FROM player_statistics latest
                        WHERE latest.player_id = p.id
                    )
              )
              AND (:leagueId IS NULL OR l.id = :leagueId)
              AND (:teamId IS NULL OR t.id = :teamId)
              AND (:position IS NULL OR p.position = :position)
              AND (CAST(:search AS text) IS NULL OR catalog_normalize_name(p.name) LIKE concat('%', catalog_normalize_name(CAST(:search AS text)), '%') OR catalog_normalize_name(t.name) LIKE concat('%', catalog_normalize_name(CAST(:search AS text)), '%'))
            GROUP BY p.id
            HAVING (:minPrice IS NULL OR MIN(tl.unit_price) >= :minPrice)
               AND (:maxPrice IS NULL OR MIN(tl.unit_price) <= :maxPrice)
          ) sub
          """,
      nativeQuery = true)
  long countP2PMarketPlayers(
      @Param("viewerId") Long viewerId,
      @Param("now") OffsetDateTime now,
      @Param("search") String search,
      @Param("leagueId") Long leagueId,
      @Param("teamId") Long teamId,
      @Param("position") String position,
      @Param("minPrice") Long minPrice,
      @Param("maxPrice") Long maxPrice);
}
