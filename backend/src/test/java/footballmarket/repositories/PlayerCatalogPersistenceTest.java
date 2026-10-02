package footballmarket.repositories;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.Player;
import footballmarket.models.enums.PlayerProvider;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.services.PlayerCatalogService;
import footballmarket.services.exceptions.PlayerSynchronizationPersistenceException;
import footballmarket.support.PlayerReferenceReadGate;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pruebas de persistencia y concurrencia real; no sustituyen las pruebas funcionales del Service.
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@Import({TestcontainersConfiguration.class, PlayerReferenceReadGate.class})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class PlayerCatalogPersistenceTest {
  @Autowired private PlayerCatalogService catalog;
  @Autowired private PlayerRepository players;
  @Autowired private PlayerExternalReferenceRepository references;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbc;

  @AfterEach
  void removeRaceGate() {
    PlayerReferenceReadGate.reset();
    jdbc.execute("DROP TRIGGER IF EXISTS test_reference_gate ON player_external_references");
    jdbc.execute("DROP FUNCTION IF EXISTS test_reference_gate()");
  }

  private PlayerCandidate candidate(String id, String name) {
    return new PlayerCandidate(id, name, "T", "L", "P", null, null);
  }

  private PlayerSnapshot snapshot(PlayerCandidate... candidates) {
    return new PlayerSnapshot(List.of(candidates), candidates.length, 0);
  }

  @Nested
  @DisplayName("Persistencia del modelo vigente")
  class Persistence {
    @Test
    @DisplayName("Un error diferido al commit sin candidato atribuible es técnico y revierte todo")
    void revierteErrorSinCandidatoEnCommit() {
      jdbc.execute(
          "ALTER TABLE players ADD CONSTRAINT test_deferred_name UNIQUE(name) DEFERRABLE INITIALLY DEFERRED");
      try {
        PlayerCandidate first = candidate("1", "Same");
        PlayerCandidate second = candidate("2", "Same");
        assertThatThrownBy(() -> catalog.applySynchronization(snapshot(first, second)))
            .isInstanceOf(PlayerSynchronizationPersistenceException.class);
        assertThat(players.count()).isZero();
        assertThat(references.count()).isZero();
      } finally {
        jdbc.execute("ALTER TABLE players DROP CONSTRAINT test_deferred_name");
      }
    }

    @Test
    @DisplayName("Genera IDs internos y persiste el alta conjunta sin confundir el ID externo")
    void guardaIdentidadesSeparadas() {
      PlayerCandidate input = candidate("900000000000", "N");
      catalog.applySynchronization(snapshot(input));
      assertThat(
              references.findByProviderAndExternalId(
                  PlayerProvider.FOOTBALL_DATA, input.externalId()))
          .hasValueSatisfying(
              reference -> {
                assertThat(reference.getPlayer().getId()).isNotEqualTo(900000000000L);
                assertThat(reference.getExternalId()).isEqualTo("900000000000");
              });
      assertThat(jdbc.queryForObject("SELECT count(*) FROM players", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("No inactiva jugadores sin referencia Football-Data ni cambia imágenes existentes")
    void protegeOtrosOrigenesEImagenes() {
      TransactionTemplate transaction = new TransactionTemplate(transactionManager);
      transaction.executeWithoutResult(
          status -> {
            Player local = new Player("Other origin", "T", "L", "P");
            local.addExternalReference(PlayerProvider.THE_SPORTS_DB, "44");
            players.saveAndFlush(local);
          });
      PlayerCandidate input = candidate("44", "Football player");
      catalog.applySynchronization(snapshot(input));
      jdbc.update(
          "UPDATE players SET image_url='https://images.example/portrait.jpg' WHERE name='Football player'");
      catalog.applySynchronization(snapshot(input));
      assertThat(catalog.getActivePlayers(0, 20).getContent()).hasSize(2);
      assertThat(
              jdbc.queryForObject(
                  "SELECT image_url FROM players WHERE name='Football player'", String.class))
          .isEqualTo("https://images.example/portrait.jpg");
      catalog.applySynchronization(snapshot());
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .extracting(Player::getName)
          .containsExactly("Other origin");
    }

    @Test
    @DisplayName("Una UNIQUE distinta es un fallo técnico y revierte toda la foto")
    void noDescartaOtraConstraint() {
      jdbc.execute("CREATE UNIQUE INDEX test_unique_player_name ON players(name)");
      try {
        PlayerCandidate first = candidate("1", "Same");
        PlayerCandidate second = candidate("2", "Same");
        assertThatThrownBy(() -> catalog.applySynchronization(snapshot(first, second)))
            .isInstanceOf(PlayerSynchronizationPersistenceException.class);
        assertThat(players.count()).isZero();
        assertThat(references.count()).isZero();
      } finally {
        jdbc.execute("DROP INDEX test_unique_player_name");
      }
    }
  }

  @Nested
  @DisplayName("Conflictos concurrentes de la referencia externa")
  class Concurrency {
    @Test
    @DisplayName(
        "Un cambio de propietario detectado antes de escribir descarta y protege a ambos jugadores")
    void descartaConflictoLogicoAntesDePersistir() throws Exception {
      PlayerCandidate first = candidate("race", "Original owner");
      PlayerCandidate second = candidate("other", "Other owner");
      catalog.applySynchronization(snapshot(first, second));
      Long originalId =
          references
              .findByProviderAndExternalId(PlayerProvider.FOOTBALL_DATA, "race")
              .orElseThrow()
              .getPlayer()
              .getId();
      Long otherId =
          references
              .findByProviderAndExternalId(PlayerProvider.FOOTBALL_DATA, "other")
              .orElseThrow()
              .getPlayer()
              .getId();
      PlayerCandidate conflicting = candidate("race", "Must not overwrite");
      PlayerCandidate good = candidate("good", "Good");
      PlayerSnapshot photo = snapshot(conflicting, good);
      try (ExecutorService executor = Executors.newSingleThreadExecutor();
          PlayerReferenceReadGate.Gate gate = PlayerReferenceReadGate.arm()) {
        Future<PlayerSynchronizationResult> execution =
            executor.submit(() -> catalog.applySynchronization(photo));
        assertThat(gate.awaitRead()).isTrue();
        // Simula una asociación incompatible externa a la sincronización entre sus dos lecturas.
        jdbc.update(
            "UPDATE player_external_references SET player_id=? WHERE provider='FOOTBALL_DATA' AND external_id='race'",
            otherId);
        gate.close();
        PlayerSynchronizationResult result = execution.get(15, TimeUnit.SECONDS);
        assertThat(result.discardedInvalid()).isEqualTo(1);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.markedInactive()).isZero();
        assertThat(players.findById(originalId))
            .hasValueSatisfying(
                player -> {
                  assertThat(player.getName()).isEqualTo("Original owner");
                  assertThat(player.isActive()).isTrue();
                });
        assertThat(players.findById(otherId))
            .hasValueSatisfying(
                player -> {
                  assertThat(player.getName()).isEqualTo("Other owner");
                  assertThat(player.isActive()).isTrue();
                });
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Aísla la UNIQUE concurrente y preserva atomicidad si después ocurre otro fallo")
    void recuperaConflictoConcurrente(boolean technicalFailureAfterReplay, CapturedOutput output)
        throws Exception {
      // Arrange: la otra transacción conserva sin confirmar una referencia que la foto no ve.
      jdbc.execute(
          """
          CREATE FUNCTION test_reference_gate() RETURNS trigger LANGUAGE plpgsql AS $$
          BEGIN
            IF NEW.external_id = 'race' THEN PERFORM pg_advisory_xact_lock(771944); END IF;
            RETURN NEW;
          END $$
          """);
      jdbc.execute(
          """
          CREATE TRIGGER test_reference_gate BEFORE INSERT ON player_external_references
          FOR EACH ROW EXECUTE FUNCTION test_reference_gate()
          """);
      PlayerCandidate good = candidate("good", "Good");
      PlayerCandidate race = candidate("race", "Must not replace owner");
      PlayerCandidate last =
          candidate("last", technicalFailureAfterReplay ? "X".repeat(256) : "Last");
      PlayerSnapshot photo = snapshot(good, race, last);
      try (Connection competing = dataSource.getConnection();
          Statement statement = competing.createStatement();
          ExecutorService executor = Executors.newSingleThreadExecutor()) {
        competing.setAutoCommit(false);
        statement.execute("SELECT pg_advisory_xact_lock(771944)");
        long owner;
        try (ResultSet row =
            statement.executeQuery(
                """
            INSERT INTO players(name,team,league,position,active)
            VALUES ('Concurrent owner','T','L','P',true) RETURNING id
            """)) {
          row.next();
          owner = row.getLong(1);
        }
        statement.execute(
            "INSERT INTO player_external_references(player_id,provider,external_id) VALUES ("
                + owner
                + ",'FOOTBALL_DATA','race')");

        // Act: el intento alcanza el INSERT tras haber escrito el primer candidato.
        Future<PlayerSynchronizationResult> execution =
            executor.submit(() -> catalog.applySynchronization(photo));
        try {
          awaitBlockedInsert();
          assertThat(
                  jdbc.queryForObject(
                      "SELECT count(*) FROM players WHERE name='Good'", Integer.class))
              .isZero();
          competing.commit();
          if (technicalFailureAfterReplay) {
            assertThatThrownBy(() -> execution.get(15, TimeUnit.SECONDS))
                .hasCauseInstanceOf(PlayerSynchronizationPersistenceException.class);
            assertThat(players.count()).isEqualTo(1);
            assertThat(references.count()).isEqualTo(1);
          } else {
            PlayerSynchronizationResult result = execution.get(15, TimeUnit.SECONDS);
            assertThat(result.created()).isEqualTo(2);
            assertThat(result.updated()).isZero();
            assertThat(result.discardedInvalid()).isEqualTo(1);
            assertThat(result.markedInactive()).isZero();
            assertThat(players.count()).isEqualTo(3);
            assertThat(references.count()).isEqualTo(3);
          }
          assertThat(players.findById(owner))
              .hasValueSatisfying(
                  player -> {
                    assertThat(player.getName()).isEqualTo("Concurrent owner");
                    assertThat(player.isActive()).isTrue();
                  });
          assertThat(references.findByProviderAndExternalId(PlayerProvider.FOOTBALL_DATA, "race"))
              .hasValueSatisfying(
                  reference -> assertThat(reference.getPlayer().getId()).isEqualTo(owner));
          assertThat(
                  output
                      .getAll()
                      .split("Jugador descartado: conflicto de referencia FOOTBALL_DATA", -1))
              .hasSize(2);
          assertThat(output.getAll().split("Sincronización confirmada:", -1))
              .hasSize(technicalFailureAfterReplay ? 1 : 2);
        } finally {
          competing.rollback();
        }
      }
    }
  }

  /**
   * Espera la barrera real de PostgreSQL con límite; no depende de pausas arbitrarias de escritura.
   */
  private void awaitBlockedInsert() throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    while (System.nanoTime() < deadline) {
      Integer waiting =
          jdbc.queryForObject(
              "SELECT count(*) FROM pg_locks WHERE locktype='advisory' AND NOT granted AND objid=771944",
              Integer.class);
      if (waiting != null && waiting > 0) {
        return;
      }
      Thread.sleep(10);
    }
    throw new AssertionError("El intento no alcanzó la barrera de concurrencia");
  }
}
