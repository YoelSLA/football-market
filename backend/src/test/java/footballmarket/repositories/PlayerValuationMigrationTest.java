package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@org.junit.jupiter.api.Tag("integration")
class PlayerValuationMigrationTest {
  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  @Nested
  @DisplayName("Esquema de valoraciones")
  class Migration {
    @Test
    @DisplayName("Crea ambas tablas y sus índices de consulta")
    void createsTablesAndIndexes() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(DATABASE, "valuation_migration");
          Statement statement = connection.createStatement();
          ResultSet rows =
              statement.executeQuery(
                  "SELECT count(*) FROM pg_indexes WHERE schemaname='valuation_migration' AND tablename IN ('player_positional_valuations','player_attribute_valuations')")) {
        rows.next();
        assertThat(rows.getInt(1)).isGreaterThanOrEqualTo(8);
      }
    }
  }
}
