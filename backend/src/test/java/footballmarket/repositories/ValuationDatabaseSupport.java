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
    Connection connection = migratedEmpty(database, schema);
    try (Statement statement = connection.createStatement()) {
      statement.execute("INSERT INTO leagues(id,name) VALUES(900,'League')");
      statement.execute("INSERT INTO teams(id,name,league_id,current) VALUES(900,'Team',900,true)");
      statement.execute(
          "INSERT INTO players(id,name,team_id,position,active) VALUES(900,'Player',900,'Forward',true)");
    }
    return connection;
  }

  static Connection migratedEmpty(PostgreSQLContainer database, String schema) throws SQLException {
    String url = createDatabase(database, schema);
    Flyway.configure()
        .dataSource(url, database.getUsername(), database.getPassword())
        .load()
        .migrate();
    return DriverManager.getConnection(url, database.getUsername(), database.getPassword());
  }

  static String createDatabase(PostgreSQLContainer database, String name) throws SQLException {
    try (Connection admin =
            DriverManager.getConnection(
                database.getJdbcUrl(), database.getUsername(), database.getPassword());
        Statement statement = admin.createStatement()) {
      statement.execute("CREATE DATABASE \"" + name + "\"");
    }
    return databaseUrl(database, name);
  }

  static String databaseUrl(PostgreSQLContainer database, String name) {
    String url = database.getJdbcUrl();
    int databaseStart = url.indexOf('/', "jdbc:postgresql://".length()) + 1;
    int queryStart = url.indexOf('?', databaseStart);
    return url.substring(0, databaseStart)
        + name
        + (queryStart < 0 ? "" : url.substring(queryStart));
  }
}
