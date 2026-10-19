package footballmarket.repositories;

import static org.assertj.core.api.Assertions.*;

import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class PlayerStatisticsMigrationTest {
  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  private Connection connection(String schema) throws SQLException {
    Flyway.configure()
        .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
        .schemas(schema)
        .defaultSchema(schema)
        .load()
        .migrate();
    Connection connection =
        DriverManager.getConnection(
            DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
    try (Statement statement = connection.createStatement()) {
      statement.execute("SET search_path TO " + schema);
      statement.execute("INSERT INTO leagues(id,name) VALUES(100,'League')");
      statement.execute("INSERT INTO teams(id,name,league_id,current) VALUES(100,'Team',100,true)");
      statement.execute(
          "INSERT INTO players(id,name,team_id,position,active) VALUES(100,'Player',100,'Forward',true)");
      statement.execute(
          """
          INSERT INTO player_statistics_extraction_runs(id,extraction_date,source,started_at,status)
          VALUES(100,'2026-10-15','SCRAPING',now(),'COMPLETED'),
                (101,'2026-10-15','SCRAPING',now(),'COMPLETED'),
                (102,'2026-10-16','SCRAPING',now(),'COMPLETED'),
                (103,'2026-10-15','JSON',now(),'COMPLETED')
          """);
    }
    return connection;
  }

  private void insertItem(Statement statement, int runId, String date, String source, String result)
      throws SQLException {
    try (PreparedStatement insert =
        statement
            .getConnection()
            .prepareStatement(
                """
        INSERT INTO player_statistics_run_items(run_id,extraction_date,source,player_id,provider_player_id,result,started_at,finished_at)
        VALUES(?,?::date,?,100,'123',?,now(),now())
        """)) {
      insert.setInt(1, runId);
      insert.setString(2, date);
      insert.setString(3, source);
      insert.setString(4, result);
      insert.executeUpdate();
    }
  }

  @Nested
  @DisplayName("Restricciones reales de campaña y precisión decimal")
  class Migration {
    @Test
    @DisplayName("La colisión al confirmar revierte todos los datos nuevos de la transacción")
    void rollsBackPartialConfirmation() throws Exception {
      // Arrange
      try (Connection connection = connection("statistics_rollback");
          Statement statement = connection.createStatement()) {
        insertItem(statement, 100, "2026-10-15", "SCRAPING", "COMPLETED");
        connection.setAutoCommit(false);
        insertItem(statement, 101, "2026-10-15", "SCRAPING", "PROVIDER_ERROR");
        statement.execute(
            """
            INSERT INTO player_statistics(
                player_id,extraction_date,source,context_status,run_item_id,observed_at,whoscored_rating_average)
            SELECT 100,'2026-10-15','SCRAPING','RESOLVED',id,now(),6.9033333333333333
            FROM player_statistics_run_items WHERE run_id=101
            """);
        // Act / Assert
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "UPDATE player_statistics_run_items SET result='COMPLETED' WHERE run_id=101"))
            .isInstanceOfSatisfying(
                SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23505"));
        connection.rollback();
        // Verify
        try (ResultSet rows =
            statement.executeQuery(
                """
            SELECT (SELECT count(*) FROM player_statistics_run_items),
                   (SELECT count(*) FROM player_statistics)
            """)) {
          rows.next();
          assertThat(rows.getLong(1)).isEqualTo(1);
          assertThat(rows.getLong(2)).isZero();
        }
      }
    }

    @Test
    @DisplayName(
        "Impide otra confirmación del mismo día y canal, permite fallos y campañas diferentes")
    void enforcesCampaignUniqueness() throws Exception {
      // Arrange
      try (Connection connection = connection("statistics_unique");
          Statement statement = connection.createStatement()) {
        insertItem(statement, 100, "2026-10-15", "SCRAPING", "COMPLETED");
        // Act / Assert
        assertThatThrownBy(() -> insertItem(statement, 101, "2026-10-15", "SCRAPING", "COMPLETED"))
            .isInstanceOfSatisfying(
                SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23505"));
        // Verify
        insertItem(statement, 101, "2026-10-15", "SCRAPING", "PROVIDER_ERROR");
        insertItem(statement, 101, "2026-10-15", "SCRAPING", "PROVIDER_ERROR");
        insertItem(statement, 102, "2026-10-16", "SCRAPING", "COMPLETED");
        insertItem(statement, 103, "2026-10-15", "JSON", "COMPLETED");
        try (ResultSet rows =
            statement.executeQuery("SELECT count(*) FROM player_statistics_run_items")) {
          rows.next();
          assertThat(rows.getLong(1)).isEqualTo(5);
        }
      }
    }

    @Test
    @DisplayName("La FK compuesta rechaza fecha o canal distintos de la ejecución")
    void enforcesCompositeForeignKey() throws Exception {
      // Arrange
      try (Connection connection = connection("statistics_fk");
          Statement statement = connection.createStatement()) {
        // Act / Assert
        assertThatThrownBy(() -> insertItem(statement, 100, "2026-10-16", "SCRAPING", "COMPLETED"))
            .isInstanceOfSatisfying(
                SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23503"));
        assertThatThrownBy(() -> insertItem(statement, 100, "2026-10-15", "JSON", "COMPLETED"))
            .isInstanceOfSatisfying(
                SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("23503"));
      }
    }

    @Test
    @DisplayName("Las sesenta métricas son NUMERIC sin precisión, escala ni default limitantes")
    void usesUnboundedNumeric() throws Exception {
      // Arrange
      try (Connection connection = connection("statistics_decimal");
          Statement statement = connection.createStatement()) {
        // Act
        try (ResultSet rows =
            statement.executeQuery(
                """
            SELECT column_name,data_type,numeric_precision,numeric_scale,column_default
            FROM information_schema.columns WHERE table_schema='statistics_decimal'
            AND table_name='player_statistics' AND data_type='numeric'
            AND column_name NOT IN ('present_metrics','absent_metrics')
            ORDER BY column_name
            """)) {
          // Assert
          int count = 0;
          while (rows.next()) {
            assertThat(rows.getString("data_type")).isEqualTo("numeric");
            assertThat(rows.getObject("numeric_precision")).isNull();
            assertThat(rows.getObject("numeric_scale")).isNull();
            assertThat(rows.getObject("column_default")).isNull();
            count++;
          }
          assertThat(count).isEqualTo(60);
        }
      }
    }
  }
}
