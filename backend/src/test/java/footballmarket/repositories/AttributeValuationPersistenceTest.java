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
@org.junit.jupiter.api.Tag("integration")
class AttributeValuationPersistenceTest {
  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  @Nested
  @DisplayName("Nulabilidad exclusiva de atributos")
  class Persistence {
    @Test
    @DisplayName("Rechaza atributos de campo en un arquero")
    void rejectsFieldAttributesForGoalkeeper() throws Exception {
      try (Connection connection =
              ValuationDatabaseSupport.migrated(DATABASE, "attribute_persistence");
          Statement statement = connection.createStatement()) {
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO player_attribute_valuations(player_id,valuation_date,strategy_name,strategy_version,general_position,source_summary,source_extraction_date,shooting,saving,distribution,activity,base_grl,discipline_adjustment,raw_grl,confidence,final_grl,imputed_metrics,calculated_at) VALUES(900,current_date,'GENERAL_ATTRIBUTES','v1','GOALKEEPER','JSON',current_date,.5,.5,.5,.5,.5,0,.5,1,.5,ARRAY[]::text[],now())"))
            .isInstanceOf(SQLException.class);
      }
    }
  }
}
