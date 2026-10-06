package footballmarket.repositories;

import static org.assertj.core.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class PlayerMigrationTest {
  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  @Nested
  @DisplayName("Migración del catálogo existente")
  class Migration {
    @Test
    @DisplayName("V3 reasigna IDs y conserva jugadores, estados y referencias del proveedor")
    void migraDatosSinPerderCorrespondencia() throws Exception {
      // Arrange: el esquema anterior se obtiene con Flyway, no con una copia manual del DDL.
      String schema = "legacy_catalog";
      Flyway.configure()
          .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
          .schemas(schema)
          .defaultSchema(schema)
          .target("2")
          .load()
          .migrate();
      try (Connection connection =
              DriverManager.getConnection(
                  DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
          Statement statement = connection.createStatement()) {
        statement.execute("SET search_path TO " + schema);
        statement.execute(
            "INSERT INTO players VALUES (44,'First','T','L','P',true),(900,'Second','T2','L2','P2',false)");

        // Act
        Flyway.configure()
            .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
            .schemas(schema)
            .defaultSchema(schema)
            .load()
            .migrate();

        // Assert
        try (ResultSet rows =
            statement.executeQuery(
                """
            SELECT p.id, p.name, p.active, r.external_id, r.provider, p.date_of_birth,
                   p.nationality, p.image_url
            FROM players p JOIN player_external_references r ON p.id=r.player_id ORDER BY p.name
            """)) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getLong("id")).isNotEqualTo(44);
          assertThat(rows.getString("name")).isEqualTo("First");
          assertThat(rows.getBoolean("active")).isTrue();
          assertThat(rows.getString("external_id")).isEqualTo("44");
          assertThat(rows.getString("provider")).isEqualTo("FOOTBALL_DATA");
          assertThat(rows.getObject("date_of_birth")).isNull();
          assertThat(rows.getString("nationality")).isNull();
          assertThat(rows.getString("image_url")).isNull();
          assertThat(rows.next()).isTrue();
          assertThat(rows.getLong("id")).isNotEqualTo(900);
          assertThat(rows.getString("name")).isEqualTo("Second");
          assertThat(rows.getBoolean("active")).isFalse();
          assertThat(rows.getString("external_id")).isEqualTo("900");
          assertThat(rows.next()).isFalse();
        }
        statement.execute(
            "INSERT INTO players(name,team,league,position,active) VALUES ('Next','T','L','P',true)");
        try (ResultSet rows =
            statement.executeQuery("SELECT count(*), count(DISTINCT id) FROM players")) {
          rows.next();
          assertThat(rows.getInt(1)).isEqualTo(3);
          assertThat(rows.getInt(2)).isEqualTo(3);
        }
        try (ResultSet rows =
            statement.executeQuery(
                """
            SELECT condeferrable FROM pg_constraint
            WHERE conrelid='player_external_references'::regclass
            AND conname='uk_player_external_references_provider_external_id'
            """)) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getBoolean(1)).isFalse();
        }
        try (ResultSet rows =
            statement.executeQuery(
                """
            SELECT count(*) FROM pg_indexes WHERE schemaname='legacy_catalog'
            AND indexname='idx_player_external_references_player_id'
            """)) {
          rows.next();
          assertThat(rows.getInt(1)).isEqualTo(1);
        }
        assertThatThrownBy(
                () ->
                    statement.execute(
                        """
            INSERT INTO player_external_references(player_id,provider,external_id)
            SELECT id,'FOOTBALL_DATA','44' FROM players WHERE name='Next'
            """))
            .isInstanceOf(java.sql.SQLException.class)
            .satisfies(
                error ->
                    assertThat(((java.sql.SQLException) error).getSQLState()).isEqualTo("23505"));
      }
    }

    @Test
    @DisplayName("Una base vacía migrada permite la primera alta con ambas secuencias")
    void migraBaseVacia() throws Exception {
      String schema = "empty_catalog";
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
            "INSERT INTO players(name,team,league,position,active) VALUES ('First','T','L','P',true)");
        statement.execute(
            "INSERT INTO player_external_references(player_id,provider,external_id) SELECT id,'FOOTBALL_DATA','44' FROM players");
        try (ResultSet rows =
            statement.executeQuery(
                "SELECT p.id,r.id FROM players p JOIN player_external_references r ON r.player_id=p.id")) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getLong(1)).isEqualTo(1);
          assertThat(rows.getLong(2)).isEqualTo(1);
          assertThat(rows.next()).isFalse();
        }
      }
    }
  }
}
