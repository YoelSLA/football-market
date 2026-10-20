package footballmarket.repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.testcontainers.postgresql.PostgreSQLContainer;

final class ValuationDatabaseSupport {
  private ValuationDatabaseSupport() {}

  static Connection migrated(PostgreSQLContainer database, String schema) throws SQLException {
    Flyway.configure()
        .dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
        .schemas(schema)
        .defaultSchema(schema)
        .load()
        .migrate();
    Connection connection =
        DriverManager.getConnection(
            database.getJdbcUrl(), database.getUsername(), database.getPassword());
    try (Statement statement = connection.createStatement()) {
      statement.execute("SET search_path TO " + schema);
      statement.execute("INSERT INTO leagues(id,name) VALUES(900,'League')");
      statement.execute("INSERT INTO teams(id,name,league_id,current) VALUES(900,'Team',900,true)");
      statement.execute(
          "INSERT INTO players(id,name,team_id,position,active) VALUES(900,'Player',900,'Forward',true)");
    }
    return connection;
  }
}
