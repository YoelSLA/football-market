package footballmarket.features.quote.infraestructure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.support.ValuationDatabaseSupport;
import java.sql.Connection;
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
class PlayerQuoteMigrationTest {
  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  @Nested
  @DisplayName("Esquema de cotizaciones")
  class Migration {

    @Test
    @DisplayName("Las filas no iniciales exigen una ejecución existente y INITIAL no la admite")
    void requiresAuditReferenceByOrigin() throws Exception {
      // Arrange
      try (Connection connection = ValuationDatabaseSupport.migrated(DATABASE, "quote_audit_fk");
          Statement statement = connection.createStatement()) {
        // Act / Assert
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO player_quotes (player_id,effective_date,price,previous_price,multiplier,"
                            + "origin,calculated_at,execution_id) VALUES (900,DATE '2026-10-20',100,100,1,"
                            + "'CARRIED_FORWARD',now(),999)"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error ->
                    org.assertj.core.api.Assertions.assertThat(((SQLException) error).getSQLState())
                        .isEqualTo("23503"));
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO player_quotes (player_id,effective_date,price,previous_price,multiplier,"
                            + "origin,calculated_at) VALUES (900,DATE '2026-10-20',100,100,1,"
                            + "'CARRIED_FORWARD',now())"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error ->
                    org.assertj.core.api.Assertions.assertThat(
                            ((org.postgresql.util.PSQLException) error)
                                .getServerErrorMessage()
                                .getConstraint())
                        .isEqualTo("ck_player_quote_execution"));
        insertCarriedForward(statement, "2026-10-21");
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO player_quotes (player_id,effective_date,price,multiplier,origin,calculated_at,execution_id)"
                            + " VALUES (900,DATE '2026-10-20',100,1,'INITIAL',now(),1)"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error ->
                    org.assertj.core.api.Assertions.assertThat(
                            ((org.postgresql.util.PSQLException) error)
                                .getServerErrorMessage()
                                .getConstraint())
                        .isEqualTo("ck_player_quote_execution"));
      }
    }

    @Test
    @DisplayName("La unicidad diaria impide insertar otra fila, incluso con diferente origen")
    void allowsOneRowPerPlayerDateAndOrigin() throws Exception {
      try (Connection connection = ValuationDatabaseSupport.migrated(DATABASE, "quote_migration");
          Statement statement = connection.createStatement()) {
        assertThatCode(
                () -> {
                  insertInitial(statement, "2026-10-20");
                })
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> insertInitial(statement, "2026-10-20"))
            .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> insertCarriedForward(statement, "2026-10-20"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error ->
                    org.assertj.core.api.Assertions.assertThat(((SQLException) error).getSQLState())
                        .isEqualTo("23505"));
        assertThatCode(() -> insertCarriedForward(statement, "2026-10-21"))
            .doesNotThrowAnyException();
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO player_quotes (player_id,effective_date,price,previous_price,multiplier,"
                            + "score,strategy_name,strategy_version,source_valuation_date,origin,calculated_at,execution_id)"
                            + " VALUES (900,DATE '2026-10-21',105,100,1.05,0.6,'POSITIONAL_PERFORMANCE','v1',"
                            + "DATE '2026-10-20','RECOMPUTED',now(),1)"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error ->
                    org.assertj.core.api.Assertions.assertThat(((SQLException) error).getSQLState())
                        .isEqualTo("23505"));
      }
    }

    @Test
    @DisplayName("Rechaza un precio nulo, cero o negativo")
    void rejectsNonPositivePrice() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(DATABASE, "quote_migration_price");
          Statement statement = connection.createStatement()) {
        assertThatThrownBy(() -> insertInitial(statement, "2026-10-20", 0))
            .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> insertInitial(statement, "2026-10-21", -5))
            .isInstanceOf(SQLException.class);
      }
    }

    @Test
    @DisplayName("Rechaza una fila inicial con estrategia o score")
    void rejectsInitialRowWithStrategy() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(DATABASE, "quote_migration_initial");
          Statement statement = connection.createStatement()) {
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO player_quotes (player_id, effective_date, price, previous_price,"
                            + " multiplier, score, strategy_name, strategy_version,"
                            + " source_valuation_date, origin, calculated_at) VALUES"
                            + " (900, DATE '2026-10-20', 100, NULL, 1, NULL, 'POSITIONAL_PERFORMANCE',"
                            + " 'v1', NULL, 'INITIAL', now())"))
            .isInstanceOf(SQLException.class);
      }
    }

    @Test
    @DisplayName("Rechaza una fila recalculada sin score, estrategia o fecha de valuación")
    void rejectsRecomputedRowWithoutTraceability() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(DATABASE, "quote_migration_recomputed");
          Statement statement = connection.createStatement()) {
        insertCarriedForward(statement, "2026-10-19");
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO player_quotes (player_id, effective_date, price, previous_price,"
                            + " multiplier, score, strategy_name, strategy_version,"
                            + " source_valuation_date, origin, calculated_at, execution_id) VALUES"
                            + " (900, DATE '2026-10-20', 105, 100, 1.05, NULL, NULL, NULL, NULL,"
                            + " 'RECOMPUTED', now(), 1)"))
            .isInstanceOf(SQLException.class);
      }
    }

    @Test
    @DisplayName("Rechaza una fila arrastrada con score o fecha de valuación")
    void rejectsCarriedForwardRowWithScore() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(DATABASE, "quote_migration_carried");
          Statement statement = connection.createStatement()) {
        insertCarriedForward(statement, "2026-10-19");
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO player_quotes (player_id, effective_date, price, previous_price,"
                            + " multiplier, score, strategy_name, strategy_version,"
                            + " source_valuation_date, origin, calculated_at, execution_id) VALUES"
                            + " (900, DATE '2026-10-20', 105, 105, 1, 0.6, NULL, NULL,"
                            + " DATE '2026-10-20', 'CARRIED_FORWARD', now(), 1)"))
            .isInstanceOf(SQLException.class);
      }
    }
  }

  private static void insertInitial(Statement statement, String effectiveDate) throws SQLException {
    insertInitial(statement, effectiveDate, 100);
  }

  private static void insertInitial(Statement statement, String effectiveDate, long price)
      throws SQLException {
    statement.execute(
        "INSERT INTO player_quotes (player_id, effective_date, price, previous_price, multiplier,"
            + " score, strategy_name, strategy_version, source_valuation_date, origin, calculated_at)"
            + " VALUES (900, DATE '"
            + effectiveDate
            + "', "
            + price
            + ", NULL, 1, NULL, NULL, NULL, NULL, 'INITIAL', now())");
  }

  private static void insertCarriedForward(Statement statement, String effectiveDate)
      throws SQLException {
    statement.execute(
        "INSERT INTO quote_executions (id, quote_date, strategy_name, strategy_version, origin,"
            + " adjustment_factor, variation_cap, min_price_floor, priced_players, recomputed_rows,"
            + " carried_forward_rows, not_repriced_players, failed_players, started_at, finished_at)"
            + " VALUES (1, DATE '2026-10-21', 'POSITIONAL_PERFORMANCE', 'v1', 'MANUAL',"
            + " 0.10, 0.10, 1, 0, 0, 0, 0, 0, now(), NULL) ON CONFLICT (id) DO NOTHING");
    statement.execute(
        "INSERT INTO player_quotes (player_id, effective_date, price, previous_price, multiplier,"
            + " score, strategy_name, strategy_version, source_valuation_date, origin, calculated_at, execution_id)"
            + " VALUES (900, DATE '"
            + effectiveDate
            + "', 100, 100, 1, NULL, NULL, NULL, NULL, 'CARRIED_FORWARD', now(), 1)");
  }
}
