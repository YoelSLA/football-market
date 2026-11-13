package footballmarket.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.support.ValuationDatabaseSupport;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@Tag("integration")
class FlywaySchemaTest {
  private static final String LAST_VERSION = "20261107030028";

  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  private String createDatabase(String name) throws Exception {
    return ValuationDatabaseSupport.createDatabase(DATABASE, name);
  }

  private Flyway flyway(String url, String target) {
    var configuration =
        Flyway.configure()
            .dataSource(url, DATABASE.getUsername(), DATABASE.getPassword())
            .validateOnMigrate(true)
            .outOfOrder(false)
            .baselineOnMigrate(false)
            .cleanDisabled(true);
    if (target != null) configuration.target(target);
    return configuration.load();
  }

  @Nested
  @DisplayName("Reconstrucción completa")
  class CompleteReconstruction {
    @Test
    @DisplayName("Migra PostgreSQL vacío y valida las veintiocho versiones")
    void migratesEmptyPostgres() throws Exception {
      String url = createDatabase("flyway_full_reconstruction");
      Flyway flyway = flyway(url, null);

      var result = flyway.migrate();
      var validation = flyway.validateWithResult();

      assertThat(result.migrationsExecuted).isEqualTo(28);
      assertThat(result.targetSchemaVersion).hasToString(LAST_VERSION);
      assertThat(validation.validationSuccessful).isTrue();
      try (Connection connection =
              DriverManager.getConnection(url, DATABASE.getUsername(), DATABASE.getPassword());
          Statement statement = connection.createStatement();
          ResultSet rows =
              statement.executeQuery(
                  "SELECT count(*) FROM flyway_schema_history WHERE success AND type = 'SQL'")) {
        rows.next();
        assertThat(rows.getInt(1)).isEqualTo(28);
      }
    }
  }

  @Nested
  @DisplayName("Evolución incremental")
  class IncrementalEvolution {
    @Test
    @DisplayName("Avanza por checkpoints y conserva los datos creados por el catálogo base")
    void migratesThroughCheckpoints() throws Exception {
      String url = createDatabase("flyway_incremental_evolution");
      List<String> checkpoints =
          List.of(
              "20261107030004",
              "20261107030012",
              "20261107030015",
              "20261107030020",
              "20261107030024",
              LAST_VERSION);

      flyway(url, checkpoints.getFirst()).migrate();
      try (Connection connection =
              DriverManager.getConnection(url, DATABASE.getUsername(), DATABASE.getPassword());
          Statement statement = connection.createStatement()) {
        statement.execute("INSERT INTO leagues(id,name) VALUES(1,'League')");
        statement.execute("INSERT INTO teams(id,name,league_id,current) VALUES(1,'Team',1,true)");
        statement.execute(
            "INSERT INTO players(id,name,position,active,team_id)"
                + " VALUES(1,'Player','Forward',true,1)");
      }

      for (String checkpoint : checkpoints.subList(1, checkpoints.size())) {
        Flyway checkpointFlyway = flyway(url, checkpoint);
        checkpointFlyway.migrate();
        assertThat(checkpointFlyway.validateWithResult().validationSuccessful).isTrue();
      }

      try (Connection connection =
              DriverManager.getConnection(url, DATABASE.getUsername(), DATABASE.getPassword());
          Statement statement = connection.createStatement();
          ResultSet rows = statement.executeQuery("SELECT name FROM players WHERE id=1")) {
        assertThat(rows.next()).isTrue();
        assertThat(rows.getString(1)).isEqualTo("Player");
        assertThat(rows.next()).isFalse();
      }
    }
  }
}
