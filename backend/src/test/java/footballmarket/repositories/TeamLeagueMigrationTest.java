package footballmarket.repositories;

import static org.assertj.core.api.Assertions.*;

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
class TeamLeagueMigrationTest {
  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  @Nested
  @DisplayName("Esquema aditivo y restricciones de Release 1")
  class ReleaseOne {
    @Test
    @DisplayName(
        "V5 exige evidencia, claves únicas, fechas coherentes y referencias válidas de intentos y asociaciones")
    void constrainCasesAndAttempts() throws Exception {
      // Arrange
      String schema = "v5_case_attempt_constraints";
      Flyway.configure()
          .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
          .schemas(schema)
          .defaultSchema(schema)
          .target("5")
          .load()
          .migrate();
      try (Connection connection =
              DriverManager.getConnection(
                  DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
          Statement statement = connection.createStatement()) {
        statement.execute("SET search_path TO " + schema);
        statement.execute("INSERT INTO leagues(name) VALUES ('League')");
        statement.execute("INSERT INTO teams(name,league_id,current) VALUES ('Team',1,true)");
        statement.execute(
            "INSERT INTO players(name,team,league,position,active,team_id) VALUES ('Player','Team','League','Forward',true,1)");
        statement.execute(
            "INSERT INTO pending_review_cases(category,cause_code,subject_type,subject_id,case_key,first_detected_at,last_detected_at,evidence) "
                + "VALUES ('LEGACY_TEAM_ASSOCIATION','NO_TEAM_MATCH','PLAYER',1,'stable-key','2026-10-01','2026-10-02','{\"originalTeamName\":\"Original\",\"candidates\":[]}'::jsonb)");
        statement.execute(
            "INSERT INTO team_resolution_attempts(team_id,last_call_at,last_call_name,technical_result) VALUES (1,'2026-10-01','Team','INVALID_RESPONSE')");

        // Act / Assert
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO pending_review_cases(category,cause_code,subject_type,case_key,first_detected_at,last_detected_at,evidence) "
                            + "SELECT category,cause_code,subject_type,case_key,first_detected_at,last_detected_at,evidence FROM pending_review_cases"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23505"));
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "UPDATE pending_review_cases SET last_detected_at='2026-09-01'"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23514"));
        assertThatThrownBy(() -> statement.execute("UPDATE pending_review_cases SET evidence=NULL"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23502"));
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "UPDATE pending_review_cases SET subject_provider='FOOTBALL_DATA',subject_external_id=NULL"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23514"));
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO team_resolution_attempts(team_id,last_call_at,last_call_name) VALUES (999,now(),'Missing')"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23503"));
        assertThatThrownBy(() -> statement.execute("DELETE FROM teams WHERE id=1"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23503"));

        // Verify
        try (ResultSet row =
            statement.executeQuery(
                "SELECT technical_result,last_valid_evaluation_at,last_valid_evaluation_name,valid_result,retry_not_before FROM team_resolution_attempts")) {
          assertThat(row.next()).isTrue();
          assertThat(row.getString(1)).isEqualTo("INVALID_RESPONSE");
          for (int column = 2; column <= 5; column++) {
            assertThat(row.getObject(column)).isNull();
          }
        }
        try (ResultSet row =
            statement.executeQuery(
                "SELECT evidence->>'originalTeamName',jsonb_array_length(evidence->'candidates') FROM pending_review_cases")) {
          assertThat(row.next()).isTrue();
          assertThat(row.getString(1)).isEqualTo("Original");
          assertThat(row.getInt(2)).isZero();
        }
      }
    }

    @Test
    @DisplayName(
        "V5 conserva IDs, actividad, referencias y textos del catálogo V4 sin crear equipos por texto")
    void preserveLegacyCatalog() throws Exception {
      // Arrange: preparación técnica de una migración, no un caso funcional de Service.
      String schema = "v5_legacy_preserved";
      Flyway.configure()
          .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
          .schemas(schema)
          .defaultSchema(schema)
          .target("4")
          .load()
          .migrate();
      try (Connection connection =
              DriverManager.getConnection(
                  DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
          Statement statement = connection.createStatement()) {
        statement.execute("SET search_path TO " + schema);
        statement.execute(
            "INSERT INTO players(name,team,league,position,active) VALUES ('Legacy','Texto original','Liga original','Forward',false)");
        statement.execute(
            "UPDATE players SET date_of_birth='1990-06-20',nationality='Spain',image_url='https://images.example/main',fallback_image_url='https://images.example/secondary'");
        statement.execute(
            "INSERT INTO player_external_references(player_id,provider,external_id) SELECT id,'FOOTBALL_DATA','player-source' FROM players");
        statement.execute(
            "INSERT INTO player_external_references(player_id,provider,external_id) SELECT id,'FOOTBALL_DATA','historical-player-source' FROM players");
        statement.execute(
            "INSERT INTO player_external_references(player_id,provider,external_id) SELECT id,'THE_SPORTS_DB','sports-source' FROM players");
      }

      // Act
      Flyway.configure()
          .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
          .schemas(schema)
          .defaultSchema(schema)
          .target("5")
          .load()
          .migrate();

      // Assert
      try (Connection connection =
              DriverManager.getConnection(
                  DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
          Statement statement = connection.createStatement()) {
        statement.execute("SET search_path TO " + schema);
        try (ResultSet rows =
            statement.executeQuery(
                "SELECT p.id,p.name,p.position,p.team,p.league,p.active,p.team_id,p.date_of_birth,p.nationality,p.image_url,p.fallback_image_url,r.id AS reference_id,r.provider,r.external_id FROM players p JOIN player_external_references r ON r.player_id=p.id ORDER BY r.id")) {
          String[] externalIds = {"player-source", "historical-player-source", "sports-source"};
          String[] providers = {"FOOTBALL_DATA", "FOOTBALL_DATA", "THE_SPORTS_DB"};
          for (int index = 0; index < externalIds.length; index++) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong("id")).isEqualTo(1L);
            assertThat(rows.getString("name")).isEqualTo("Legacy");
            assertThat(rows.getString("position")).isEqualTo("Forward");
            assertThat(rows.getDate("date_of_birth").toLocalDate())
                .isEqualTo(java.time.LocalDate.of(1990, 6, 20));
            assertThat(rows.getString("nationality")).isEqualTo("Spain");
            assertThat(rows.getString("image_url")).isEqualTo("https://images.example/main");
            assertThat(rows.getString("fallback_image_url"))
                .isEqualTo("https://images.example/secondary");
            assertThat(rows.getLong("reference_id")).isEqualTo(index + 1L);
            assertThat(rows.getString("provider")).isEqualTo(providers[index]);
            assertThat(rows.getString("team")).isEqualTo("Texto original");
            assertThat(rows.getString("league")).isEqualTo("Liga original");
            assertThat(rows.getBoolean("active")).isFalse();
            assertThat(rows.getObject("team_id")).isNull();
            assertThat(rows.getString("external_id")).isEqualTo(externalIds[index]);
          }
          assertThat(rows.next()).isFalse();
        }
        // La cardinalidad por propietario sigue abierta después de V5, pero la identidad es única.
        statement.execute(
            "INSERT INTO player_external_references(player_id,provider,external_id) SELECT id,'FOOTBALL_DATA','additional-player-source' FROM players");
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO player_external_references(player_id,provider,external_id) SELECT id,'FOOTBALL_DATA','player-source' FROM players"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23505"));
        statement.execute(
            "INSERT INTO players(name,team,league,position,active) VALUES ('Other owner','Otro equipo','Otra liga','Forward',true)");
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO player_external_references(player_id,provider,external_id) SELECT id,'FOOTBALL_DATA','player-source' FROM players WHERE name='Other owner'"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23505"));
        try (ResultSet rows =
            statement.executeQuery(
                "SELECT (SELECT count(*) FROM teams), (SELECT count(*) FROM leagues),completed_at FROM catalog_transition WHERE id=1")) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getInt(1)).isZero();
          assertThat(rows.getInt(2)).isZero();
          assertThat(rows.getObject(3)).isNull();
          assertThat(rows.next()).isFalse();
        }
        assertThatThrownBy(() -> statement.execute("UPDATE players SET team=NULL"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23502"));
        assertThatThrownBy(() -> statement.execute("UPDATE players SET league=NULL"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23502"));
        assertThatThrownBy(() -> statement.execute("UPDATE players SET team_id=999"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23503"));
      }
    }

    @Test
    @DisplayName(
        "Las referencias exigen propietario y unicidad de identidad y proveedor por propietario")
    void constrainExternalReferences() throws Exception {
      // Arrange
      String schema = "v5_reference_constraints";
      Flyway.configure()
          .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
          .schemas(schema)
          .defaultSchema(schema)
          .target("5")
          .load()
          .migrate();
      try (Connection connection =
              DriverManager.getConnection(
                  DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
          Statement statement = connection.createStatement()) {
        statement.execute("SET search_path TO " + schema);
        statement.execute("INSERT INTO leagues(name) VALUES ('Primera'),('Segunda')");
        statement.execute(
            "INSERT INTO teams(name,league_id,current) VALUES ('Primero',1,true),('Segundo',2,false)");
        statement.execute(
            "INSERT INTO league_external_references(league_id,provider,external_id) VALUES (1,'FOOTBALL_DATA','league-source')");
        statement.execute(
            "INSERT INTO team_external_references(team_id,provider,external_id) VALUES (1,'FOOTBALL_DATA','team-source')");

        // Act / Assert
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO league_external_references(league_id,provider,external_id) VALUES (2,'FOOTBALL_DATA','league-source')"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23505"));
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO league_external_references(league_id,provider,external_id) VALUES (1,'FOOTBALL_DATA','another-source')"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23505"));
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO team_external_references(team_id,provider,external_id) VALUES (2,'FOOTBALL_DATA','team-source')"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23505"));
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO team_external_references(team_id,provider,external_id) VALUES (1,'FOOTBALL_DATA','another-source')"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23505"));
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO teams(name,league_id,current) VALUES ('Sin liga',NULL,true)"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23502"));
      }
    }
  }
}
