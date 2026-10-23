package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@org.junit.jupiter.api.Tag("integration")
class QuoteExecutionPersistenceTest {
  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  @Nested
  @DisplayName("Persistencia única de ejecuciones")
  class ExecutionUniqueness {

    @Test
    @DisplayName("Rechaza una segunda ejecución para la misma fecha")
    void rejectsDuplicateDate() throws Exception {
      try (Connection connection = ValuationDatabaseSupport.migrated(DATABASE, "execution_unique");
          Statement statement = connection.createStatement()) {
        insertExecution(statement, "2026-10-21", "0.10");

        assertThatThrownBy(() -> insertExecution(statement, "2026-10-21", "0.10"))
            .isInstanceOf(SQLException.class);
        assertThat(countExecutions(statement)).isEqualTo(1L);
      }
    }

    @Test
    @DisplayName("Una ejecución rechazada por parámetros no deja resumen")
    void rejectedExecutionLeavesNoRow() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(DATABASE, "execution_rejected");
          Statement statement = connection.createStatement()) {
        assertThatThrownBy(() -> insertExecution(statement, "2026-10-21", "2.00"))
            .isInstanceOf(SQLException.class);
        assertThat(countExecutions(statement)).isZero();
      }
    }

    @Test
    @DisplayName("Dos inserciones concurrentes dejan una sola ejecución")
    void concurrentExecutionsLeaveOneRow() throws Exception {
      String schema = "execution_concurrent";
      try (Connection ignored = ValuationDatabaseSupport.migrated(DATABASE, schema)) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
          Callable<Boolean> insertion = () -> tryInsert(schema);
          List<Future<Boolean>> results = executor.invokeAll(List.of(insertion, insertion));

          assertThat(results)
              .extracting(result -> result.get())
              .containsExactlyInAnyOrder(true, false);
          try (Connection verification = connection(schema);
              Statement statement = verification.createStatement()) {
            assertThat(countExecutions(statement)).isEqualTo(1L);
          }
        } finally {
          executor.shutdownNow();
        }
      }
    }
  }

  private static boolean tryInsert(String schema) throws Exception {
    try (Connection connection = connection(schema);
        Statement statement = connection.createStatement()) {
      insertExecution(statement, "2026-10-21", "0.10");
      return true;
    } catch (SQLException exception) {
      return false;
    }
  }

  private static Connection connection(String schema) throws SQLException {
    Connection connection =
        java.sql.DriverManager.getConnection(
            DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
    try (Statement statement = connection.createStatement()) {
      statement.execute("SET search_path TO " + schema);
    }
    return connection;
  }

  private static void insertExecution(Statement statement, String date, String factor)
      throws SQLException {
    statement.execute(
        "INSERT INTO quote_executions (quote_date, strategy_name, strategy_version,"
            + " adjustment_factor, variation_cap, min_price_floor, priced_players,"
            + " recomputed_rows, carried_forward_rows, not_repriced_players, failed_players,"
            + " started_at, finished_at) VALUES (DATE '"
            + date
            + "', 'POSITIONAL_PERFORMANCE', 'v1', "
            + factor
            + ", 0.10, 1, 0, 0, 0, 0, 0, now(), now())");
  }

  private static long countExecutions(Statement statement) throws SQLException {
    try (ResultSet result = statement.executeQuery("SELECT count(*) FROM quote_executions")) {
      result.next();
      return result.getLong(1);
    }
  }
}
