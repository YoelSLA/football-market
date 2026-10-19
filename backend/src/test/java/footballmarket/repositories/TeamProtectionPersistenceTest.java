package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.models.records.LeagueCandidate;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.models.records.TeamCandidate;
import footballmarket.services.PlayerCatalogService;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

/** Atomicidad, conflictos UNIQUE y filtro de vigencia mediante persistencia real de testing. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class TeamProtectionPersistenceTest {
  private static final long LOCK_KEY = 552211;
  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  @Autowired private PlayerCatalogService catalog;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private DataSource dataSource;
  @Autowired private PlayerRepository players;

  private PlayerCandidate candidate(String id, String name, String teamId, String teamName) {
    return new PlayerCandidate(id, name, teamName, "League", "Forward", null, null, teamId);
  }

  private PlayerSnapshot initialPhoto() {
    PlayerCandidate player = candidate("1", "Player", "101", "Team A");
    LeagueCandidate league = new LeagueCandidate("10", "League");
    TeamCandidate first = new TeamCandidate("101", "Team A", "10", true);
    TeamCandidate second = new TeamCandidate("102", "Team B", "10", true);
    return new PlayerSnapshot(
        List.of(player), 1, 0, List.of(league), List.of(first, second), List.of());
  }

  private Map<String, Object> protectedState() {
    return jdbc.queryForMap(
        """
        SELECT t.id, t.name, t.league_id, t.current, l.name AS league_name,
               p.id AS player_id, p.team_id, p.active, p.name AS player_name, p.position
        FROM teams t JOIN team_external_references r ON r.team_id = t.id
        JOIN leagues l ON l.id = t.league_id
        JOIN players p ON p.team_id = t.id
        JOIN player_external_references pr ON pr.player_id = p.id
        WHERE r.provider = 'FOOTBALL_DATA' AND r.external_id = '200'
          AND pr.provider = 'FOOTBALL_DATA' AND pr.external_id = '3'
        """);
  }

  /** Observa el bloqueo real; el intervalo de sondeo no decide cuándo confirmar la competidora. */
  private void awaitBlockedAttempt(int ownerPid, Future<?> attempt) throws Exception {
    long deadline = System.nanoTime() + TIMEOUT.toNanos();
    while (System.nanoTime() < deadline) {
      Boolean blocked =
          jdbc.queryForObject(
              """
              SELECT EXISTS (
                SELECT 1 FROM pg_locks
                WHERE locktype = 'advisory' AND classid::bigint = 0 AND objid::bigint = ? AND objsubid = 1
                  AND NOT granted AND pid <> ? AND ? = ANY(pg_blocking_pids(pid))
              )
              """,
              Boolean.class,
              LOCK_KEY,
              ownerPid,
              ownerPid);
      if (Boolean.TRUE.equals(blocked)) {
        return;
      }
      if (attempt.isDone()) {
        attempt.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        throw new AssertionError("El intento finalizó sin alcanzar el bloqueo esperado");
      }
      if (Thread.currentThread().isInterrupted()) {
        throw new InterruptedException("Se interrumpió la observación del bloqueo");
      }
      LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
    }
    throw new AssertionError("El intento no alcanzó el bloqueo advisory dentro del timeout");
  }

  @Nested
  @DisplayName("Rollback de fotografías incompletas")
  class IncompletePhotos {
    @Test
    @DisplayName("La liga ausente revierte dominios parciales y conserva el catálogo completo")
    void missingLeagueRollsBackEveryChange() {
      catalog.applySynchronization(initialPhoto());
      List<Map<String, Object>> oldTeams = jdbc.queryForList("SELECT * FROM teams ORDER BY id");
      List<Map<String, Object>> oldLeagues = jdbc.queryForList("SELECT * FROM leagues ORDER BY id");
      List<Map<String, Object>> oldPlayers = jdbc.queryForList("SELECT * FROM players ORDER BY id");
      List<Map<String, Object>> oldReferences =
          jdbc.queryForList("SELECT * FROM team_external_references ORDER BY id");
      List<Map<String, Object>> oldLeagueReferences =
          jdbc.queryForList("SELECT * FROM league_external_references ORDER BY id");
      List<Map<String, Object>> oldPlayerReferences =
          jdbc.queryForList("SELECT * FROM player_external_references ORDER BY id");
      List<Map<String, Object>> oldCases =
          jdbc.queryForList("SELECT * FROM pending_review_cases ORDER BY id");
      LeagueCandidate newLeague = new LeagueCandidate("20", "Temporary league");
      TeamCandidate newTeam = new TeamCandidate("201", "Temporary team", "20", true);
      TeamCandidate orphan = new TeamCandidate("999", "Orphan Team", "404", true);
      PlayerSnapshot incomplete =
          new PlayerSnapshot(
              List.of(), 0, 0, List.of(newLeague), List.of(newTeam, orphan), List.of());

      PlayerSynchronizationResult result = catalog.applySynchronization(incomplete);

      assertThat(result).isEqualTo(new PlayerSynchronizationResult(0, 0, 0, 0, 0));
      assertThat(jdbc.queryForList("SELECT * FROM teams ORDER BY id"))
          .isEqualTo(oldTeams)
          .hasSize(2);
      assertThat(jdbc.queryForList("SELECT * FROM leagues ORDER BY id"))
          .isEqualTo(oldLeagues)
          .hasSize(1);
      assertThat(jdbc.queryForList("SELECT * FROM players ORDER BY id")).isEqualTo(oldPlayers);
      assertThat(jdbc.queryForList("SELECT * FROM team_external_references ORDER BY id"))
          .isEqualTo(oldReferences);
      assertThat(jdbc.queryForList("SELECT * FROM league_external_references ORDER BY id"))
          .isEqualTo(oldLeagueReferences);
      assertThat(jdbc.queryForList("SELECT * FROM player_external_references ORDER BY id"))
          .isEqualTo(oldPlayerReferences);
      assertThat(jdbc.queryForList("SELECT * FROM pending_review_cases ORDER BY id"))
          .isEqualTo(oldCases)
          .isEmpty();
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM teams WHERE name = 'Orphan Team'", Integer.class))
          .isZero();
    }
  }

  @Nested
  @DisplayName("Filtro de equipos retirados")
  class RetiredTeams {
    @Test
    @DisplayName("La consulta excluye un jugador activo cuyo equipo continúa retirado")
    void excludeRetiredTeamFromCatalog() {
      catalog.applySynchronization(initialPhoto());
      Map<String, Object> original = jdbc.queryForMap("SELECT id, team_id FROM players");
      Object playerId = original.get("id");
      Object teamId = original.get("team_id");
      LeagueCandidate league = new LeagueCandidate("10", "League");
      TeamCandidate remaining = new TeamCandidate("102", "Team B", "10", true);
      PlayerSnapshot absent =
          new PlayerSnapshot(List.of(), 0, 0, List.of(league), List.of(remaining), List.of());

      catalog.applySynchronization(absent);

      assertThat(
              jdbc.queryForObject("SELECT current FROM teams WHERE id = ?", Boolean.class, teamId))
          .isFalse();
      assertThat(jdbc.update("UPDATE players SET active = true WHERE id = ?", playerId))
          .isEqualTo(1);
      assertThat(
              jdbc.queryForObject(
                  "SELECT active FROM players WHERE id = ?", Boolean.class, playerId))
          .isTrue();
      assertThat(
              jdbc.queryForObject("SELECT current FROM teams WHERE id = ?", Boolean.class, teamId))
          .isFalse();
      assertThat(players.findByActiveTrue(org.springframework.data.domain.PageRequest.of(0, 20)))
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("Conflictos recuperables de liga")
  class RecoverableLeagueConflicts {
    @Test
    @DisplayName(
        "El conflicto UNIQUE observado protege todo el estado del equipo sin abstener la foto")
    void excludedLeagueKeepsPhotoApplicable() throws Exception {
      catalog.applySynchronization(initialPhoto());
      PlayerCandidate racer = candidate("3", "Racer", "200", "Team C");
      LeagueCandidate league = new LeagueCandidate("10", "League");
      TeamCandidate affected = new TeamCandidate("200", "Team C", "10", true);
      PlayerSnapshot previous =
          new PlayerSnapshot(List.of(racer), 1, 0, List.of(league), List.of(affected), List.of());
      catalog.applySynchronization(previous);
      Map<String, Object> before = protectedState();
      List<Map<String, Object>> referencesBefore =
          jdbc.queryForList("SELECT * FROM player_external_references ORDER BY id");
      List<Map<String, Object>> teamReferencesBefore =
          jdbc.queryForList("SELECT * FROM team_external_references ORDER BY id");
      PlayerCandidate regular = candidate("1", "Player", "101", "Team A");
      LeagueCandidate raced = new LeagueCandidate("race", "Raced league");
      TeamCandidate normal = new TeamCandidate("101", "Team A", "10", true);
      TeamCandidate changed = new TeamCandidate("200", "Changed Team C", "race", true);
      PlayerSnapshot photo =
          new PlayerSnapshot(
              List.of(regular, racer),
              2,
              0,
              List.of(league, raced),
              List.of(normal, changed),
              List.of());
      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<PlayerSynchronizationResult> attempt = null;
      try {
        jdbc.execute(
            """
            CREATE FUNCTION test_league_reference_gate() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN
              IF NEW.external_id = 'race' THEN PERFORM pg_advisory_xact_lock(552211); END IF;
              RETURN NEW;
            END $$
            """);
        jdbc.execute(
            """
            CREATE TRIGGER test_league_reference_gate BEFORE INSERT ON league_external_references
            FOR EACH ROW EXECUTE FUNCTION test_league_reference_gate()
            """);
        try (Connection competing = dataSource.getConnection();
            Statement statement = competing.createStatement()) {
          statement.setQueryTimeout((int) TIMEOUT.toSeconds());
          competing.setAutoCommit(false);
          statement.execute("SET LOCAL lock_timeout = '30s'");
          statement.execute("SELECT pg_advisory_xact_lock(552211)");
          int ownerPid;
          try (java.sql.ResultSet rows = statement.executeQuery("SELECT pg_backend_pid()")) {
            rows.next();
            ownerPid = rows.getInt(1);
          }
          statement.execute("INSERT INTO leagues(name) VALUES ('Raced league')");
          statement.execute(
              "INSERT INTO league_external_references(league_id,provider,external_id) "
                  + "SELECT id,'FOOTBALL_DATA','race' FROM leagues WHERE name='Raced league'");
          attempt = executor.submit(() -> catalog.applySynchronization(photo));
          awaitBlockedAttempt(ownerPid, attempt);
          competing.commit();
          PlayerSynchronizationResult result = attempt.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);

          assertThat(result.updated()).isEqualTo(1);
          assertThat(result.markedInactive()).isZero();
          assertThat(result.discardedInvalid()).isEqualTo(1);
          assertThat(protectedState()).isEqualTo(before);
          assertThat(jdbc.queryForList("SELECT * FROM player_external_references ORDER BY id"))
              .isEqualTo(referencesBefore);
          assertThat(jdbc.queryForList("SELECT * FROM team_external_references ORDER BY id"))
              .isEqualTo(teamReferencesBefore);
          assertThat(
                  jdbc.queryForObject(
                      "SELECT current FROM teams WHERE name = 'Team A'", Boolean.class))
              .isTrue();
          assertThat(
                  jdbc.queryForObject(
                      "SELECT active FROM players WHERE name = 'Player'", Boolean.class))
              .isTrue();
          assertThat(
                  jdbc.queryForObject(
                      "SELECT count(*) FROM pending_review_cases WHERE category = 'TEAM_LEAGUE_UNRESOLVED'",
                      Integer.class))
              .isEqualTo(1);
          assertThat(
                  jdbc.queryForObject(
                      "SELECT subject_id FROM pending_review_cases WHERE category = 'TEAM_LEAGUE_UNRESOLVED' "
                          + "AND cause_code = 'LEAGUE_UNRESOLVED' AND subject_external_id = '200'",
                      Long.class))
              .isEqualTo(before.get("id"));
        }
      } finally {
        if (attempt != null) {
          attempt.cancel(true);
        }
        executor.shutdownNow();
        try {
          if (!executor.awaitTermination(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
            throw new AssertionError(
                "El intento concurrente no terminó dentro del timeout de limpieza");
          }
        } finally {
          try {
            jdbc.execute(
                "DROP TRIGGER IF EXISTS test_league_reference_gate ON league_external_references");
          } finally {
            jdbc.execute("DROP FUNCTION IF EXISTS test_league_reference_gate()");
          }
        }
      }
    }
  }
}
