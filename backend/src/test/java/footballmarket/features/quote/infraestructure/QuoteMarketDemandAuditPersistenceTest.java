package footballmarket.features.quote.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.support.ValuationDatabaseSupport;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@org.junit.jupiter.api.Tag("integration")
class QuoteMarketDemandAuditPersistenceTest {
  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  @Nested
  @DisplayName("Compatibilidad del esquema histórico")
  class HistoricalSchema {
    @Test
    @DisplayName("Mantiene nula la auditoría no reconstruible y clasifica el recálculo previo")
    void preservesHistoricalNullAuditFields() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(DATABASE, "market_audit_historical");
          Statement statement = connection.createStatement()) {
        insertLegacyExecution(statement);
        statement.execute(
            "INSERT INTO player_quotes (player_id, effective_date, price, previous_price, multiplier,"
                + " score, strategy_name, strategy_version, source_valuation_date, origin,"
                + " calculated_at, execution_id) VALUES (900, DATE '2026-11-23', 105, 100, 1.05,"
                + " 0.60, 'POSITIONAL_PERFORMANCE', 'v1', DATE '2026-11-22', 'RECOMPUTED',"
                + " TIMESTAMPTZ '2026-11-23 10:00:00+00', 1)");

        try (ResultSet execution =
            statement.executeQuery(
                "SELECT recalculation_type, demand_window, strategy_name, adjustment_factor"
                    + " FROM quote_executions WHERE id = 1")) {
          assertThat(execution.next()).isTrue();
          assertThat(execution.getString("recalculation_type")).isEqualTo("SPORT_AND_MARKET");
          assertThat(execution.getObject("demand_window")).isNull();
          assertThat(execution.getString("strategy_name")).isEqualTo("POSITIONAL_PERFORMANCE");
          assertThat(execution.getBigDecimal("adjustment_factor")).isNotNull();
        }
        try (ResultSet quote =
            statement.executeQuery(
                "SELECT sport_multiplier, market_demand_factor, market_multiplier,"
                    + " market_net_demand_tokens FROM player_quotes WHERE player_id = 900")) {
          assertThat(quote.next()).isTrue();
          assertThat(quote.getObject("sport_multiplier")).isNull();
          assertThat(quote.getObject("market_demand_factor")).isNull();
          assertThat(quote.getObject("market_multiplier")).isNull();
          assertThat(quote.getObject("market_net_demand_tokens")).isNull();
        }
      }
    }
  }

  @Nested
  @DisplayName("Restricciones de auditoría de mercado")
  class MarketOnlyAudit {
    @Test
    @DisplayName("Admite recálculo solo de mercado sin estrategia ni factor deportivo")
    void allowsMarketOnlyExecutionAndQuoteAudit() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(DATABASE, "market_audit_market_only");
          Statement statement = connection.createStatement()) {
        statement.execute(
            "INSERT INTO quote_executions (quote_date, strategy_name, strategy_version,"
                + " adjustment_factor, variation_cap, min_price_floor, priced_players,"
                + " recomputed_rows, carried_forward_rows, not_repriced_players, failed_players,"
                + " origin, started_at, finished_at, recalculation_type, demand_window)"
                + " VALUES (DATE '2026-11-23', NULL, NULL, NULL, 0.10, 1, 1, 1, 0, 0, 0,"
                + " 'MANUAL', TIMESTAMPTZ '2026-11-23 10:00:00+00',"
                + " TIMESTAMPTZ '2026-11-23 10:01:00+00', 'MARKET_DEMAND_ONLY',"
                + " INTERVAL '24 hours')");
        statement.execute(
            "INSERT INTO player_quotes (player_id, effective_date, price, previous_price,"
                + " multiplier, origin, calculated_at, execution_id, market_demand_factor,"
                + " market_multiplier, market_net_demand_tokens) VALUES (900, DATE '2026-11-23',"
                + " 105, 100, 1.05, 'MARKET_DEMAND', TIMESTAMPTZ '2026-11-23 10:01:00+00',"
                + " 1, 0.05, 1.05, 10)");

        try (ResultSet result =
            statement.executeQuery(
                "SELECT recalculation_type, demand_window, strategy_name, strategy_version,"
                    + " adjustment_factor FROM quote_executions WHERE id = 1")) {
          assertThat(result.next()).isTrue();
          assertThat(result.getString("recalculation_type")).isEqualTo("MARKET_DEMAND_ONLY");
          assertThat(result.getObject("demand_window")).isNotNull();
          assertThat(result.getObject("strategy_name")).isNull();
          assertThat(result.getObject("strategy_version")).isNull();
          assertThat(result.getObject("adjustment_factor")).isNull();
        }
      }
    }

    @Test
    @DisplayName("Rechaza ejecución solo de mercado con datos deportivos o sin ventana")
    void rejectsInvalidMarketOnlyExecutionParameters() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(DATABASE, "market_audit_invalid_execution");
          Statement statement = connection.createStatement()) {
        assertThatThrownBy(
                () ->
                    insertMarketOnlyExecution(
                        statement, "'POSITIONAL_PERFORMANCE'", "'v1'", "0.10", "NULL"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error ->
                    assertThat(
                            ((org.postgresql.util.PSQLException) error)
                                .getServerErrorMessage()
                                .getConstraint())
                        .isEqualTo("ck_quote_execution_strategy_by_type"));
        assertThatThrownBy(
                () -> insertMarketOnlyExecution(statement, "NULL", "NULL", "NULL", "NULL"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error ->
                    assertThat(
                            ((org.postgresql.util.PSQLException) error)
                                .getServerErrorMessage()
                                .getConstraint())
                        .isEqualTo("ck_quote_execution_strategy_by_type"));
      }
    }
  }

  private static void insertLegacyExecution(Statement statement) throws SQLException {
    statement.execute(
        "INSERT INTO quote_executions (quote_date, strategy_name, strategy_version,"
            + " adjustment_factor, variation_cap, min_price_floor, priced_players,"
            + " recomputed_rows, carried_forward_rows, not_repriced_players, failed_players,"
            + " origin, started_at, finished_at) VALUES (DATE '2026-11-23',"
            + " 'POSITIONAL_PERFORMANCE', 'v1', 0.10, 0.10, 1, 1, 1, 0, 0, 0, 'MANUAL',"
            + " TIMESTAMPTZ '2026-11-23 10:00:00+00', TIMESTAMPTZ '2026-11-23 10:01:00+00')");
  }

  private static void insertMarketOnlyExecution(
      Statement statement,
      String strategyName,
      String strategyVersion,
      String adjustmentFactor,
      String demandWindow)
      throws SQLException {
    statement.execute(
        "INSERT INTO quote_executions (quote_date, strategy_name, strategy_version,"
            + " adjustment_factor, variation_cap, min_price_floor, priced_players,"
            + " recomputed_rows, carried_forward_rows, not_repriced_players, failed_players,"
            + " origin, started_at, finished_at, recalculation_type, demand_window)"
            + " VALUES (DATE '2026-11-23', "
            + strategyName
            + ", "
            + strategyVersion
            + ", "
            + adjustmentFactor
            + ", 0.10, 1, 0, 0, 0, 0, 0, 'MANUAL', now(), now(),"
            + " 'MARKET_DEMAND_ONLY', "
            + demandWindow
            + ")");
  }
}
