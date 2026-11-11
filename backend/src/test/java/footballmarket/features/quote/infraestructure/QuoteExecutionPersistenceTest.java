package footballmarket.features.quote.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import footballmarket.support.TestcontainersConfiguration;
import footballmarket.support.ValuationDatabaseSupport;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@org.junit.jupiter.api.Tag("integration")
class QuoteExecutionPersistenceTest {
  @Autowired private PostgreSQLContainer database;
  @Autowired private QuoteExecutionRepository executions;

  @Nested
  @DisplayName("Persistencia de múltiples ejecuciones diarias")
  class ExecutionUniqueness {

    @Test
    @Transactional
    @DisplayName("La consulta real excluye contextos sin cierre y desempata por identidad")
    void queriesOnlyFinishedInTotalOrder() {
      // Arrange
      LocalDate day = LocalDate.of(2026, 11, 10);
      Instant start = Instant.parse("2026-11-10T09:00:00Z");
      QuoteExecution first = context(day, start);
      QuoteExecution second = context(day, start);
      QuoteExecution pending = context(day, start.plusSeconds(1));
      first.complete(0, 0, 0, 0, 0, start.plusSeconds(2));
      second.complete(0, 0, 0, 0, 0, start.plusSeconds(2));
      executions.saveAndFlush(first);
      executions.saveAndFlush(second);
      executions.saveAndFlush(pending);
      // Act
      List<QuoteExecution> finished =
          executions.findAllByQuoteDateAndFinishedAtIsNotNullOrderByStartedAtDescIdDesc(day);
      // Assert
      assertThat(finished)
          .extracting(QuoteExecution::getId)
          .containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("Exige un origen explícito sin valor por defecto y conserva el catálogo cerrado")
    void requiresExplicitValidOrigin() throws Exception {
      // Arrange
      try (Connection connection = ValuationDatabaseSupport.migrated(database, "execution_origin");
          Statement statement = connection.createStatement()) {
        String columns =
            "quote_date, strategy_name, strategy_version, adjustment_factor, variation_cap,"
                + " min_price_floor, priced_players, recomputed_rows, carried_forward_rows,"
                + " not_repriced_players, failed_players, started_at, finished_at";
        String values =
            "DATE '2026-10-21', 'POSITIONAL_PERFORMANCE', 'v1', 0.10, 0.10, 1,"
                + " 0, 0, 0, 0, 0, now(), now()";
        // Act / Assert
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO quote_executions (" + columns + ") VALUES (" + values + ")"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23502"));
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO quote_executions ("
                            + columns
                            + ", origin) VALUES ("
                            + values
                            + ", NULL)"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23502"));
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO quote_executions ("
                            + columns
                            + ", origin) VALUES ("
                            + values
                            + ", 'OTHER')"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> {
                  assertThat(((SQLException) error).getSQLState()).isEqualTo("23514");
                  assertThat(
                          ((org.postgresql.util.PSQLException) error)
                              .getServerErrorMessage()
                              .getConstraint())
                      .isEqualTo("ck_quote_execution_origin");
                });
        statement.execute(
            "INSERT INTO quote_executions ("
                + columns
                + ", origin) VALUES ("
                + values
                + ", 'AUTOMATIC')");
        // Verify
        assertThat(countExecutions(statement)).isEqualTo(1L);
      }
    }

    @Test
    @DisplayName("Rechaza una segunda ejecución automática para la misma fecha")
    void rejectsDuplicateDate() throws Exception {
      try (Connection connection = ValuationDatabaseSupport.migrated(database, "execution_unique");
          Statement statement = connection.createStatement()) {
        insertExecution(statement, "2026-10-21", "0.10");

        assertThatThrownBy(() -> insertExecution(statement, "2026-10-21", "0.10"))
            .isInstanceOf(SQLException.class);
        assertThat(countExecutions(statement)).isEqualTo(1L);
      }
    }

    @Test
    @DisplayName("Admite varias manuales antes y después de una automática del mismo día")
    void allowsRepeatedManualExecutions() throws Exception {
      // Arrange
      try (Connection connection = ValuationDatabaseSupport.migrated(database, "manual_repeated");
          Statement statement = connection.createStatement()) {
        // Act
        insertExecution(statement, "2026-10-21", "0.10", "MANUAL");
        insertExecution(statement, "2026-10-21", "0.10", "MANUAL");
        insertExecution(statement, "2026-10-21", "0.10");
        insertExecution(statement, "2026-10-21", "0.10", "MANUAL");
        // Assert
        assertThat(countExecutions(statement)).isEqualTo(4L);
        assertThatThrownBy(() -> insertExecution(statement, "2026-10-21", "0.10"))
            .isInstanceOf(SQLException.class);
        assertThat(countExecutions(statement)).isEqualTo(4L);
      }
    }

    @Test
    @DisplayName("Una ejecución rechazada por parámetros no deja resumen")
    void rejectedExecutionLeavesNoRow() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(database, "execution_rejected");
          Statement statement = connection.createStatement()) {
        assertThatThrownBy(() -> insertExecution(statement, "2026-10-21", "2.00"))
            .isInstanceOf(SQLException.class);
        assertThat(countExecutions(statement)).isZero();
      }
    }

    @Test
    @DisplayName("Dos inserciones automáticas concurrentes dejan una sola ejecución")
    void concurrentExecutionsLeaveOneRow() throws Exception {
      String schema = "execution_concurrent";
      try (Connection ignored = ValuationDatabaseSupport.migrated(database, schema)) {
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

  private boolean tryInsert(String schema) throws Exception {
    try (Connection connection = connection(schema);
        Statement statement = connection.createStatement()) {
      insertExecution(statement, "2026-10-21", "0.10");
      return true;
    } catch (SQLException exception) {
      return false;
    }
  }

  private Connection connection(String schema) throws SQLException {
    Connection connection =
        java.sql.DriverManager.getConnection(
            ValuationDatabaseSupport.databaseUrl(database, schema),
            database.getUsername(),
            database.getPassword());
    return connection;
  }

  private static void insertExecution(Statement statement, String date, String factor)
      throws SQLException {
    insertExecution(statement, date, factor, "AUTOMATIC");
  }

  private static void insertExecution(
      Statement statement, String date, String factor, String origin) throws SQLException {
    statement.execute(
        "INSERT INTO quote_executions (quote_date, strategy_name, strategy_version,"
            + " adjustment_factor, variation_cap, min_price_floor, priced_players,"
            + " recomputed_rows, carried_forward_rows, not_repriced_players, failed_players,"
            + " origin, started_at, finished_at) VALUES (DATE '"
            + date
            + "', 'POSITIONAL_PERFORMANCE', 'v1', "
            + factor
            + ", 0.10, 1, 0, 0, 0, 0, 0, '"
            + origin
            + "', now(), now())");
  }

  private static long countExecutions(Statement statement) throws SQLException {
    try (ResultSet result = statement.executeQuery("SELECT count(*) FROM quote_executions")) {
      result.next();
      return result.getLong(1);
    }
  }

  private QuoteExecution context(LocalDate day, Instant startedAt) {
    return new QuoteExecution(
        day,
        QuoteExecutionOrigin.MANUAL,
        ValuationStrategy.POSITIONAL_PERFORMANCE,
        "v1",
        new BigDecimal("0.10"),
        new BigDecimal("0.10"),
        1L,
        0,
        0,
        0,
        0,
        0,
        startedAt,
        null);
  }
}
