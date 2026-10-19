package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
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

/**
 * Convergencia de una base que aplicó la forma aditiva de V5.
 *
 * <p>V5 se aplicó en dos formas sucesivas en el historial del repositorio. Una instalación limpia
 * siempre obtiene la forma definitiva; esta prueba reconstruye la forma aditiva sobre una base
 * migrada y comprueba que V8 la lleva al mismo estado final, preservando los jugadores existentes.
 */
@Testcontainers
@org.junit.jupiter.api.Tag("integration")
class TeamLeagueConvergenceMigrationTest {
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

  private List<String> playerColumns(Connection connection) throws SQLException {
    List<String> names = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        var rows =
            statement.executeQuery(
                "SELECT column_name FROM information_schema.columns "
                    + "WHERE table_schema = current_schema() AND table_name = 'players' "
                    + "ORDER BY column_name")) {
      while (rows.next()) {
        names.add(rows.getString("column_name"));
      }
    }
    return names;
  }

  /**
   * Reconstruye sobre una base migrada la forma aditiva que V5 tuvo antes de ser reescrita.
   *
   * <p>Una instalación limpia nunca pasa por esta forma, por eso la prueba debe provocarla
   * explícitamente para poder verificar la convergencia.
   */
  private void seedLegacyCatalog(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute("ALTER TABLE players ADD COLUMN \"team\" VARCHAR(255) NOT NULL");
      statement.execute("ALTER TABLE players ADD COLUMN \"league\" VARCHAR(255) NOT NULL");
      statement.execute("ALTER TABLE players ALTER COLUMN team_id DROP NOT NULL");
      statement.execute("CREATE TABLE catalog_transition (id INTEGER PRIMARY KEY CHECK (id = 1))");
      statement.execute("INSERT INTO catalog_transition (id) VALUES (1)");
      statement.execute(
          """
          INSERT INTO players(name, team, league, position, active, team_id) VALUES
            ('Jugador Uno','River Plate','Primera Division','Forward',true,NULL),
            ('Jugador Dos','Boca Juniors','Primera Division','Midfielder',true,NULL),
            ('Jugador Tres','River Plate','Primera Division','Defender',false,NULL),
            ('Jugador Cuatro','Arsenal','Premier League','Forward',true,NULL)
          """);
    }
  }

  private int count(Connection connection, String schema, String query) throws SQLException {
    try (var rows = connection.createStatement().executeQuery(query.replace("@schema@", schema))) {
      rows.next();
      return rows.getInt(1);
    }
  }

  @Nested
  @DisplayName("Convergencia desde la forma aditiva de V5")
  class FromAdditiveShape {
    @Test
    @DisplayName("Deriva ligas y equipos del catálogo legacy sin perder jugadores")
    void derivaLigasYEquiposSinPerderJugadores() throws Exception {
      // Arrange
      String schema = "v8_converge_deriva";
      migrate(schema, "5");
      try (Connection connection = open(schema)) {
        seedLegacyCatalog(connection);

        // Act
        migrate(schema, "8");

        // Assert
        assertThat(count(connection, schema, "SELECT count(*) FROM @schema@.players")).isEqualTo(4);
        assertThat(count(connection, schema, "SELECT count(*) FROM @schema@.teams")).isEqualTo(3);
        assertThat(count(connection, schema, "SELECT count(*) FROM @schema@.leagues")).isEqualTo(2);
        assertThat(
                count(
                    connection,
                    schema,
                    "SELECT count(*) FROM @schema@.players p "
                        + "LEFT JOIN @schema@.teams t ON t.id = p.team_id WHERE t.id IS NULL"))
            .isZero();
      }
    }

    @Test
    @DisplayName("La liga de cada equipo se deriva del texto legacy del jugador")
    void derivaLaLigaDeCadaEquipo() throws Exception {
      // Arrange
      String schema = "v8_converge_liga";
      migrate(schema, "5");
      try (Connection connection = open(schema)) {
        seedLegacyCatalog(connection);

        // Act
        migrate(schema, "8");

        // Assert
        try (var rows =
            connection
                .createStatement()
                .executeQuery(
                    "SELECT t.name, l.name FROM "
                        + schema
                        + ".teams t "
                        + "JOIN "
                        + schema
                        + ".leagues l ON l.id = t.league_id "
                        + "ORDER BY t.name")) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString(1)).isEqualTo("Arsenal");
          assertThat(rows.getString(2)).isEqualTo("Premier League");
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString(1)).isEqualTo("Boca Juniors");
          assertThat(rows.getString(2)).isEqualTo("Primera Division");
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString(1)).isEqualTo("River Plate");
          assertThat(rows.getString(2)).isEqualTo("Primera Division");
          assertThat(rows.next()).isFalse();
        }
      }
    }

    @Test
    @DisplayName("Retira los textos legacy y vuelve obligatoria la asociación con el equipo")
    void retiraTextosLegacyYVuelveObligatoriaLaAsociacion() throws Exception {
      // Arrange
      String schema = "v8_converge_esquema";
      migrate(schema, "5");
      try (Connection connection = open(schema)) {
        seedLegacyCatalog(connection);

        // Act
        migrate(schema, "8");

        // Assert
        assertThat(playerColumns(connection)).contains("team_id").doesNotContain("team", "league");
        try (Statement statement = connection.createStatement()) {
          // Verify: la asociación es la única fuente y es obligatoria.
          assertThatThrownBy(
                  () ->
                      statement.execute(
                          "INSERT INTO "
                              + schema
                              + ".players(name,position,active) "
                              + "VALUES ('Sin equipo','Forward',true)"))
              .isInstanceOf(SQLException.class)
              .satisfies(
                  error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23502"));
        }
      }
    }

    @Test
    @DisplayName("Elimina la tabla de control manual de la forma aditiva")
    void eliminaLaTablaDeControlManual() throws Exception {
      // Arrange
      String schema = "v8_converge_control";
      migrate(schema, "5");
      try (Connection connection = open(schema)) {
        seedLegacyCatalog(connection);

        // Act
        migrate(schema, "8");

        // Assert
        assertThat(
                count(
                    connection,
                    schema,
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema ="
                        + " current_schema() AND table_name = 'catalog_transition'"))
            .isZero();
      }
    }

    @Test
    @DisplayName("Retira la categoría de revisión exclusiva de la forma aditiva")
    void retiraLaCategoriaLegacyDeRevision() throws Exception {
      // Arrange
      String schema = "v8_converge_categoria";
      migrate(schema, "5");
      try (Connection connection = open(schema)) {
        seedLegacyCatalog(connection);

        // Act
        migrate(schema, "8");

        // Assert
        try (Statement statement = connection.createStatement()) {
          assertThatThrownBy(
                  () ->
                      statement.execute(
                          "INSERT INTO "
                              + schema
                              + ".pending_review_cases(case_key,category,"
                              + "subject_provider,subject_external_id,first_detected_at,"
                              + "last_detected_at) VALUES ('k1','LEGACY_TEAM_ASSOCIATION',"
                              + "'FOOTBALL_DATA','1',now(),now())"))
              .isInstanceOf(SQLException.class);
        }
      }
    }
  }

  @Nested
  @DisplayName("Convergencia sobre una base ya definitiva")
  class FromFinalShape {
    @Test
    @DisplayName("No modifica una base que ya obtuvo el esquema definitivo en V5")
    void noModificaUnaBaseYaDefinitiva() throws Exception {
      // Arrange
      String schema = "v8_converge_idempotente";
      migrate(schema, "5");
      try (Connection connection = open(schema);
          Statement statement = connection.createStatement()) {
        statement.execute("INSERT INTO leagues(name) VALUES ('League')");
        statement.execute("INSERT INTO teams(name,league_id,current) VALUES ('Team',1,true)");
        statement.execute(
            "INSERT INTO players(name,position,active,team_id) "
                + "VALUES ('Asociado','Forward',true,1)");

        // Act
        migrate(schema, "8");

        // Assert
        assertThat(playerColumns(connection)).contains("team_id").doesNotContain("team", "league");
        try (var rows =
            connection.createStatement().executeQuery("SELECT name FROM " + schema + ".players")) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString(1)).isEqualTo("Asociado");
          assertThat(rows.next()).isFalse();
        }
      }
    }
  }
}
