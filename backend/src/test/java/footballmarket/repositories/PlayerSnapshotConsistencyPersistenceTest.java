package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.records.LeagueCandidate;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.models.records.TeamCandidate;
import footballmarket.services.PlayerCatalogService;
import footballmarket.services.exceptions.PlayerSynchronizationPersistenceException;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Regresiones técnicas de conservación y atomicidad sobre cardinalidad legacy permitida por V4. SQL
 * prepara exclusivamente ese estado histórico y observa referencias/casos/espejo no expuestos por
 * APIs productivas; no se agrega una API de producto para fabricar referencias en tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class PlayerSnapshotConsistencyPersistenceTest {
  @Autowired private PlayerCatalogService catalog;
  @Autowired private JdbcTemplate jdbc;

  private Long playerId;

  @BeforeEach
  void prepareLegacyCardinality() {
    PlayerCandidate original = candidate("7001", "Original", "101");
    catalog.applySynchronization(snapshot(original));
    playerId = catalog.getActivePlayers(0, 20).getContent().getFirst().getId();
    jdbc.update(
        "INSERT INTO player_external_references(player_id,provider,external_id) VALUES (?, 'FOOTBALL_DATA', '7002')",
        playerId);
  }

  private PlayerCandidate candidate(String externalId, String name, String teamId) {
    return new PlayerCandidate(
        externalId,
        name,
        "101".equals(teamId) ? "Team A" : "Team B",
        "League",
        "Forward",
        null,
        null,
        teamId);
  }

  private PlayerCandidate optionalCandidate(
      String externalId, java.time.LocalDate dateOfBirth, String nationality) {
    return new PlayerCandidate(
        externalId, "Original", "Team A", "League", "Forward", dateOfBirth, nationality, "101");
  }

  private PlayerSnapshot snapshot(PlayerCandidate... candidates) {
    LeagueCandidate league = new LeagueCandidate("10", "League");
    TeamCandidate first = new TeamCandidate("101", "Team A", "10", true);
    TeamCandidate second = new TeamCandidate("102", "Team B", "10", true);
    return new PlayerSnapshot(
        List.of(candidates),
        candidates.length,
        0,
        List.of(league),
        List.of(first, second),
        List.of());
  }

  private Map<String, Object> persistedPlayer() {
    return jdbc.queryForMap(
        "SELECT id,name,team_id,team,league,position,active,date_of_birth,nationality,image_url FROM players WHERE id = ?",
        playerId);
  }

  private List<Map<String, Object>> persistedReferences() {
    return jdbc.queryForList(
        "SELECT id,player_id,provider,external_id FROM player_external_references WHERE player_id = ? ORDER BY id",
        playerId);
  }

  @Nested
  @DisplayName("Conservación ante asociaciones contradictorias del mismo propietario")
  class ConflictingAssociations {
    @ParameterizedTest
    @CsvSource({"Different,Forward", "Original,Goalkeeper", "Different,Goalkeeper"})
    @DisplayName(
        "Nombre, posición o ambos contradictorios protegen integralmente al sujeto con un único caso")
    void preserveContradictorySubjectState(String secondName, String secondPosition) {
      // Arrange
      Map<String, Object> before = persistedPlayer();
      List<Map<String, Object>> referencesBefore = persistedReferences();
      PlayerCandidate first = candidate("7001", "Original", "101");
      PlayerCandidate second =
          new PlayerCandidate(
              "7002", secondName, "Team A", "League", secondPosition, null, null, "101");
      PlayerCandidate other = candidate("7003", "Other player", "102");
      PlayerSnapshot forward = snapshot(first, second, other);
      PlayerSnapshot reverse = snapshot(second, first, other);

      // Act
      PlayerSynchronizationResult result = catalog.applySynchronization(forward);
      Map<String, Object> review =
          jdbc.queryForMap(
              "SELECT id,case_key,first_detected_at,evidence::text FROM pending_review_cases");
      catalog.applySynchronization(reverse);

      // Assert
      assertThat(result.created()).isEqualTo(1);
      assertThat(result.updated()).isZero();
      assertThat(result.markedInactive()).isZero();
      assertThat(result.discardedInvalid()).isEqualTo(2);

      // Verify
      assertThat(persistedPlayer()).isEqualTo(before);
      assertThat(persistedReferences()).isEqualTo(referencesBefore);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM pending_review_cases", Integer.class))
          .isEqualTo(1);
      assertThat(jdbc.queryForObject("SELECT cause_code FROM pending_review_cases", String.class))
          .isEqualTo("CONFLICTING_PLAYER_STATE");
      assertThat(
              jdbc.queryForMap(
                  "SELECT id,case_key,first_detected_at,evidence::text FROM pending_review_cases"))
          .isEqualTo(review);
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .extracting(player -> player.getName())
          .containsExactly("Original", "Other player");
    }

    @Test
    @DisplayName(
        "El conflicto impide refrescar el espejo del Player aunque su Team o League cambien de nombre")
    void preserveMirrorDuringRename() {
      // Arrange
      Map<String, Object> before = persistedPlayer();
      PlayerCandidate first = candidate("7001", "Changed A", "101");
      PlayerCandidate second = candidate("7002", "Changed B", "102");
      LeagueCandidate league = new LeagueCandidate("10", "Renamed league");
      TeamCandidate firstTeam = new TeamCandidate("101", "Renamed team A", "10", true);
      TeamCandidate secondTeam = new TeamCandidate("102", "Team B", "10", true);
      PlayerSnapshot conflict =
          new PlayerSnapshot(
              List.of(first, second),
              2,
              0,
              List.of(league),
              List.of(firstTeam, secondTeam),
              List.of());

      // Act
      PlayerSynchronizationResult result = catalog.applySynchronization(conflict);

      // Assert
      assertThat(result.updated()).isZero();
      assertThat(result.discardedInvalid()).isEqualTo(2);

      // Verify
      assertThat(persistedPlayer()).isEqualTo(before);
      assertThat(
              jdbc.queryForObject(
                  "SELECT name FROM teams WHERE id = ?", String.class, before.get("team_id")))
          .isEqualTo("Renamed team A");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName(
        "Conserva todo el Player activo o inactivo y registra un caso interno sin cambiar referencias")
    void preserveConflictingPlayer(boolean active) {
      // Arrange
      jdbc.update("UPDATE players SET active = ? WHERE id = ?", active, playerId);
      Map<String, Object> before = persistedPlayer();
      List<Map<String, Object>> referencesBefore = persistedReferences();
      PlayerCandidate first = candidate("7001", "Must not overwrite A", "101");
      PlayerCandidate second = candidate("7002", "Must not overwrite B", "102");
      PlayerSnapshot conflict = snapshot(first, second);

      // Act
      PlayerSynchronizationResult result = catalog.applySynchronization(conflict);

      // Assert
      assertThat(result.obtained()).isEqualTo(2);
      assertThat(result.created()).isZero();
      assertThat(result.updated()).isZero();
      assertThat(result.markedInactive()).isZero();
      assertThat(result.discardedInvalid()).isEqualTo(2);

      // Verify
      assertThat(persistedPlayer()).isEqualTo(before);
      assertThat(persistedReferences()).isEqualTo(referencesBefore);
      Map<String, Object> review =
          jdbc.queryForMap(
              "SELECT category,cause_code,subject_type,subject_id,subject_provider,subject_external_id FROM pending_review_cases");
      assertThat(review)
          .containsEntry("category", "INVALID_SUBJECT_DATA")
          .containsEntry("cause_code", "CONFLICTING_PLAYER_TEAMS")
          .containsEntry("subject_type", "PLAYER")
          .containsEntry("subject_id", playerId)
          .containsEntry("subject_provider", null)
          .containsEntry("subject_external_id", null);
      assertThat(
              jdbc.queryForObject(
                  "SELECT evidence->>'previousActive' FROM pending_review_cases", String.class))
          .isEqualTo(Boolean.toString(active));
      assertThat(
              jdbc.queryForObject(
                  "SELECT jsonb_array_length(evidence->'observations') FROM pending_review_cases",
                  Integer.class))
          .isEqualTo(2);
    }

    @Test
    @DisplayName(
        "Invertir referencias conserva resultado, evidencia y primera detección sin duplicar el caso")
    void preserveOrderIndependence() {
      // Arrange
      PlayerCandidate first = candidate("7001", "Changed A", "101");
      PlayerCandidate second = candidate("7002", "Changed B", "102");
      PlayerSnapshot forward = snapshot(first, second);
      PlayerSnapshot reverse = snapshot(second, first);
      Map<String, Object> before = persistedPlayer();
      List<Map<String, Object>> referencesBefore = persistedReferences();
      PlayerSynchronizationResult forwardResult = catalog.applySynchronization(forward);
      Map<String, Object> firstReview =
          jdbc.queryForMap(
              "SELECT id,case_key,first_detected_at,evidence::text FROM pending_review_cases");

      // Act
      PlayerSynchronizationResult reverseResult = catalog.applySynchronization(reverse);

      // Assert
      assertThat(reverseResult).isEqualTo(forwardResult);

      // Verify
      assertThat(persistedPlayer()).isEqualTo(before);
      assertThat(persistedReferences()).isEqualTo(referencesBefore);
      assertThat(
              jdbc.queryForMap(
                  "SELECT id,case_key,first_detected_at,evidence::text FROM pending_review_cases"))
          .isEqualTo(firstReview);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM pending_review_cases", Integer.class))
          .isEqualTo(1);
    }

    @Test
    @DisplayName(
        "El conflicto protege solo a su propietario y permite confirmar otro Player válido")
    void processOtherPlayers() {
      // Arrange
      Map<String, Object> before = persistedPlayer();
      PlayerCandidate first = candidate("7001", "Changed A", "101");
      PlayerCandidate second = candidate("7002", "Changed B", "102");
      PlayerCandidate other = candidate("7003", "Other player", "102");
      PlayerSnapshot conflict = snapshot(first, second, other);

      // Act
      PlayerSynchronizationResult result = catalog.applySynchronization(conflict);

      // Assert
      assertThat(result.created()).isEqualTo(1);
      assertThat(result.updated()).isZero();
      assertThat(result.markedInactive()).isZero();
      assertThat(result.discardedInvalid()).isEqualTo(2);

      // Verify
      assertThat(persistedPlayer()).isEqualTo(before);
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .extracting(player -> player.getName())
          .containsExactly("Original", "Other player");
      assertThat(jdbc.queryForObject("SELECT count(*) FROM players", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName(
        "Un Player legacy sin Team conserva texto y actividad sin recibir backfill por el conflicto")
    void preserveUnassociatedLegacyPlayer() {
      // Arrange
      jdbc.update("UPDATE players SET team_id = NULL WHERE id = ?", playerId);
      jdbc.update("UPDATE catalog_transition SET completed_at = NULL WHERE id = 1");
      Map<String, Object> before = persistedPlayer();
      List<Map<String, Object>> referencesBefore = persistedReferences();
      PlayerCandidate first = candidate("7001", "Changed A", "101");
      PlayerCandidate second = candidate("7002", "Changed B", "102");
      PlayerSnapshot conflict = snapshot(first, second);

      // Act
      PlayerSynchronizationResult result = catalog.applySynchronization(conflict);

      // Assert
      assertThat(result.updated()).isZero();
      assertThat(result.markedInactive()).isZero();
      assertThat(result.discardedInvalid()).isEqualTo(2);

      // Verify
      assertThat(persistedPlayer()).isEqualTo(before);
      assertThat(persistedReferences()).isEqualTo(referencesBefore);
      assertThat(catalog.getActivePlayers(0, 20)).isEmpty();
      assertThat(
              jdbc.queryForObject(
                  "SELECT evidence->>'originalTeamName' FROM pending_review_cases", String.class))
          .isEqualTo("Team A");
      assertThat(
              jdbc.queryForObject(
                  "SELECT completed_at IS NOT NULL FROM catalog_transition WHERE id = 1",
                  Boolean.class))
          .isTrue();
    }

    @Test
    @DisplayName(
        "Un fallo técnico posterior revierte también el caso de inconsistencia del snapshot")
    void rollBackConflictCase() {
      // Arrange
      Map<String, Object> before = persistedPlayer();
      List<Map<String, Object>> referencesBefore = persistedReferences();
      PlayerCandidate first = candidate("7001", "Changed A", "101");
      PlayerCandidate second = candidate("7002", "Changed B", "102");
      PlayerCandidate other = candidate("7003", "Collision", "101");
      PlayerCandidate duplicateName = candidate("7004", "Collision", "102");
      PlayerSnapshot conflict = snapshot(first, second, other, duplicateName);
      jdbc.execute("CREATE UNIQUE INDEX test_consistency_player_name ON players(name)");
      try {
        // Act / Assert
        assertThatThrownBy(() -> catalog.applySynchronization(conflict))
            .isInstanceOf(PlayerSynchronizationPersistenceException.class);

        // Verify
        assertThat(persistedPlayer()).isEqualTo(before);
        assertThat(persistedReferences()).isEqualTo(referencesBefore);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM players", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pending_review_cases", Integer.class))
            .isZero();
      } finally {
        jdbc.execute("DROP INDEX test_consistency_player_name");
      }
    }
  }

  @Nested
  @DisplayName("Asociaciones coherentes y protección limitada a la foto")
  class CoherentAssociations {
    @Test
    @DisplayName(
        "Formas equivalentes de referencias distintas mantienen nombre y posición ya persistidos")
    void preserveEquivalentPresentation() {
      // Arrange
      jdbc.update(
          "UPDATE players SET name = 'José Pérez', position = 'midfielder' WHERE id = ?", playerId);
      Map<String, Object> before = persistedPlayer();
      List<Map<String, Object>> referencesBefore = persistedReferences();
      PlayerCandidate first =
          new PlayerCandidate(
              "7001", "JOSE PEREZ", "Team A", "League", "MIDFIELDER", null, null, "101");
      PlayerCandidate second =
          new PlayerCandidate(
              "7002", "Jose   Perez", "Team A", "League", "Midfielder", null, null, "101");
      PlayerSnapshot forward = snapshot(first, second);
      PlayerSnapshot reverse = snapshot(second, first);

      // Act
      catalog.applySynchronization(forward);
      catalog.applySynchronization(reverse);

      // Assert / Verify
      assertThat(persistedPlayer()).isEqualTo(before);
      assertThat(persistedReferences()).isEqualTo(referencesBefore);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM pending_review_cases", Integer.class))
          .isZero();
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.getName()).isEqualTo("José Pérez");
                assertThat(player.getPosition()).isEqualTo("midfielder");
              });
    }

    @Test
    @DisplayName(
        "Una foto coherente posterior permite actualizar al Player con caso histórico por nombre o posición")
    void processAfterSubjectStateConflict() {
      // Arrange
      PlayerCandidate first = candidate("7001", "Name A", "101");
      PlayerCandidate second = candidate("7002", "Name B", "101");
      catalog.applySynchronization(snapshot(first, second));
      PlayerCandidate coherentFirst = candidate("7001", "José Pérez", "102");
      PlayerCandidate coherentSecond = candidate("7002", "JOSE PEREZ", "102");
      List<Map<String, Object>> referencesBefore = persistedReferences();
      PlayerSnapshot coherent = snapshot(coherentSecond, coherentFirst);

      // Act
      PlayerSynchronizationResult result = catalog.applySynchronization(coherent);

      // Assert
      assertThat(result.created()).isZero();
      assertThat(result.discardedInvalid()).isZero();

      // Verify
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.getId()).isEqualTo(playerId);
                assertThat(player.getName()).isEqualTo("José Pérez");
                assertThat(player.getTeam().getName()).isEqualTo("Team B");
              });
      assertThat(persistedReferences()).isEqualTo(referencesBefore);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM pending_review_cases", Integer.class))
          .isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName(
        "Valores opcionales incompatibles no invalidan al sujeto y conservan el valor persistido")
    void preservePersistedOptionalsOnConflict(boolean reverse) {
      // Arrange
      jdbc.update(
          "UPDATE players SET date_of_birth='1990-01-01', nationality='Spain' WHERE id = ?",
          playerId);
      PlayerCandidate first =
          optionalCandidate("7001", java.time.LocalDate.of(1998, 4, 10), "Argentina");
      PlayerCandidate second =
          optionalCandidate("7002", java.time.LocalDate.of(1999, 4, 10), "Uruguay");
      PlayerSnapshot photo = reverse ? snapshot(second, first) : snapshot(first, second);
      Map<String, Object> referencesBefore = (Map<String, Object>) persistedReferences();

      // Act
      PlayerSynchronizationResult result = catalog.applySynchronization(photo);

      // Assert
      assertThat(result.discardedInvalid()).isZero();
      assertThat(result.updated()).isEqualTo(2);

      // Verify
      assertThat(persistedReferences()).isEqualTo(referencesBefore);
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.getDateOfBirth()).isEqualTo(java.time.LocalDate.of(1990, 1, 1));
                assertThat(player.getNationality()).isEqualTo("Spain");
                assertThat(player.getTeam().getName()).isEqualTo("Team A");
                assertThat(player.getName()).isEqualTo("Original");
                assertThat(player.getPosition()).isEqualTo("Forward");
              });
      assertThat(
              jdbc.queryForList(
                  "SELECT cause_code,evidence->>'attribute' FROM pending_review_cases ORDER BY cause_code"))
          .containsExactlyInAnyOrder(
              Map.of("cause_code", "CONFLICTING_DATE_OF_BIRTH", "evidence", "dateOfBirth"),
              Map.of("cause_code", "CONFLICTING_NATIONALITY", "evidence", "nationality"));
    }

    @Test
    @DisplayName(
        "Un conflicto de un atributo no impide consolidar el otro ni una foto posterior completarlo")
    void consolidateCoherentOptionalAndRecoverLater() {
      // Arrange
      PlayerCandidate conflictingDate =
          optionalCandidate("7001", java.time.LocalDate.of(1998, 4, 10), "Argentina");
      PlayerCandidate conflictingNationality =
          optionalCandidate("7002", java.time.LocalDate.of(1998, 4, 10), "Uruguay");
      catalog.applySynchronization(snapshot(conflictingDate, conflictingNationality));

      // Assert / Verify
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.getDateOfBirth()).isEqualTo(java.time.LocalDate.of(1998, 4, 10));
                assertThat(player.getNationality()).isNull();
              });

      // Act
      catalog.applySynchronization(
          snapshot(
              optionalCandidate("7001", java.time.LocalDate.of(1998, 4, 10), "Argentina"),
              optionalCandidate("7002", java.time.LocalDate.of(1998, 4, 10), "Argentina")));

      // Verify
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.getDateOfBirth()).isEqualTo(java.time.LocalDate.of(1998, 4, 10));
                assertThat(player.getNationality()).isEqualTo("Argentina");
              });
    }

    @Test
    @DisplayName("El caso histórico no protege de una ausencia confirmada en una foto posterior")
    void retireAfterConflictingSnapshot() {
      // Arrange
      List<Map<String, Object>> referencesBefore = persistedReferences();
      PlayerCandidate first = candidate("7001", "Wrong A", "101");
      PlayerCandidate second = candidate("7002", "Wrong B", "102");
      catalog.applySynchronization(snapshot(first, second));
      Map<String, Object> historicalCase =
          jdbc.queryForMap("SELECT id,last_detected_at,evidence::text FROM pending_review_cases");
      PlayerSnapshot absent = snapshot();

      // Act
      PlayerSynchronizationResult result = catalog.applySynchronization(absent);

      // Assert
      assertThat(result.markedInactive()).isEqualTo(1);
      assertThat(result.updated()).isZero();
      assertThat(result.discardedInvalid()).isZero();

      // Verify
      assertThat(persistedPlayer()).containsEntry("active", false);
      assertThat(persistedReferences()).isEqualTo(referencesBefore);
      assertThat(catalog.getActivePlayers(0, 20)).isEmpty();
      assertThat(
              jdbc.queryForMap(
                  "SELECT id,last_detected_at,evidence::text FROM pending_review_cases"))
          .isEqualTo(historicalCase);
    }

    @Test
    @DisplayName(
        "Referencias distintas al mismo Team permiten procesamiento sin caso ni pérdida de identidad")
    void processCoherentReferences() {
      // Arrange
      List<Map<String, Object>> referencesBefore = persistedReferences();
      PlayerCandidate first = candidate("7001", "Current name", "102");
      PlayerCandidate second = candidate("7002", "Current name", "102");
      PlayerSnapshot coherent = snapshot(first, second);

      // Act
      PlayerSynchronizationResult result = catalog.applySynchronization(coherent);

      // Assert
      assertThat(result.created()).isZero();
      assertThat(result.discardedInvalid()).isZero();
      assertThat(result.markedInactive()).isZero();

      // Verify
      assertThat(persistedReferences()).isEqualTo(referencesBefore);
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.getId()).isEqualTo(playerId);
                assertThat(player.getName()).isEqualTo("Current name");
                assertThat(player.getTeam().getName()).isEqualTo("Team B");
              });
      assertThat(jdbc.queryForObject("SELECT count(*) FROM pending_review_cases", Integer.class))
          .isZero();
    }

    @Test
    @DisplayName(
        "Una foto posterior coherente permite transferencia y regreso sin cerrar ni usar el caso como bloqueo")
    void processCoherentSnapshotAfterConflict() {
      // Arrange
      jdbc.update("UPDATE players SET active = false WHERE id = ?", playerId);
      List<Map<String, Object>> referencesBefore = persistedReferences();
      PlayerCandidate conflictingFirst = candidate("7001", "Wrong A", "101");
      PlayerCandidate conflictingSecond = candidate("7002", "Wrong B", "102");
      catalog.applySynchronization(snapshot(conflictingFirst, conflictingSecond));
      Map<String, Object> historicalCase =
          jdbc.queryForMap(
              "SELECT id,first_detected_at,last_detected_at,evidence::text FROM pending_review_cases");
      PlayerCandidate coherentFirst = candidate("7001", "Returned", "102");
      PlayerCandidate coherentSecond = candidate("7002", "Returned", "102");
      PlayerSnapshot coherent = snapshot(coherentFirst, coherentSecond);

      // Act
      PlayerSynchronizationResult result = catalog.applySynchronization(coherent);

      // Assert
      assertThat(result.created()).isZero();
      assertThat(result.discardedInvalid()).isZero();
      assertThat(result.markedInactive()).isZero();

      // Verify
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.getId()).isEqualTo(playerId);
                assertThat(player.getName()).isEqualTo("Returned");
                assertThat(player.getTeam().getName()).isEqualTo("Team B");
                assertThat(player.isActive()).isTrue();
              });
      assertThat(persistedPlayer()).containsEntry("team", "Team B").containsEntry("active", true);
      assertThat(persistedReferences()).isEqualTo(referencesBefore);
      assertThat(
              jdbc.queryForMap(
                  "SELECT id,first_detected_at,last_detected_at,evidence::text FROM pending_review_cases"))
          .isEqualTo(historicalCase);
    }
  }
}
