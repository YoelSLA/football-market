package footballmarket.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Aísla pruebas con commits REQUIRES_NEW usando exclusivamente el contenedor de testing. */
public class ResetPlayerCatalogListener extends AbstractTestExecutionListener {
  @Override
  public void beforeTestMethod(TestContext context) throws Exception {
    PostgreSQLContainer container =
        context.getApplicationContext().getBean(PostgreSQLContainer.class);
    try (Connection connection =
            DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
        Statement statement = connection.createStatement()) {
      statement.execute("TRUNCATE TABLE players CASCADE");
    }
  }
}
