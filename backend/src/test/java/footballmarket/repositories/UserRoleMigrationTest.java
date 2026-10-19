package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@org.junit.jupiter.api.Tag("integration")
class UserRoleMigrationTest {
  @Container static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  private void migrate(String schema, String target) {
    Flyway.configure()
        .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
        .schemas(schema)
        .defaultSchema(schema)
        .target(target)
        .load()
        .migrate();
  }

  private Connection open(String schema) throws Exception {
    Connection connection =
        DriverManager.getConnection(
            DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
    connection.createStatement().execute("SET search_path TO " + schema);
    return connection;
  }

  private List<String> usernames(Connection connection, String schema) throws SQLException {
    List<String> names = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rows =
            statement.executeQuery("SELECT username FROM " + schema + ".users ORDER BY username")) {
      while (rows.next()) {
        names.add(rows.getString("username"));
      }
    }
    return names;
  }

  @Nested
  @DisplayName("Relleno de usuarios existentes")
  class Backfill {
    @Test
    @DisplayName("Preserva la cuenta y la credencial de los usuarios previos a la ampliación")
    void conservaLasCuentasExistentes() throws Exception {
      // Arrange
      String schema = "v6_backfill_preserva";
      migrate(schema, "5");
      try (Connection connection = open(schema);
          Statement statement = connection.createStatement()) {
        statement.execute(
            "INSERT INTO users(id,email,password) VALUES "
                + "(1,'juan.perez@test.com','hash-previo'),"
                + "(2,'maria@test.com','otro-hash')");

        // Act
        migrate(schema, "7");

        // Assert
        try (ResultSet rows =
            statement.executeQuery(
                "SELECT id,email,password,role FROM " + schema + ".users ORDER BY id")) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getLong("id")).isEqualTo(1L);
          assertThat(rows.getString("email")).isEqualTo("juan.perez@test.com");
          assertThat(rows.getString("password")).isEqualTo("hash-previo");
          assertThat(rows.getString("role")).isEqualTo("USER");

          assertThat(rows.next()).isTrue();
          assertThat(rows.getLong("id")).isEqualTo(2L);
          assertThat(rows.getString("email")).isEqualTo("maria@test.com");
          assertThat(rows.getString("password")).isEqualTo("otro-hash");
          assertThat(rows.getString("role")).isEqualTo("USER");
          assertThat(rows.next()).isFalse();
        }
      }
    }

    @Test
    @DisplayName("Asigna a todo usuario previo el rol USER y un nombre de usuario derivado")
    void derivaNombreDeUsuarioYAsignaRolUser() throws Exception {
      // Arrange
      String schema = "v6_backfill_deriva";
      migrate(schema, "5");
      try (Connection connection = open(schema);
          Statement statement = connection.createStatement()) {
        statement.execute(
            "INSERT INTO users(id,email,password) VALUES "
                + "(1,'juan.perez@test.com','hash'),"
                + "(2,'MARIA@Test.com','hash'),"
                + "(3,'user+tag@test.com','hash')");

        // Act
        migrate(schema, "7");

        // Assert
        assertThat(usernames(connection, schema))
            .containsExactlyInAnyOrder("u_juan.perez", "u_maria", "u_usertag");
      }
    }

    @Test
    @DisplayName("Deriva nombres de usuario distintos cuando la parte local exige filtrado")
    void desambiguaNombresDeUsuarioRepetidos() throws Exception {
      // Arrange
      String schema = "v6_backfill_desambigua";
      migrate(schema, "5");
      try (Connection connection = open(schema);
          Statement statement = connection.createStatement()) {
        statement.execute(
            "INSERT INTO users(id,email,password) VALUES (1,'a b@test.com','hash'),"
                + "(2,'ab@test.com','hash')");

        // Act
        migrate(schema, "7");

        // Assert
        List<String> names = usernames(connection, schema);
        assertThat(names)
            .hasSize(2)
            .doesNotHaveDuplicates()
            .allSatisfy(name -> assertThat(name).matches("^[a-z0-9._-]{3,30}$"));
      }
    }
  }

  @Nested
  @DisplayName("Restricciones añadidas a usuarios")
  class Constraints {
    @Test
    @DisplayName("La base de datos rechaza un nombre de usuario repetido")
    void rechazaNombreDeUsuarioDuplicado() throws Exception {
      // Arrange
      String schema = "v6_username_unico";
      migrate(schema, "7");
      try (Connection connection = open(schema);
          Statement statement = connection.createStatement()) {
        statement.execute(
            "INSERT INTO users(email,username,password,role) VALUES "
                + "('first@test.com','mismo','hash','USER')");

        // Act / Assert
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO users(email,username,password,role) VALUES "
                            + "('second@test.com','mismo','hash','USER')"))
            .isInstanceOf(SQLException.class);
      }
    }

    @Test
    @DisplayName("La base de datos rechaza un nombre de usuario fuera del formato admitido")
    void rechazaNombreDeUsuarioFueraDeFormato() throws Exception {
      // Arrange
      String schema = "v6_username_formato";
      migrate(schema, "7");
      try (Connection connection = open(schema);
          Statement statement = connection.createStatement()) {
        // Act / Assert
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO users(email,username,password,role) VALUES "
                            + "('invalid@test.com','No Valido','hash','USER')"))
            .isInstanceOf(SQLException.class);
      }
    }

    @Test
    @DisplayName("La base de datos rechaza un rol fuera de los admitidos")
    void rechazaRolNoAdmitido() throws Exception {
      // Arrange
      String schema = "v6_rol_admitido";
      migrate(schema, "7");
      try (Connection connection = open(schema);
          Statement statement = connection.createStatement()) {
        // Act / Assert
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO users(email,username,password,role) VALUES "
                            + "('rol@test.com','rol.user','hash','ADMIN')"))
            .isInstanceOf(SQLException.class);
      }
    }
  }

  @Nested
  @DisplayName("Singularidad del SUPERUSER")
  class SingleSuperuser {
    @Test
    @DisplayName("La base de datos admite un único usuario con rol SUPERUSER")
    void admiteUnUnicoSuperuser() throws Exception {
      // Arrange
      String schema = "v7_superuser_unico";
      migrate(schema, "7");
      try (Connection connection = open(schema);
          Statement statement = connection.createStatement()) {
        statement.execute(
            "INSERT INTO users(email,username,password,role) VALUES "
                + "('owner@test.com','market.owner','hash','SUPERUSER')");

        // Act / Assert
        assertThatThrownBy(
                () ->
                    statement.execute(
                        "INSERT INTO users(email,username,password,role) VALUES "
                            + "('otro@test.com','otro.owner','hash','SUPERUSER')"))
            .isInstanceOf(SQLException.class);

        // Verify: los usuarios registrados públicamente siguen siendo admitidos.
        statement.execute(
            "INSERT INTO users(email,username,password,role) VALUES "
                + "('publico@test.com','publico.user','hash','USER')");
      }
    }
  }
}
