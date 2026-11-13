package footballmarket.features.market.infraestructure;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.infraestructure.projections.OfficialMarketPlayerProjection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de infraestructura para la consulta del catálogo del Mercado Oficial con cotizaciones
 * integradas.
 *
 * <p>Aplica una subconsulta SQL nativa {@code LEFT JOIN LATERAL} para seleccionar la cotización más
 * reciente de cada jugador activo preservando la precedencia canónica, sin crear vistas DDL
 * permanentes ni migraciones Flyway.
 */
@Repository
public interface OfficialMarketRepository extends JpaRepository<Player, Long> {

  /**
   * Obtiene la lista paginada de jugadores y cotizaciones integradas con filtros globales y
   * ordenamiento determinista.
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
              q.quote_id AS "quoteId",
              q.price AS price,
              q.previous_price AS "previousPrice",
              q.variation_percentage AS "variationPercentage",
              CAST(q.score AS double precision) AS score,
              CAST(q.multiplier AS double precision) AS multiplier,
              q.origin AS origin,
              q.effective_date AS "effectiveDate",
              q.calculated_at AS "calculatedAt"
          FROM players p
          JOIN teams t ON t.id = p.team_id
          JOIN leagues l ON l.id = t.league_id
          LEFT JOIN LATERAL (
              SELECT
                  pq.id AS quote_id,
                  pq.price,
                  pq.previous_price,
                  CASE
                      WHEN pq.previous_price IS NOT NULL AND pq.previous_price > 0
                      THEN ROUND(((pq.price - pq.previous_price)::numeric / pq.previous_price::numeric) * 100, 2)
                      ELSE NULL
                  END AS variation_percentage,
                  pq.score,
                  pq.multiplier,
                  pq.origin,
                  pq.effective_date,
                  pq.calculated_at
              FROM player_quotes pq
              WHERE pq.player_id = p.id
              ORDER BY
                  pq.effective_date DESC,
                  (CASE WHEN pq.origin = 'RECOMPUTED' THEN 0 ELSE 1 END),
                  pq.calculated_at DESC,
                  pq.id DESC
              LIMIT 1
          ) q ON true
          WHERE p.active = true AND t.current = true
            AND (:leagueId IS NULL OR l.id = :leagueId)
            AND (:teamId IS NULL OR t.id = :teamId)
            AND (:position IS NULL OR p.position = :position)
            AND (:nationality IS NULL OR p.nationality = :nationality)
            AND (CAST(:name AS text) IS NULL OR catalog_normalize_name(p.name) LIKE concat('%', catalog_normalize_name(CAST(:name AS text)), '%'))
            AND (:hasQuote IS NULL OR (:hasQuote = true AND q.quote_id IS NOT NULL) OR (:hasQuote = false AND q.quote_id IS NULL))
            AND (:minPrice IS NULL OR q.price >= :minPrice)
            AND (:maxPrice IS NULL OR q.price <= :maxPrice)
          ORDER BY
            CASE WHEN :sortProperty = 'price' AND :sortDirection = 'asc' THEN q.price END ASC NULLS LAST,
            CASE WHEN :sortProperty = 'price' AND :sortDirection = 'desc' THEN q.price END DESC NULLS LAST,
            CASE WHEN :sortProperty = 'variation' AND :sortDirection = 'asc' THEN q.variation_percentage END ASC NULLS LAST,
            CASE WHEN :sortProperty = 'variation' AND :sortDirection = 'desc' THEN q.variation_percentage END DESC NULLS LAST,
            CASE WHEN :sortProperty = 'name' AND :sortDirection = 'asc' THEN catalog_normalize_name(p.name) END ASC,
            CASE WHEN :sortProperty = 'name' AND :sortDirection = 'desc' THEN catalog_normalize_name(p.name) END DESC,
            CASE WHEN :sortProperty = 'id' AND :sortDirection = 'desc' THEN p.id END DESC,
            p.id ASC
          LIMIT :limit OFFSET :offset
          """,
      nativeQuery = true)
  List<OfficialMarketPlayerProjection> findOfficialMarketPlayers(
      @Param("name") String name,
      @Param("leagueId") Long leagueId,
      @Param("teamId") Long teamId,
      @Param("position") String position,
      @Param("nationality") String nationality,
      @Param("hasQuote") Boolean hasQuote,
      @Param("minPrice") Long minPrice,
      @Param("maxPrice") Long maxPrice,
      @Param("sortProperty") String sortProperty,
      @Param("sortDirection") String sortDirection,
      @Param("limit") int limit,
      @Param("offset") int offset);

  /**
   * Consulta desacoplada de conteo optimizada para búsquedas sin filtros financieros (evita el join
   * lateral con cotizaciones).
   */
  @Query(
      value =
          """
          SELECT COUNT(*)
          FROM players p
          JOIN teams t ON t.id = p.team_id
          JOIN leagues l ON l.id = t.league_id
          WHERE p.active = true AND t.current = true
            AND (:leagueId IS NULL OR l.id = :leagueId)
            AND (:teamId IS NULL OR t.id = :teamId)
            AND (:position IS NULL OR p.position = :position)
            AND (:nationality IS NULL OR p.nationality = :nationality)
            AND (CAST(:name AS text) IS NULL OR catalog_normalize_name(p.name) LIKE concat('%', catalog_normalize_name(CAST(:name AS text)), '%'))
          """,
      nativeQuery = true)
  long countOfficialMarketPlayersWithoutFinancialFilters(
      @Param("name") String name,
      @Param("leagueId") Long leagueId,
      @Param("teamId") Long teamId,
      @Param("position") String position,
      @Param("nationality") String nationality);

  /**
   * Consulta desacoplada de conteo que evalúa cotizaciones únicamente cuando se solicitan filtros
   * financieros.
   */
  @Query(
      value =
          """
          SELECT COUNT(*)
          FROM players p
          JOIN teams t ON t.id = p.team_id
          JOIN leagues l ON l.id = t.league_id
          LEFT JOIN LATERAL (
              SELECT
                  pq.id AS quote_id,
                  pq.price
              FROM player_quotes pq
              WHERE pq.player_id = p.id
              ORDER BY
                  pq.effective_date DESC,
                  (CASE WHEN pq.origin = 'RECOMPUTED' THEN 0 ELSE 1 END),
                  pq.calculated_at DESC,
                  pq.id DESC
              LIMIT 1
          ) q ON true
          WHERE p.active = true AND t.current = true
            AND (:leagueId IS NULL OR l.id = :leagueId)
            AND (:teamId IS NULL OR t.id = :teamId)
            AND (:position IS NULL OR p.position = :position)
            AND (:nationality IS NULL OR p.nationality = :nationality)
            AND (CAST(:name AS text) IS NULL OR catalog_normalize_name(p.name) LIKE concat('%', catalog_normalize_name(CAST(:name AS text)), '%'))
            AND (:hasQuote IS NULL OR (:hasQuote = true AND q.quote_id IS NOT NULL) OR (:hasQuote = false AND q.quote_id IS NULL))
            AND (:minPrice IS NULL OR q.price >= :minPrice)
            AND (:maxPrice IS NULL OR q.price <= :maxPrice)
          """,
      nativeQuery = true)
  long countOfficialMarketPlayersWithFinancialFilters(
      @Param("name") String name,
      @Param("leagueId") Long leagueId,
      @Param("teamId") Long teamId,
      @Param("position") String position,
      @Param("nationality") String nationality,
      @Param("hasQuote") Boolean hasQuote,
      @Param("minPrice") Long minPrice,
      @Param("maxPrice") Long maxPrice);
}
