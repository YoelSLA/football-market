package footballmarket.repositories;

import static org.assertj.core.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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

  private void migrate(String schema, String target) {
    Flyway.configure()
        .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
        .schemas(schema)
        .defaultSchema(schema)
        .target(target)
        .load()
        .migrate();
  }

  private Connection open(String schema) throws Exception {
    Connection connection =
        DriverManager.getConnection(
            DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
    connection.createStatement().execute("SET search_path TO " + schema);
    return connection;
  }

  @Nested
  @DisplayName("Esquema definitivo tras V5")
  class FinalSchema {
    @Test
    @DisplayName("La instalación limpia no expone textos legacy de equipo ni de liga")
    void dropLegacyTexts() throws Exception {
      // Arrange
      String schema = "v5_final_schema";
      migrate(schema, "5");

      // Act
      try (Connection connection = open(schema);
          Statement statement = connection.createStatement()) {
        // Assert
        try (ResultSet columns =
            statement.executeQuery(
                "SELECT column_name FROM information_schema.columns "
                    + "WHERE table_schema = current_schema() AND table_name = 'players' "
                    + "ORDER BY column_name")) {
          List<String> names = new ArrayList<>();
          while (columns.next()) {
            names.add(columns.getString("column_name"));
          }
          assertThat(names).contains("team_id").doesNotContain("team", "league");
        }

        // Verify: la asociación es la única fuente y es obligatoria.
        statement.execute("INSERT INTO leagues(name) VALUES ('League')");
        statement.execute("INSERT INTO teams(name,league_id,current) VALUES ('Team',1,true)");
        statement.execute(
            "INSERT INTO players(name,position,active,team_id) VALUES ('Associated','Forward',true,1)");
        try (ResultSet rows =
            statement.executeQuery(
                "SELECT p.name,t.name AS team,l.name AS league FROM players p "
                    + "JOIN teams t ON t.id=p.team_id JOIN leagues l ON l.id=t.league_id")) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString("name")).isEqualTo("Associated");
          assertThat(rows.getString("team")).isEqualTo("Team");
          assertThat(rows.getString("league")).isEqualTo("League");
          assertThat(rows.next()).isFalse();
        }
        assertThatThrownBy(() -> statement.execute("UPDATE players SET team_id=999"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23503"));
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO players(name,position,active) VALUES ('X','F',true)"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23502"));
      }
    }

    @Test
    @DisplayName(
        "V5 exige evidencia, claves únicas, fechas coherentes y referencias válidas de casos y equipos")
    void constrainCasesAndAttempts() throws Exception {
      // Arrange
      String schema = "v5_case_attempt_constraints";
      migrate(schema, "5");

      // Act
      try (Connection connection = open(schema);
          Statement statement = connection.createStatement()) {
        statement.execute("INSERT INTO leagues(name) VALUES ('League')");
        statement.execute("INSERT INTO teams(name,league_id,current) VALUES ('Team',1,true)");
        statement.execute(
            "INSERT INTO players(name,position,active,team_id) VALUES ('Player','Forward',true,1)");
        statement.execute(
            "INSERT INTO pending_review_cases(category,cause_code,subject_type,subject_id,case_key,first_detected_at,last_detected_at,evidence) "
                + "VALUES ('PLAYER_TEAM_UNRESOLVED','TEAM_UNRESOLVED','PLAYER',1,'stable-key','2026-10-01','2026-10-02','{\"receivedTeamName\":\"Original\",\"observations\":[]}'::jsonb)");
        statement.execute(
            "INSERT INTO team_resolution_attempts(team_id,last_call_at,last_call_name,technical_result) VALUES (1,'2026-10-01','Team','INVALID_RESPONSE')");

        // Assert
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
                        "INSERT INTO pending_review_cases(category,cause_code,subject_type,case_key,first_detected_at,last_detected_at,evidence) "
                            + "VALUES ('LEGACY_TEAM_ASSOCIATION','NO_TEAM_MATCH','PLAYER','retired-key','2026-10-01','2026-10-02','{}'::jsonb)"))
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
                "SELECT evidence->>'receivedTeamName',jsonb_array_length(evidence->'observations') FROM pending_review_cases")) {
          assertThat(row.next()).isTrue();
          assertThat(row.getString(1)).isEqualTo("Original");
          assertThat(row.getInt(2)).isZero();
        }
      }
    }

    @Test
    @DisplayName(
        "La instalación limpia conserva identidad, atributos y referencias, sin textos legacy")
    void installCleanCatalog() throws Exception {
      // Arrange: US06 retiró la migración del catálogo previo, de modo que V5 se aplica sobre una
      // base sin jugadores. Las invariantes de identidad se comprueban sobre el catálogo
      // resultante.
      String schema = "v5_clean_install";
      migrate(schema, "5");

      // Act
      try (Connection connection = open(schema);
          Statement statement = connection.createStatement()) {
        statement.execute("INSERT INTO leagues(name) VALUES ('League')");
        statement.execute("INSERT INTO teams(name,league_id,current) VALUES ('Team A',1,true)");
        statement.execute(
            "INSERT INTO players(name,position,active,team_id) VALUES ('Player','Forward',false,1)");
        statement.execute(
            "UPDATE players SET date_of_birth='1990-06-20',nationality='Spain',image_url='https://images.example/main',fallback_image_url='https://images.example/secondary'");
        statement.execute(
            "INSERT INTO player_external_references(player_id,provider,external_id) SELECT id,'FOOTBALL_DATA','player-source' FROM players");
        statement.execute(
            "INSERT INTO player_external_references(player_id,provider,external_id) SELECT id,'FOOTBALL_DATA','historical-player-source' FROM players");
        statement.execute(
            "INSERT INTO player_external_references(player_id,provider,external_id) SELECT id,'THE_SPORTS_DB','sports-source' FROM players");

        // Assert
        try (ResultSet rows =
            statement.executeQuery(
                "SELECT p.id,p.name,p.position,p.active,p.team_id,p.date_of_birth,p.nationality,p.image_url,p.fallback_image_url,r.id AS reference_id,r.provider,r.external_id FROM players p JOIN player_external_references r ON r.player_id=p.id ORDER BY r.id")) {
          String[] externalIds = {"player-source", "historical-player-source", "sports-source"};
          String[] providers = {"FOOTBALL_DATA", "FOOTBALL_DATA", "THE_SPORTS_DB"};
          for (int index = 0; index < externalIds.length; index++) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong("id")).isEqualTo(1L);
            assertThat(rows.getString("name")).isEqualTo("Player");
            assertThat(rows.getString("position")).isEqualTo("Forward");
            assertThat(rows.getDate("date_of_birth").toLocalDate())
                .isEqualTo(java.time.LocalDate.of(1990, 6, 20));
            assertThat(rows.getString("nationality")).isEqualTo("Spain");
            assertThat(rows.getString("image_url")).isEqualTo("https://images.example/main");
            assertThat(rows.getString("fallback_image_url"))
                .isEqualTo("https://images.example/secondary");
            assertThat(rows.getLong("reference_id")).isEqualTo(index + 1L);
            assertThat(rows.getString("provider")).isEqualTo(providers[index]);
            assertThat(rows.getBoolean("active")).isFalse();
            assertThat(rows.getObject("team_id")).isNotNull();
            assertThat(rows.getString("external_id")).isEqualTo(externalIds[index]);
          }
          assertThat(rows.next()).isFalse();
        }
        // La liga del Player se deriva del Team; no existe columna ni tabla de liga en Player.
        try (ResultSet row =
            statement.executeQuery(
                "SELECT l.name FROM players p JOIN teams t ON t.id=p.team_id "
                    + "JOIN leagues l ON l.id=t.league_id")) {
          assertThat(row.next()).isTrue();
          assertThat(row.getString(1)).isEqualTo("League");
          assertThat(row.next()).isFalse();
        }
        // La cardinalidad por propietario sigue abierta, pero la identidad es única.
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
            "INSERT INTO players(name,position,active,team_id) VALUES ('Other owner','Forward',true,1)");
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO player_external_references(player_id,provider,external_id) SELECT id,'FOOTBALL_DATA','player-source' FROM players WHERE name='Other owner'"))
            .isInstanceOf(SQLException.class)
            .satisfies(
                error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23505"));
      }
    }

    @Test
    @DisplayName(
        "Las referencias exigen propietario y unicidad de identidad y proveedor por propietario")
    void constrainExternalReferences() throws Exception {
      // Arrange
      String schema = "v5_reference_constraints";
      migrate(schema, "5");

      // Act
      try (Connection connection = open(schema);
          Statement statement = connection.createStatement()) {
        statement.execute("INSERT INTO leagues(name) VALUES ('Primera'),('Segunda')");
        statement.execute(
            "INSERT INTO teams(name,league_id,current) VALUES ('Primero',1,true),('Segundo',2,false)");
        statement.execute(
            "INSERT INTO league_external_references(league_id,provider,external_id) VALUES (1,'FOOTBALL_DATA','league-source')");
        statement.execute(
            "INSERT INTO team_external_references(team_id,provider,external_id) VALUES (1,'FOOTBALL_DATA','team-source')");

        // Assert
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
