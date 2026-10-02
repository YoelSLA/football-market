package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class PlayerImageMigrationTest {
  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  @Nested
  @DisplayName("Migración V3 a V4 de imágenes")
  class Migration {
    @Test
    @DisplayName("Retroalimenta PENDING para activos e inactivos sin borrar sus imágenes")
    void backfillsResolutions() throws Exception {
      String schema = "populated_image_migration";
      Flyway.configure()
          .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
          .schemas(schema)
          .defaultSchema(schema)
          .target("3")
          .load()
          .migrate();
      try (Connection connection =
              DriverManager.getConnection(
                  DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
          Statement statement = connection.createStatement()) {
        statement.execute("SET search_path TO " + schema);
        statement.execute(
            """
            INSERT INTO players(name,team,league,position,active,image_url) VALUES
            ('Active','T','L','P',true,'https://thesportsdb.com/old'),
            ('Inactive','T','L','P',false,null)
            """);
        Flyway.configure()
            .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
            .schemas(schema)
            .defaultSchema(schema)
            .load()
            .migrate();
        try (ResultSet result =
            statement.executeQuery(
                """
            SELECT p.name, p.image_url, p.fallback_image_url, r.status, r.last_attempt_at
            FROM players p JOIN player_image_resolutions r ON r.player_id=p.id ORDER BY p.name
            """)) {
          assertThat(result.next()).isTrue();
          assertThat(result.getString("name")).isEqualTo("Active");
          assertThat(result.getString("image_url")).isEqualTo("https://thesportsdb.com/old");
          assertThat(result.getString("fallback_image_url")).isNull();
          assertThat(result.getString("status")).isEqualTo("PENDING");
          assertThat(result.getObject("last_attempt_at")).isNull();
          assertThat(result.next()).isTrue();
          assertThat(result.getString("name")).isEqualTo("Inactive");
          assertThat(result.getString("status")).isEqualTo("PENDING");
          assertThat(result.next()).isFalse();
        }
        try (ResultSet indexes =
            statement.executeQuery(
                """
            SELECT count(*) FROM pg_indexes WHERE schemaname='populated_image_migration'
            AND indexname IN ('idx_player_image_resolutions_eligibility',
              'idx_player_image_sync_runs_history','idx_player_image_sync_run_items_run_order')
            """)) {
          assertThat(indexes.next()).isTrue();
          assertThat(indexes.getInt(1)).isEqualTo(3);
        }
      }
    }

    @Test
    @DisplayName("Una base vacía migra y mantiene la unicidad de items por run y jugador")
    void emptyDatabaseAndUniqueItems() throws Exception {
      String schema = "empty_image_migration";
      Flyway.configure()
          .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
          .schemas(schema)
          .defaultSchema(schema)
          .load()
          .migrate();
      try (Connection connection =
              DriverManager.getConnection(
                  DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
          Statement statement = connection.createStatement()) {
        statement.execute("SET search_path TO " + schema);
        statement.execute(
            "INSERT INTO players(name,team,league,position,active) "
                + "VALUES ('First','T','L','P',true)");
        statement.execute(
            "INSERT INTO player_image_resolutions(player_id,status) "
                + "SELECT id,'PENDING' FROM players");
        statement.execute(
            "INSERT INTO player_image_sync_runs(started_at,force,status) "
                + "VALUES (CURRENT_TIMESTAMP,false,'RUNNING')");
        statement.execute(
            "INSERT INTO player_image_sync_run_items(run_id,player_id,previous_state) "
                + "SELECT r.id,p.id,'PENDING' FROM player_image_sync_runs r CROSS JOIN players p");
        assertThatThrownBy(
                () ->
                    statement.execute(
                        """
            INSERT INTO player_image_sync_run_items(run_id,player_id,previous_state)
            SELECT r.id,p.id,'PENDING' FROM player_image_sync_runs r CROSS JOIN players p
            """))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23505"));
      }
    }
  }
}
