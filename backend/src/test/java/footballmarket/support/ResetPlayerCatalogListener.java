package footballmarket.support;

import org.flywaydb.core.Flyway;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Reaprovisiona el esquema desechable entre pruebas con commits REQUIRES_NEW. El historial canónico
 * no admite limpieza por DML/TRUNCATE; se recrea con las migraciones reales.
 */
public class ResetPlayerCatalogListener extends AbstractTestExecutionListener {
  @Override
  public void beforeTestMethod(TestContext context) throws Exception {
    PostgreSQLContainer container =
        context.getApplicationContext().getBean(PostgreSQLContainer.class);
    Flyway applicationFlyway = context.getApplicationContext().getBean(Flyway.class);
    // Nunca limpiar el datasource externo: el destino se toma del contenedor de testing.
    Flyway fixtureFlyway =
        Flyway.configure()
            .configuration(applicationFlyway.getConfiguration())
            .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
            .schemas("public")
            .defaultSchema("public")
            .cleanDisabled(false)
            .load();
    fixtureFlyway.clean();
    fixtureFlyway.migrate();
  }
}
