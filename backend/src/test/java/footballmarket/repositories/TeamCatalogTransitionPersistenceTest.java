package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.Player;
import footballmarket.models.records.InvalidPlayerObservation;
import footballmarket.models.records.LeagueCandidate;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.models.records.TeamCandidate;
import footballmarket.services.PlayerCatalogService;
import footballmarket.services.exceptions.PlayerSynchronizationPersistenceException;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Invariantes técnicas de la transición V5: marcador, espejo legacy, backfill único y conservación
 * de identidad. La preparación usa SQL técnico del contenedor; el comportamiento se observa por la
 * API pública del Service y por las tablas que esa API no expone.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class TeamCatalogTransitionPersistenceTest {
  @Autowired private PlayerCatalogService catalog;
  @Autowired private JdbcTemplate jdbc;

  private void legacyPlayer(String name, String team, String league, boolean active) {
    jdbc.update(
        "INSERT INTO players(name,team,league,position,active,date_of_birth,nationality) VALUES (?,?,?,?,?,?,?)",
        name,
        team,
        league,
        "Forward",
        active,
        LocalDate.of(1990, 1, 1),
        "Spain");
    jdbc.update(
        "INSERT INTO player_external_references(player_id,provider,external_id) "
            + "SELECT id,'FOOTBALL_DATA',? FROM players WHERE name=?",
        "legacy-" + name,
        name);
  }

  private PlayerSnapshot photo(PlayerCandidate... candidates) {
    return photo(List.of(), candidates);
  }

  private PlayerSnapshot photo(
      List<InvalidPlayerObservation> invalid, PlayerCandidate... candidates) {
    LeagueCandidate league = new LeagueCandidate("10", "League");
    TeamCandidate team = new TeamCandidate("101", "Team A", "10", true);
    return new PlayerSnapshot(
        List.of(candidates),
        candidates.length,
        invalid.size(),
        List.of(league),
        List.of(team),
        invalid);
  }

  private PlayerCandidate candidate(String externalId, String name) {
    return new PlayerCandidate(externalId, name, "Team A", "League", "Forward", null, null, "101");
  }

  @Nested
  @DisplayName("Backfill de ausentes y marcador de transición")
  class BackfillAndMarker {
    @Test
    @DisplayName(
        "Un ausente sin equipo con una única coincidencia estricta queda asociado sin cambiar su actividad")
    void backfillSingleMatchKeepsActivity() {
      // Arrange
      legacyPlayer("Absent", "Team A", "League", false);

      // Act
      PlayerSynchronizationResult result =
          catalog.applySynchronization(photo(candidate("1", "Present")));

      // Assert
      assertThat(result.markedInactive()).isZero();

      // Verify
      Map<String, Object> row =
          jdbc.queryForMap("SELECT team_id,active,team,league FROM players WHERE name='Absent'");
      assertThat(row.get("team_id")).isNotNull();
      assertThat(row.get("active")).isEqualTo(false);
      assertThat(row.get("team")).isEqualTo("Team A");
      assertThat(row.get("league")).isEqualTo("League");
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .extracting(Player::getName)
          .containsExactly("Present");
    }

    @Test
    @DisplayName("Sin coincidencias el ausente conserva su texto original y registra evidencia")
    void backfillWithoutMatchKeepsOriginalText() {
      // Arrange
      legacyPlayer("Absent", "Arsenal FC", "League", true);

      // Act
      catalog.applySynchronization(photo(candidate("1", "Present")));

      // Verify
      assertThat(jdbc.queryForObject("SELECT team FROM players WHERE name='Absent'", String.class))
          .isEqualTo("Arsenal FC");
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM pending_review_cases WHERE category='LEGACY_TEAM_ASSOCIATION'",
                  Integer.class))
          .isEqualTo(1);
      assertThat(
              jdbc.queryForObject(
                  "SELECT evidence->>'originalTeamName' FROM pending_review_cases WHERE category='LEGACY_TEAM_ASSOCIATION'",
                  String.class))
          .isEqualTo("Arsenal FC");
    }

    @Test
    @DisplayName("El marcador se completa una vez y evita repetir el backfill")
    void completeMarkerOnce() {
      // Arrange
      legacyPlayer("Absent", "Team A", "League", false);
      catalog.applySynchronization(photo(candidate("1", "Present")));
      assertThat(
              jdbc.queryForObject(
                  "SELECT completed_at FROM catalog_transition WHERE id=1", Object.class))
          .isNotNull();

      // Act
      jdbc.update("UPDATE players SET team_id=NULL, team='Nombre actual' WHERE name='Absent'");
      catalog.applySynchronization(photo(candidate("1", "Present")));

      // Verify
      assertThat(
              jdbc.queryForObject("SELECT team_id FROM players WHERE name='Absent'", Object.class))
          .isNull();
    }

    @Test
    @DisplayName("Un presente sin equipo resoluble no recibe backfill textual")
    void noBackfillForInvalidPresence() {
      // Arrange
      legacyPlayer("Present", "Arsenal FC", "League", true);

      // Act
      PlayerSynchronizationResult result =
          catalog.applySynchronization(
              photo(
                  List.of(
                      new InvalidPlayerObservation(
                          "legacy-Present", "999", "Team X", "position"))));

      // Assert
      assertThat(result.discardedInvalid()).isEqualTo(1);

      // Verify
      assertThat(
              jdbc.queryForObject("SELECT team_id FROM players WHERE name='Present'", Object.class))
          .isNull();
      assertThat(
              jdbc.queryForObject("SELECT active FROM players WHERE name='Present'", Boolean.class))
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Espejo legacy y conservación de identidad")
  class LegacyMirror {
    @Test
    @DisplayName("El alta y la asociación escriben nombres derivados para las columnas legacy")
    void mirrorDerivedNames() {
      // Act
      catalog.applySynchronization(photo(candidate("1", "Player")));

      // Verify
      Map<String, Object> row =
          jdbc.queryForMap("SELECT team,league,team_id FROM players WHERE name='Player'");
      assertThat(row.get("team")).isEqualTo("Team A");
      assertThat(row.get("league")).isEqualTo("League");
      assertThat(row.get("team_id")).isNotNull();
    }

    @Test
    @DisplayName("La foto prevalece sobre el texto legacy y conserva el identificador interno")
    void photoOverridesLegacyText() {
      // Arrange
      legacyPlayer("Present", "Equipo desactualizado", "Liga desactualizada", true);
      Long legacyId =
          jdbc.queryForObject("SELECT id FROM players WHERE name='Present'", Long.class);

      // Act
      catalog.applySynchronization(
          photo(
              new PlayerCandidate(
                  "legacy-Present", "Present", "Team A", "League", "Forward", null, null, "101")));

      // Verify
      Map<String, Object> row =
          jdbc.queryForMap("SELECT id,team,league,team_id FROM players WHERE name='Present'");
      assertThat(row.get("id")).isEqualTo(legacyId);
      assertThat(row.get("team")).isEqualTo("Team A");
      assertThat(row.get("league")).isEqualTo("League");
      assertThat(row.get("team_id")).isNotNull();
    }

    @Test
    @DisplayName("Un fallo técnico revierte asociaciones, casos y marcador del mismo intento")
    void rollbackEverythingTogether() {
      // Arrange
      legacyPlayer("Absent", "Team A", "League", false);
      jdbc.execute("CREATE UNIQUE INDEX test_transition_name ON players(name)");

      // Act / Assert
      try {
        assertThatThrownBy(
                () ->
                    catalog.applySynchronization(
                        photo(candidate("1", "Present"), candidate("2", "Present"))))
            .isInstanceOf(PlayerSynchronizationPersistenceException.class);

        // Verify
        assertThat(jdbc.queryForObject("SELECT count(*) FROM teams", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pending_review_cases", Integer.class))
            .isZero();
        assertThat(
                jdbc.queryForObject(
                    "SELECT completed_at FROM catalog_transition WHERE id=1", Object.class))
            .isNull();
        // La referencia única del intento deja el jugador preexistente sin asociación y su
        // evidencia,
        // pero ningún otro cambio de esa foto queda confirmado.
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM pending_review_cases WHERE category <> 'PLAYER_TEAM_UNRESOLVED'",
                    Integer.class))
            .isZero();
      } finally {
        jdbc.execute("DROP INDEX test_transition_name");
      }
    }
  }
}
