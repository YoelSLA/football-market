package footballmarket.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/** Barrera JDBC de testing: pausa una lectura real sin sustituir Repository ni transacciones. */
@TestConfiguration(proxyBeanMethods = false)
public class PlayerReferenceReadGate {
  private static final AtomicReference<Gate> NEXT = new AtomicReference<>();

  public static Gate arm() {
    Gate gate = new Gate();
    NEXT.set(gate);
    return gate;
  }

  public static void reset() {
    Gate gate = NEXT.getAndSet(null);
    if (gate != null) {
      gate.release.countDown();
    }
  }

  public static final class Gate implements AutoCloseable {
    private final CountDownLatch entered = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);

    public boolean awaitRead() throws InterruptedException {
      return entered.await(10, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
      release.countDown();
    }
  }

  @Bean
  static BeanPostProcessor gateDataSource() {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String name) {
        if (!(bean instanceof DataSource source)) {
          return bean;
        }
        return new DelegatingDataSource(source) {
          @Override
          public Connection getConnection() throws SQLException {
            return gated(super.getConnection());
          }

          @Override
          public Connection getConnection(String username, String password) throws SQLException {
            return gated(super.getConnection(username, password));
          }
        };
      }
    };
  }

  private static Connection gated(Connection connection) {
    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, arguments) -> {
              if (method.getName().equals("prepareStatement")
                  && arguments[0] instanceof String sql
                  && sql.startsWith("select")
                  && sql.contains("player_external_references")
                  && sql.matches("(?s).*where.*external_id\\s*=.*")) {
                Gate gate = NEXT.getAndSet(null);
                if (gate != null) {
                  gate.entered.countDown();
                  if (!gate.release.await(10, TimeUnit.SECONDS)) {
                    throw new SQLException("Se agotó la barrera de testing");
                  }
                }
              }
              try {
                return method.invoke(connection, arguments);
              } catch (InvocationTargetException failure) {
                throw failure.getCause();
              }
            });
  }
}
