package footballmarket.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Fallo técnico de una sentencia de cierre en la DB aislada de testing. Decora conexiones reales;
 * no prepara datos, no consulta resultados y no sustituye Services, Repository ni JPA.
 */
public class StatisticsCloseFailureInjector implements BeanPostProcessor {
  private final AtomicReference<Thread> armedThread = new AtomicReference<>();

  /** Arma un único fallo en el hilo coordinador, sin tocar datos ni ejecutar el caso de uso. */
  public void failNextClose() {
    armedThread.set(Thread.currentThread());
  }

  public void reset() {
    armedThread.set(null);
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (!(bean instanceof DataSource source)) {
      return bean;
    }
    return new DelegatingDataSource(source) {
      @Override
      public Connection getConnection() throws SQLException {
        return connection(source.getConnection());
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
        return connection(source.getConnection(username, password));
      }
    };
  }

  private Connection connection(Connection delegate) {
    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
              Object result = invoke(delegate, method, args);
              if ("prepareStatement".equals(method.getName())
                  && args != null
                  && args[0] instanceof String sql
                  && sql.stripLeading()
                      .toLowerCase(Locale.ROOT)
                      .replaceAll("\\s+", " ")
                      .startsWith("update player_statistics_extraction_runs ")
                  && result instanceof PreparedStatement statement) {
                return statement(statement);
              }
              return result;
            });
  }

  private PreparedStatement statement(PreparedStatement delegate) {
    return (PreparedStatement)
        Proxy.newProxyInstance(
            PreparedStatement.class.getClassLoader(),
            new Class<?>[] {PreparedStatement.class},
            (proxy, method, args) -> {
              if (("executeUpdate".equals(method.getName())
                      || "executeBatch".equals(method.getName())
                      || "execute".equals(method.getName()))
                  && armedThread.compareAndSet(Thread.currentThread(), null)) {
                throw new SQLException(
                    "Fallo controlado del cierre de campaña en testing", "08006");
              }
              return invoke(delegate, method, args);
            });
  }

  private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
    try {
      return method.invoke(target, args);
    } catch (InvocationTargetException failure) {
      throw failure.getCause();
    }
  }
}
