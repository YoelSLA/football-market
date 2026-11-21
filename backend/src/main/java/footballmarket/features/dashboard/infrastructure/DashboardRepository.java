package footballmarket.features.dashboard.infrastructure;

import footballmarket.features.dashboard.infrastructure.projections.FmiBenchmarkProjection;
import footballmarket.features.dashboard.infrastructure.projections.P2PDealProjection;
import footballmarket.features.dashboard.infrastructure.projections.PlayerMoverProjection;
import footballmarket.features.dashboard.infrastructure.projections.ValuePickProjection;
import footballmarket.features.quote.domain.models.PlayerQuote;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Consultas analíticas SQL nativas optimizadas para el Dashboard. */
@Repository
public interface DashboardRepository extends JpaRepository<PlayerQuote, Long> {

  @Query(
      value =
          """
          WITH latest_quotes AS (
            SELECT DISTINCT ON (q.player_id) q.*
            FROM player_quotes q
            ORDER BY q.player_id, q.effective_date DESC, q.calculated_at DESC, q.id DESC
          )
          SELECT q.player_id AS "playerId",
                 p.name AS "name",
                 t.name AS "teamName",
                 l.name AS "leagueName",
                 p.position AS "position",
                 p.image_url AS "imageUrl",
                 q.price AS "currentPrice",
                 q.previous_price AS "previousPrice",
                 (q.price - q.previous_price) AS "deltaCredits",
                 ROUND(((q.price - q.previous_price)::numeric / q.previous_price::numeric) * 100, 2) AS "deltaPercentage",
                 q.score AS "score"
          FROM latest_quotes q
          JOIN players p ON p.id = q.player_id
          LEFT JOIN teams t ON t.id = p.team_id
          LEFT JOIN leagues l ON l.id = t.league_id
          WHERE q.previous_price IS NOT NULL AND q.previous_price > 0
          ORDER BY "deltaPercentage" DESC, q.price DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<PlayerMoverProjection> findTopGainers(@Param("limit") int limit);

  @Query(
      value =
          """
          WITH latest_quotes AS (
            SELECT DISTINCT ON (q.player_id) q.*
            FROM player_quotes q
            ORDER BY q.player_id, q.effective_date DESC, q.calculated_at DESC, q.id DESC
          )
          SELECT q.player_id AS "playerId",
                 p.name AS "name",
                 t.name AS "teamName",
                 l.name AS "leagueName",
                 p.position AS "position",
                 p.image_url AS "imageUrl",
                 q.price AS "currentPrice",
                 q.previous_price AS "previousPrice",
                 (q.price - q.previous_price) AS "deltaCredits",
                 ROUND(((q.price - q.previous_price)::numeric / q.previous_price::numeric) * 100, 2) AS "deltaPercentage",
                 q.score AS "score"
          FROM latest_quotes q
          JOIN players p ON p.id = q.player_id
          LEFT JOIN teams t ON t.id = p.team_id
          LEFT JOIN leagues l ON l.id = t.league_id
          WHERE q.previous_price IS NOT NULL AND q.previous_price > 0
          ORDER BY "deltaPercentage" ASC, q.price ASC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<PlayerMoverProjection> findTopLosers(@Param("limit") int limit);

  @Query(
      value =
          """
          WITH latest_quotes AS (
            SELECT DISTINCT ON (q.player_id) q.*
            FROM player_quotes q
            ORDER BY q.player_id, q.effective_date DESC, q.calculated_at DESC, q.id DESC
          )
          SELECT q.player_id AS "playerId",
                 p.name AS "name",
                 t.name AS "teamName",
                 l.name AS "leagueName",
                 p.position AS "position",
                 p.image_url AS "imageUrl",
                 q.score AS "whoScoredScore",
                 q.price AS "currentPrice",
                 ROUND(((q.score / (q.price::numeric / 100.0)) * 1000)::numeric, 2) AS "valueRatio"
          FROM latest_quotes q
          JOIN players p ON p.id = q.player_id
          LEFT JOIN teams t ON t.id = p.team_id
          LEFT JOIN leagues l ON l.id = t.league_id
          WHERE q.score IS NOT NULL AND q.price > 0
          ORDER BY "valueRatio" DESC, q.score DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<ValuePickProjection> findValuePicks(@Param("limit") int limit);

  @Query(
      value =
          """
          WITH latest_quotes AS (
            SELECT DISTINCT ON (q.player_id) q.*
            FROM player_quotes q
            ORDER BY q.player_id, q.effective_date DESC, q.calculated_at DESC, q.id DESC
          )
          SELECT tl.id AS "listingId",
                 tl.player_id AS "playerId",
                 p.name AS "playerName",
                 t.name AS "teamName",
                 l.name AS "leagueName",
                 p.image_url AS "imageUrl",
                 tl.quantity AS "availableTokens",
                 tl.unit_price AS "p2pUnitPrice",
                 q.price AS "officialQuotePrice",
                 ROUND(((q.price - tl.unit_price)::numeric / q.price::numeric) * 100, 2) AS "discountPercentage"
          FROM token_listings tl
          JOIN players p ON p.id = tl.player_id
          LEFT JOIN teams t ON t.id = p.team_id
          LEFT JOIN leagues l ON l.id = t.league_id
          JOIN latest_quotes q ON q.player_id = tl.player_id
          WHERE tl.status = 'ACTIVE'
            AND tl.expires_at > :now
            AND tl.unit_price < q.price
          ORDER BY "discountPercentage" DESC, tl.created_at DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<P2PDealProjection> findBestP2PDeals(
      @Param("now") OffsetDateTime now, @Param("limit") int limit);

  @Query(
      value =
          """
          WITH latest_quotes AS (
            SELECT DISTINCT ON (q.player_id) q.price
            FROM player_quotes q
            ORDER BY q.player_id, q.effective_date DESC, q.calculated_at DESC, q.id DESC
          )
          SELECT COALESCE(SUM(price * 100), 0) FROM latest_quotes
          """,
      nativeQuery = true)
  long calculateTotalMarketCap();

  @Query(
      value =
          """
          SELECT COALESCE(SUM(total_amount), 0)
          FROM market_transactions
          WHERE executed_at >= :since
          """,
      nativeQuery = true)
  long calculateVolumeSince(@Param("since") OffsetDateTime since);

  @Query(value = "SELECT COUNT(*) FROM market_transactions", nativeQuery = true)
  long countTotalTransactions();

  @Query(
      value = "SELECT COUNT(*) FROM token_listings WHERE status = 'ACTIVE' AND expires_at > :now",
      nativeQuery = true)
  int countActiveListings(@Param("now") OffsetDateTime now);

  @Query(
      value =
          """
          SELECT effective_date AS "effectiveDate",
                 ROUND(AVG(price), 2) AS "averagePrice"
          FROM player_quotes
          GROUP BY effective_date
          ORDER BY effective_date ASC
          """,
      nativeQuery = true)
  List<FmiBenchmarkProjection> findHistoricalFmiBenchmark();

  @Query(
      value = "SELECT DISTINCT effective_date FROM player_quotes ORDER BY effective_date ASC",
      nativeQuery = true)
  List<LocalDate> findDistinctQuoteDates();
}
