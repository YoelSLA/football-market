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
class ValuationSnapshotQueryTest {
  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  @Nested
  @DisplayName("Campaña de origen")
  class Snapshot {
    @Test
    @DisplayName("Mantiene índices por campaña y canal para seleccionar el snapshot")
    void hasCampaignIndex() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(DATABASE, "valuation_snapshot");
          Statement statement = connection.createStatement();
          ResultSet rows =
              statement.executeQuery(
                  "SELECT count(*) FROM pg_indexes WHERE schemaname='valuation_snapshot' AND indexname='ix_player_statistics_campaign'")) {
        rows.next();
        assertThat(rows.getInt(1)).isOne();
      }
    }
  }
}
