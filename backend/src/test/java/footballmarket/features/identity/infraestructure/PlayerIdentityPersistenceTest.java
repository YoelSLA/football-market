package footballmarket.features.identity.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.catalog.infraestructure.LeagueRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.catalog.infraestructure.TeamRepository;
import footballmarket.features.identity.application.internal.PlayerIdentityReferenceConfirmation;
import footballmarket.features.identity.domain.enums.PlayerIdentityItemResult;
import footballmarket.features.identity.domain.models.WhoScoredIdentityMatcher;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class PlayerIdentityPersistenceTest {
  @Autowired private LeagueRepository leagues;
  @Autowired private TeamRepository teams;
  @Autowired private PlayerRepository players;
  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager manager;
  @Autowired private PlayerIdentityReferenceConfirmation confirmation;

  private record ConfirmationCase(
      Long playerId, Long leagueId, WhoScoredIdentityMatcher.Decision decision) {}

  private TransactionTemplate transaction() {
    TransactionTemplate transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return transaction;
  }

  private ConfirmationCase prepareConfirmation(String externalId) {
    return transaction()
        .execute(
            status -> {
              League league = leagues.save(new League("Concurrent League"));
              Team team = teams.save(new Team("Concurrent Team", league, true));
              Player player =
                  players.saveAndFlush(new Player("Concurrent Player", team, "Forward"));
              WhoScoredIdentityMatcher.Decision decision =
                  new WhoScoredIdentityMatcher.Decision(
                      PlayerIdentityItemResult.RESOLVED,
                      "UNIQUE_NAME_AND_TEAM",
                      List.of(),
                      externalId);
              return new ConfirmationCase(player.getId(), league.getId(), decision);
            });
  }

  @Nested
  @DisplayName("Esquema y selección sin auditoría")
  class SchemaAndSelection {
    @Test
    @DisplayName(
        "Flyway retira las cinco tablas de auditoría y conserva índice y revisión de catálogo")
    void removesOnlyIdentityAuditSchema() {
      // Act
      Number auditTables =
          (Number)
              entityManager
                  .createNativeQuery(
                      "select count(*) from information_schema.tables where table_schema = 'public' and table_name in "
                          + "('player_identity_sync_runs','player_identity_sync_items','player_identity_league_discoveries',"
                          + "'whoscored_identity_candidates','player_identity_discarded_rows')")
                  .getSingleResult();
      Number uniqueIndex =
          (Number)
              entityManager
                  .createNativeQuery(
                      "select count(*) from pg_indexes where schemaname = 'public' and indexname = 'uk_player_external_references_whoscored_player'")
                  .getSingleResult();
      Number reviews =
          (Number)
              entityManager
                  .createNativeQuery(
                      "select count(*) from information_schema.tables where table_schema = 'public' and table_name = 'pending_review_cases'")
                  .getSingleResult();
      // Assert
      assertThat(auditTables.longValue()).isZero();
      assertThat(uniqueIndex.longValue()).isEqualTo(1);
      assertThat(reviews.longValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("El snapshot incluye inactivos de equipos no actuales y carga sus referencias")
    void snapshotsWithoutAdditionalFilters() {
      // Arrange
      League league = leagues.save(new League("Identity League"));
      Team team = teams.save(new Team("Identity Team", league, false));
      Player player = new Player("Identity Player", team, "Forward");
      player.deactivate();
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "snapshot-player");
      players.saveAndFlush(player);
      entityManager.clear();
      // Act
      List<Player> result = players.findIdentitySnapshot(List.of(league.getId()));
      entityManager.clear();
      // Assert
      assertThat(result)
          .singleElement()
          .satisfies(
              snapshot -> {
                assertThat(snapshot.getTeam().getName()).isEqualTo("Identity Team");
                assertThat(snapshot.getLeague().getId()).isEqualTo(league.getId());
                assertThat(snapshot.getExternalReferences()).hasSize(1);
                assertThat(snapshot.isActive()).isFalse();
              });
    }

    @Test
    @DisplayName("Recaptura solo IDs elegibles existentes y conserva referencias concurrentes")
    void recapturesEligibleIdsWithConcurrentReferences() {
      // Arrange
      League league = leagues.save(new League("Historical League"));
      Team team = teams.save(new Team("Historical Team", league, false));
      Player referenced = new Player("Renamed Player", team, "Forward");
      referenced.deactivate();
      referenced.addExternalReference(ExternalProvider.WHOSCORED, "99001");
      players.saveAndFlush(referenced);
      players.saveAndFlush(new Player("Not Selected", team, "Forward"));
      entityManager.clear();
      // Act
      List<Player> selected =
          players.findHistoricalIdentitySnapshot(List.of(referenced.getId(), Long.MAX_VALUE));
      entityManager.clear();
      // Assert
      assertThat(selected)
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.getId()).isEqualTo(referenced.getId());
                assertThat(player.getName()).isEqualTo("Renamed Player");
                assertThat(player.getExternalReferences())
                    .extracting(reference -> reference.getExternalId())
                    .containsExactly("99001");
                assertThat(player.isActive()).isFalse();
              });
    }

    @Test
    @DisplayName("El índice parcial impide una segunda identidad WhoScored al eludir el Model")
    void enforcesInverseWhoScoredCardinality() {
      // Arrange
      League league = leagues.save(new League("Inverse League"));
      Team team = teams.save(new Team("Inverse Team", league, true));
      Player player = new Player("Inverse Player", team, "Forward");
      player.addExternalReference(ExternalProvider.WHOSCORED, "770001");
      players.saveAndFlush(player);
      // Act / Assert
      assertThatThrownBy(
              () ->
                  entityManager
                      .createNativeQuery(
                          "INSERT INTO player_external_references(id, player_id, provider, external_id) VALUES(nextval('player_external_references_id_seq'), :player, 'WHOSCORED', '770002')")
                      .setParameter("player", player.getId())
                      .executeUpdate())
          .isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    @DisplayName("La DB impide asignar a otro jugador un ID externo ya utilizado")
    void enforcesExternalOwnership() {
      // Arrange
      League league = leagues.save(new League("Ownership League"));
      Team team = teams.save(new Team("Ownership Team", league, true));
      Player first = new Player("First", team, "Forward");
      first.addExternalReference(ExternalProvider.WHOSCORED, "987654");
      players.saveAndFlush(first);
      Player second = new Player("Second", team, "Forward");
      second.addExternalReference(ExternalProvider.WHOSCORED, "987654");
      // Act / Assert
      assertThatThrownBy(() -> players.saveAndFlush(second))
          .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName(
        "El snapshot conserva referencias heredadas contradictorias sin escoger ni borrarlas")
    void snapshotsLegacyMultipleReferences() {
      // Arrange
      League league = leagues.save(new League("Legacy League"));
      Team team = teams.save(new Team("Legacy Team", league, true));
      Player player = new Player("Legacy Player", team, "Forward");
      player.addExternalReference(ExternalProvider.WHOSCORED, "780001");
      players.saveAndFlush(player);
      entityManager
          .createNativeQuery("DROP INDEX uk_player_external_references_whoscored_player")
          .executeUpdate();
      entityManager
          .createNativeQuery(
              "INSERT INTO player_external_references(id, player_id, provider, external_id) VALUES(nextval('player_external_references_id_seq'), :player, 'WHOSCORED', '780002')")
          .setParameter("player", player.getId())
          .executeUpdate();
      entityManager.clear();
      // Act
      List<Player> selected = players.findIdentitySnapshot(List.of(league.getId()));
      // Assert
      assertThat(selected)
          .singleElement()
          .satisfies(
              local ->
                  assertThat(local.getExternalReferences())
                      .extracting(reference -> reference.getExternalId())
                      .containsExactlyInAnyOrder("780001", "780002"));
    }
  }

  @Nested
  @DisplayName("Confirmación aislada de referencias")
  class Confirmation {
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Confirmar conserva los atributos del catálogo modificados después del snapshot")
    void preservesConcurrentCatalogueChanges() {
      // Arrange
      ConfirmationCase captured = prepareConfirmation("880001");
      transaction()
          .executeWithoutResult(
              status -> {
                Player player = players.findById(captured.playerId()).orElseThrow();
                player.update("Renamed Player", player.getTeam(), "Midfielder");
              });
      // Act
      PlayerIdentityItemResult result =
          confirmation.confirm(captured.playerId(), captured.decision());
      // Assert
      assertThat(result).isEqualTo(PlayerIdentityItemResult.RESOLVED);
      // Verify
      List<Player> saved = players.findIdentitySnapshot(List.of(captured.leagueId()));
      assertThat(saved)
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.getName()).isEqualTo("Renamed Player");
                assertThat(player.getPosition()).isEqualTo("Midfielder");
                assertThat(player.getExternalReferences())
                    .extracting(reference -> reference.getExternalId())
                    .containsExactly("880001");
              });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Dos confirmaciones concurrentes del mismo jugador son idempotentes")
    void serializesConcurrentSameReference() throws Exception {
      // Arrange
      ConfirmationCase captured = prepareConfirmation("880002");
      CountDownLatch start = new CountDownLatch(1);
      try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
        Future<PlayerIdentityItemResult> one =
            executor.submit(
                () -> {
                  start.await();
                  return confirmation.confirm(captured.playerId(), captured.decision());
                });
        Future<PlayerIdentityItemResult> two =
            executor.submit(
                () -> {
                  start.await();
                  return confirmation.confirm(captured.playerId(), captured.decision());
                });
        // Act
        start.countDown();
        List<PlayerIdentityItemResult> results =
            List.of(one.get(10, TimeUnit.SECONDS), two.get(10, TimeUnit.SECONDS));
        // Assert
        assertThat(results)
            .containsExactlyInAnyOrder(
                PlayerIdentityItemResult.RESOLVED, PlayerIdentityItemResult.ALREADY_REFERENCED);
      }
      // Verify
      List<Player> saved = players.findIdentitySnapshot(List.of(captured.leagueId()));
      assertThat(saved)
          .singleElement()
          .satisfies(player -> assertThat(player.getExternalReferences()).hasSize(1));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Un ID pretendido por dos jugadores concurrentes tiene un único propietario")
    void protectsConcurrentOwnership() throws Exception {
      // Arrange
      ConfirmationCase first = prepareConfirmation("880003");
      ConfirmationCase second = prepareConfirmation("880003");
      CountDownLatch start = new CountDownLatch(1);
      try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
        Future<PlayerIdentityItemResult> one =
            executor.submit(
                () -> {
                  start.await();
                  return confirmation.confirm(first.playerId(), first.decision());
                });
        Future<PlayerIdentityItemResult> two =
            executor.submit(
                () -> {
                  start.await();
                  return confirmation.confirm(second.playerId(), second.decision());
                });
        // Act
        start.countDown();
        List<PlayerIdentityItemResult> results =
            List.of(one.get(10, TimeUnit.SECONDS), two.get(10, TimeUnit.SECONDS));
        // Assert
        assertThat(results)
            .containsExactlyInAnyOrder(
                PlayerIdentityItemResult.RESOLVED, PlayerIdentityItemResult.CONFLICT);
      }
      // Verify
      List<Player> saved =
          players.findIdentitySnapshot(List.of(first.leagueId(), second.leagueId()));
      assertThat(saved.stream().flatMap(player -> player.getExternalReferences().stream()))
          .hasSize(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Una referencia distinta confirmada antes de guardar no se sobrescribe")
    void preservesDifferentConcurrentReference() {
      // Arrange
      ConfirmationCase captured = prepareConfirmation("880004");
      transaction()
          .executeWithoutResult(
              status ->
                  players
                      .findById(captured.playerId())
                      .orElseThrow()
                      .addExternalReference(ExternalProvider.WHOSCORED, "880005"));
      // Act
      PlayerIdentityItemResult result =
          confirmation.confirm(captured.playerId(), captured.decision());
      // Assert
      assertThat(result).isEqualTo(PlayerIdentityItemResult.CONFLICT);
      // Verify
      assertThat(players.findIdentitySnapshot(List.of(captured.leagueId())))
          .singleElement()
          .satisfies(
              player ->
                  assertThat(player.getExternalReferences())
                      .extracting(reference -> reference.getExternalId())
                      .containsExactly("880005"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Un jugador borrado no recibe referencia y produce error de confirmación")
    void handlesDeletedPlayer() {
      // Arrange
      ConfirmationCase captured = prepareConfirmation("880006");
      transaction().executeWithoutResult(status -> players.deleteById(captured.playerId()));
      // Act
      PlayerIdentityItemResult result =
          confirmation.confirm(captured.playerId(), captured.decision());
      // Assert
      assertThat(result).isEqualTo(PlayerIdentityItemResult.PERSISTENCE_ERROR);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Un fallo técnico revierte la referencia y permite confirmar otro jugador")
    void rollsBackFailedConfirmationAndContinues() {
      // Arrange
      ConfirmationCase failed = prepareConfirmation("999001");
      ConfirmationCase successful = prepareConfirmation("999002");
      transaction()
          .executeWithoutResult(
              status -> {
                entityManager
                    .createNativeQuery(
                        "CREATE FUNCTION identity_test_reject_reference() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.external_id = '999001' THEN RAISE EXCEPTION 'controlled test failure' USING ERRCODE = 'XX000'; END IF; RETURN NEW; END $$")
                    .executeUpdate();
                entityManager
                    .createNativeQuery(
                        "CREATE TRIGGER identity_test_reject_reference BEFORE INSERT ON player_external_references FOR EACH ROW EXECUTE FUNCTION identity_test_reject_reference()")
                    .executeUpdate();
              });
      // Act
      PlayerIdentityItemResult failedResult =
          confirmation.confirm(failed.playerId(), failed.decision());
      PlayerIdentityItemResult successResult =
          confirmation.confirm(successful.playerId(), successful.decision());
      // Assert
      assertThat(failedResult).isEqualTo(PlayerIdentityItemResult.PERSISTENCE_ERROR);
      assertThat(successResult).isEqualTo(PlayerIdentityItemResult.RESOLVED);
      // Verify
      assertThat(players.findIdentitySnapshot(List.of(failed.leagueId())))
          .singleElement()
          .satisfies(player -> assertThat(player.getExternalReferences()).isEmpty());
    }
  }
}
