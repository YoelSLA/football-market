package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class PositionalValuationPersistenceTest {
  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  @Nested
  @DisplayName("Restricciones posicionales")
  class Persistence {
    @Test
    @DisplayName("Rechaza penalización de rendimiento para arqueros")
    void rejectsGoalkeeperPenalty() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(DATABASE, "positional_persistence");
          Statement statement = connection.createStatement()) {
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO player_positional_valuations(player_id,valuation_date,strategy_name,strategy_version,general_position,source_summary,source_extraction_date,positive_score,performance_penalty,performance_adjustment,discipline_penalty,discipline_adjustment,raw_score,confidence,final_score,imputed_metrics,calculated_at) VALUES(900,current_date,'POSITIONAL_PERFORMANCE','v1','GOALKEEPER','JSON',current_date,.5,.2,.1,.5,0,.5,1,.5,ARRAY[]::text[],now())"))
            .isInstanceOf(SQLException.class);
      }
    }
  }
}
