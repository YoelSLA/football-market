package footballmarket.repositories;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.Player;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.records.LeagueCandidate;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.models.records.TeamCandidate;
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
@Import({TestcontainersConfiguration.class, PlayerReferenceReadGate.class})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class PlayerCatalogPersistenceTest {
  @Autowired private PlayerCatalogService catalog;
  @Autowired private PlayerRepository players;
  @Autowired private TeamRepository teams;
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
    return new PlayerCandidate(id, name, "T", "L", "P", null, null, "100");
  }

  private PlayerSnapshot snapshot(PlayerCandidate... candidates) {
    LeagueCandidate league = new LeagueCandidate("10", "L");
    TeamCandidate team = new TeamCandidate("100", "T", "10", true);
    return new PlayerSnapshot(
        List.of(candidates), candidates.length, 0, List.of(league), List.of(team), List.of());
  }

  @Nested
  @DisplayName("Persistencia del modelo vigente")
  class Persistence {
    @Test
    @DisplayName(
        "La consulta carga equipo y liga fuera de transacción, filtra vigencia y no excluye por casos históricos")
    void queryCurrentCatalogWithDetachedAssociations() {
      // Arrange
      jdbc.update("INSERT INTO leagues(id,name) VALUES (401,'League')");
      jdbc.update(
          "INSERT INTO teams(id,name,league_id,current) VALUES (501,'Current',401,true),(502,'Retired',401,false)");
      jdbc.update(
          "INSERT INTO players(id,name,position,active,team_id) VALUES "
              + "(601,'First','Forward',true,501),"
              + "(602,'Inactive','Forward',false,501),"
              + "(603,'Retired','Forward',true,502),"
              + "(605,'Last','Forward',true,501)");
      jdbc.update(
          "INSERT INTO pending_review_cases(category,cause_code,subject_type,subject_id,case_key,first_detected_at,last_detected_at,evidence) "
              + "VALUES ('PLAYER_TEAM_UNRESOLVED','TEAM_UNRESOLVED','PLAYER',601,'historical',now(),now(),'{}'::jsonb)");

      // Act
      org.springframework.data.domain.Page<Player> first = catalog.getActivePlayers(0, 1);
      org.springframework.data.domain.Page<Player> second = catalog.getActivePlayers(1, 1);
      org.springframework.data.domain.Page<Player> beyond = catalog.getActivePlayers(2, 1);

      // Assert / Verify: no transacción del test ni open-in-view para los objetos devueltos.
      assertThat(first.getContent())
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.getId()).isEqualTo(601L);
                assertThat(player.getTeam().getName()).isEqualTo("Current");
                assertThat(player.getLeague().getName()).isEqualTo("League");
              });
      assertThat(first.getTotalElements()).isEqualTo(2);
      assertThat(second.getContent()).extracting(Player::getId).containsExactly(605L);
      assertThat(beyond.getContent()).isEmpty();
      assertThat(beyond.getTotalElements()).isEqualTo(2);
      jdbc.update("UPDATE players SET active=false");
      assertThat(catalog.getActivePlayers(0, 20).getTotalElements()).isZero();
    }

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
                  ExternalProvider.FOOTBALL_DATA, input.externalId()))
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
      PlayerCandidate input = candidate("44", "Football player");
      catalog.applySynchronization(snapshot(input));
      TransactionTemplate transaction = new TransactionTemplate(transactionManager);
      transaction.executeWithoutResult(
          status -> {
            // Se asocia al equipo vigente de la foto: un equipo ausente se retira y su jugador
            // deja de formar parte del catálogo consultado.
            Long teamId = jdbc.queryForObject("SELECT id FROM teams WHERE name = 'T'", Long.class);
            Player local = new Player("Other origin", teams.getReferenceById(teamId), "P");
            local.addExternalReference(ExternalProvider.THE_SPORTS_DB, "44");
            players.saveAndFlush(local);
          });
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
    @DisplayName(
        "La sincronización conserva identidades WhoScored y la multiplicidad de los demás proveedores")
    void conservaIdentidadWhoScoredYOtrosProveedores() {
      // Arrange
      PlayerCandidate input = candidate("77", "Player with identities");
      catalog.applySynchronization(snapshot(input));
      Long playerId = jdbc.queryForObject("SELECT id FROM players", Long.class);
      TransactionTemplate transaction = new TransactionTemplate(transactionManager);
      transaction.executeWithoutResult(
          status -> {
            Player local = players.findById(playerId).orElseThrow();
            local.addExternalReference(ExternalProvider.THE_SPORTS_DB, "77");
            local.addExternalReference(ExternalProvider.WHOSCORED, "00123");
            players.saveAndFlush(local);
          });

      // Act
      PlayerSynchronizationResult result = catalog.applySynchronization(snapshot(input));

      // Assert
      assertThat(result.updated()).isEqualTo(1);
      assertThat(result.created()).isZero();

      // Verify
      assertThat(references.findByProviderAndExternalId(ExternalProvider.WHOSCORED, "123"))
          .hasValueSatisfying(
              reference -> assertThat(reference.getPlayer().getId()).isEqualTo(playerId));
      assertThat(references.findByProviderAndExternalId(ExternalProvider.THE_SPORTS_DB, "77"))
          .hasValueSatisfying(
              reference -> assertThat(reference.getPlayer().getId()).isEqualTo(playerId));
      assertThat(references.findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, "77"))
          .hasValueSatisfying(
              reference -> assertThat(reference.getPlayer().getId()).isEqualTo(playerId));
      assertThat(jdbc.queryForObject("SELECT count(*) FROM players", Integer.class)).isEqualTo(1);
      assertThat(
              jdbc.queryForObject("SELECT count(*) FROM player_external_references", Integer.class))
          .isEqualTo(3);
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
              .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, "race")
              .orElseThrow()
              .getPlayer()
              .getId();
      Long otherId =
          references
              .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, "other")
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
    @ExtendWith(OutputCaptureExtension.class)
    void recuperaConflictoConcurrente(boolean technicalFailureAfterReplay, CapturedOutput output)
        throws Exception {
      // Arrange: la otra transacción conserva sin confirmar una referencia que la foto no ve.
      jdbc.update("INSERT INTO leagues(name) VALUES ('Competing league')");
      jdbc.update(
          "INSERT INTO teams(name,league_id,current) SELECT 'Competing team', id, true FROM leagues "
              + "WHERE name='Competing league'");
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
            INSERT INTO players(name,position,active,team_id)
            SELECT 'Concurrent owner','P',true, id FROM teams WHERE name='Competing team'
            RETURNING id
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
          assertThat(references.findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, "race"))
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
