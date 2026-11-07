package footballmarket.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("unit")
class FlywayMigrationPolicyTest {
  @TempDir Path temporaryDirectory;

  private ProcessResult run(Path directory, String... command) throws Exception {
    Process process =
        new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes());
    return new ProcessResult(process.waitFor(), output);
  }

  private Path repository() throws Exception {
    Path repository = temporaryDirectory.resolve("repository");
    Path migrations = repository.resolve("backend/src/main/resources/db/migration");
    Path hooks = repository.resolve(".githooks");
    Files.createDirectories(migrations);
    Files.createDirectories(hooks);
    Files.copy(
        Path.of("..").resolve(".githooks/flyway-migrations.sh").normalize(),
        hooks.resolve("flyway-migrations.sh"));
    Files.writeString(
        migrations.resolve("V20261107030001__create_users.sql"),
        "CREATE TABLE users(id BIGINT);\n");
    assertThat(run(repository, "git", "init", "-q").exitCode()).isZero();
    assertThat(run(repository, "git", "config", "user.email", "test@example.com").exitCode())
        .isZero();
    assertThat(run(repository, "git", "config", "user.name", "Flyway Test").exitCode()).isZero();
    assertThat(run(repository, "git", "add", ".").exitCode()).isZero();
    assertThat(run(repository, "git", "commit", "-qm", "baseline").exitCode()).isZero();
    assertThat(run(repository, "git", "branch", "dev").exitCode()).isZero();
    return repository;
  }

  @Nested
  @DisplayName("Inmutabilidad de migraciones integradas")
  class Immutability {
    @Test
    @DisplayName("Rechaza modificar una migración presente en dev")
    void rejectsModification() throws Exception {
      Path repository = repository();
      Files.writeString(
          repository.resolve(
              "backend/src/main/resources/db/migration/V20261107030001__create_users.sql"),
          "CREATE TABLE users(id INTEGER);\n");
      assertThat(run(repository, "git", "add", ".").exitCode()).isZero();
      assertThat(run(repository, "git", "commit", "-qm", "modify").exitCode()).isZero();

      ProcessResult result =
          run(repository, "bash", ".githooks/flyway-migrations.sh", "dev", "HEAD");

      assertThat(result.exitCode()).isEqualTo(1);
      assertThat(result.output()).contains("Migración integrada alterada");
    }

    @Test
    @DisplayName("Rechaza eliminar una migración presente en dev")
    void rejectsDeletion() throws Exception {
      Path repository = repository();
      Files.delete(
          repository.resolve(
              "backend/src/main/resources/db/migration/V20261107030001__create_users.sql"));
      assertThat(run(repository, "git", "add", "-A").exitCode()).isZero();
      assertThat(run(repository, "git", "commit", "-qm", "delete").exitCode()).isZero();

      ProcessResult result =
          run(repository, "bash", ".githooks/flyway-migrations.sh", "dev", "HEAD");

      assertThat(result.exitCode()).isEqualTo(1);
      assertThat(result.output()).contains("Migración integrada alterada");
    }

    @Test
    @DisplayName("Rechaza renombrar una migración presente en dev")
    void rejectsRename() throws Exception {
      Path repository = repository();
      Path migrations = repository.resolve("backend/src/main/resources/db/migration");
      Files.move(
          migrations.resolve("V20261107030001__create_users.sql"),
          migrations.resolve("V20261107030002__create_users.sql"));
      assertThat(run(repository, "git", "add", "-A").exitCode()).isZero();
      assertThat(run(repository, "git", "commit", "-qm", "rename").exitCode()).isZero();

      ProcessResult result =
          run(repository, "bash", ".githooks/flyway-migrations.sh", "dev", "HEAD");

      assertThat(result.exitCode()).isEqualTo(1);
      assertThat(result.output()).contains("Migración integrada alterada");
    }

    @Test
    @DisplayName("Admite una migración nueva posterior a la máxima de dev")
    void acceptsLaterMigration() throws Exception {
      Path repository = repository();
      Files.writeString(
          repository.resolve(
              "backend/src/main/resources/db/migration/V20261107030002__create_leagues.sql"),
          "CREATE TABLE leagues(id BIGINT);\n");
      assertThat(run(repository, "git", "add", ".").exitCode()).isZero();
      assertThat(run(repository, "git", "commit", "-qm", "add").exitCode()).isZero();

      ProcessResult result =
          run(repository, "bash", ".githooks/flyway-migrations.sh", "dev", "HEAD");

      assertThat(result.exitCode()).isZero();
    }
  }

  private record ProcessResult(int exitCode, String output) {}
}
