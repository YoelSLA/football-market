package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.League;
import footballmarket.models.Player;
import footballmarket.models.PlayerIdentitySyncItem;
import footballmarket.models.PlayerIdentitySyncRun;
import footballmarket.models.Team;
import footballmarket.models.WhoScoredIdentityCandidate;
import footballmarket.models.WhoScoredIdentityMatcher;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.enums.PlayerIdentityItemResult;
import footballmarket.models.enums.PlayerIdentityPassType;
import footballmarket.models.records.WhoScoredIdentityContext;
import footballmarket.services.internal.PlayerIdentityReferenceConfirmation;
import footballmarket.support.TestcontainersConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
@org.junit.jupiter.api.Tag("integration")
class PlayerIdentityPersistenceTest {
  @Autowired private LeagueRepository leagues;
  @Autowired private TeamRepository teams;
  @Autowired private PlayerRepository players;
  @Autowired private PlayerIdentitySyncRunRepository runs;
  @Autowired private PlayerIdentitySyncItemRepository items;
  @Autowired private WhoScoredIdentityCandidateRepository candidates;
  @Autowired private jakarta.persistence.EntityManager entityManager;
  @Autowired private PlatformTransactionManager manager;
  @Autowired private PlayerIdentityReferenceConfirmation confirmation;

  private record ConfirmationCase(
      Long playerId, Long itemId, WhoScoredIdentityMatcher.Decision decision) {}

  private TransactionTemplate transaction() {
    TransactionTemplate transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return transaction;
  }

  private ConfirmationCase prepareConfirmation(Player existing, String externalId) {
    return transaction()
        .execute(
            status -> {
              Player player;
              if (existing == null) {
                League league = leagues.save(new League("Concurrent League"));
                Team team = teams.save(new Team("Concurrent Team", league, true));
                player = players.saveAndFlush(new Player("Concurrent Player", team, "Forward"));
              } else {
                player = players.findById(existing.getId()).orElseThrow();
              }
              PlayerIdentitySyncRun run =
                  runs.saveAndFlush(
                      new PlayerIdentitySyncRun(
                          PlayerIdentityPassType.CURRENT, null, Instant.now()));
              String identity =
                  externalId == null ? Long.toString(80000000L + run.getId()) : externalId;
              WhoScoredIdentityCandidate candidate =
                  new WhoScoredIdentityCandidate(run.getId(), identity);
              candidate.observe(
                  new WhoScoredIdentityContext(
                      "Concurrent Player",
                      null,
                      "Concurrent Team",
                      4,
                      null,
                      player.getLeague().getId(),
                      "Concurrent League",
                      null,
                      null));
              candidates.saveAndFlush(candidate);
              PlayerIdentitySyncItem item =
                  items.saveAndFlush(
                      new PlayerIdentitySyncItem(
                          run.getId(),
                          player.getId(),
                          player.getName(),
                          player.getTeam().getId(),
                          player.getTeam().getName(),
                          player.getLeague().getId(),
                          player.getLeague().getName(),
                          List.of()));
              WhoScoredIdentityMatcher.Decision decision =
                  new WhoScoredIdentityMatcher.Decision(
                      PlayerIdentityItemResult.RESOLVED,
                      "UNIQUE_NAME_AND_TEAM",
                      List.of(candidate),
                      identity);
              return new ConfirmationCase(player.getId(), item.getId(), decision);
            });
  }

  @Nested
  @DisplayName("Selección y auditoría del pase histórico")
  class Historical {
    @Test
    @DisplayName(
        "Selecciona solo pendientes existentes sin referencias y materializa atributos nuevos")
    void selectsEligibleSnapshotsOnly() {
      // Arrange
      League league = leagues.save(new League("Historical League"));
      Team team = teams.save(new Team("Historical Team", league, false));
      PlayerIdentitySyncRun source =
          runs.saveAndFlush(
              new PlayerIdentitySyncRun(PlayerIdentityPassType.CURRENT, null, Instant.now()));
      java.util.ArrayList<Long> expected = new java.util.ArrayList<>();
      for (PlayerIdentityItemResult result : PlayerIdentityItemResult.values()) {
        Player player = players.saveAndFlush(new Player("Original " + result, team, "Forward"));
        PlayerIdentitySyncItem item =
            new PlayerIdentitySyncItem(
                source.getId(),
                player.getId(),
                player.getName(),
                team.getId(),
                team.getName(),
                league.getId(),
                league.getName(),
                List.of());
        if (result != PlayerIdentityItemResult.PENDING) {
          item.decide(
              result,
              "TEST_RESULT",
              "Resultado controlado",
              List.of(),
              result == PlayerIdentityItemResult.RESOLVED
                      || result == PlayerIdentityItemResult.ALREADY_REFERENCED
                  ? "101"
                  : null,
              Instant.now());
        }
        items.saveAndFlush(item);
        if (result == PlayerIdentityItemResult.NO_MATCH
            || result == PlayerIdentityItemResult.AMBIGUOUS) {
          expected.add(player.getId());
          player.update("New " + result, team, "Midfielder");
          player.deactivate();
        }
      }
      Player referenced = new Player("Referenced", team, "Forward");
      referenced.addExternalReference(ExternalProvider.WHOSCORED, "99001");
      players.saveAndFlush(referenced);
      PlayerIdentitySyncItem excluded =
          new PlayerIdentitySyncItem(
              source.getId(),
              referenced.getId(),
              "Referenced",
              team.getId(),
              team.getName(),
              league.getId(),
              league.getName(),
              List.of());
      excluded.decide(
          PlayerIdentityItemResult.NO_MATCH,
          "NO_NAME_MATCH",
          "Sin coincidencia",
          List.of(),
          null,
          Instant.now());
      items.saveAndFlush(excluded);
      PlayerIdentitySyncItem deleted =
          new PlayerIdentitySyncItem(
              source.getId(),
              Long.MAX_VALUE,
              "Deleted",
              team.getId(),
              team.getName(),
              league.getId(),
              league.getName(),
              List.of());
      deleted.decide(
          PlayerIdentityItemResult.NO_MATCH,
          "NO_NAME_MATCH",
          "Sin coincidencia",
          List.of(),
          null,
          Instant.now());
      items.saveAndFlush(deleted);
      entityManager.flush();
      entityManager.clear();
      // Act
      List<Long> pendingIds = items.findHistoricalPendingPlayerIds(source.getId());
      List<Player> selected = players.findHistoricalIdentitySnapshot(pendingIds);
      entityManager.clear();
      // Assert
      assertThat(selected).extracting(Player::getId).containsExactlyInAnyOrderElementsOf(expected);
      assertThat(selected)
          .extracting(Player::getName)
          .containsExactlyInAnyOrder("New NO_MATCH", "New AMBIGUOUS");
      assertThat(selected)
          .allSatisfy(
              player -> {
                assertThat(player.getExternalReferences()).isEmpty();
                assertThat(player.getLeague().getId()).isEqualTo(league.getId());
                assertThat(player.isActive()).isFalse();
              });
      // Verify
      assertThat(items.findByRunIdOrderById(source.getId()))
          .filteredOn(item -> expected.contains(item.getPlayerId()))
          .allSatisfy(item -> assertThat(item.getPlayerName()).startsWith("Original "));
    }

    @Test
    @DisplayName("La selección excluye también varias referencias WhoScored heredadas")
    void excludesMultipleReferences() {
      // Arrange
      League league = leagues.save(new League("Multiple Historical League"));
      Team team = teams.save(new Team("Multiple Team", league, true));
      Player player = new Player("Multiple Player", team, "Forward");
      player.addExternalReference(ExternalProvider.WHOSCORED, "99002");
      players.saveAndFlush(player);
      entityManager
          .createNativeQuery("DROP INDEX uk_player_external_references_whoscored_player")
          .executeUpdate();
      entityManager
          .createNativeQuery(
              "INSERT INTO player_external_references(id, player_id, provider, external_id) VALUES(nextval('player_external_references_id_seq'), :player, 'WHOSCORED', '99003')")
          .setParameter("player", player.getId())
          .executeUpdate();
      entityManager.clear();
      // Act / Assert
      assertThat(players.findHistoricalIdentitySnapshot(List.of(player.getId()))).isEmpty();
    }

    @Test
    @DisplayName(
        "El origen conserva su evidencia cuando el histórico confirma con carrera de referencia")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void preservesSourceAndHistoricalSnapshotDuringRace() {
      // Arrange
      ConfirmationCase original = prepareConfirmation(null, null);
      Long sourceId = items.findById(original.itemId()).orElseThrow().getRunId();
      transaction()
          .executeWithoutResult(
              status -> {
                PlayerIdentitySyncItem item = items.findById(original.itemId()).orElseThrow();
                item.decide(
                    PlayerIdentityItemResult.NO_MATCH,
                    "TEAM_NOT_MATCHED",
                    "Equipo distinto",
                    original.decision().candidates(),
                    null,
                    Instant.now());
                PlayerIdentitySyncRun source = runs.findById(sourceId).orElseThrow();
                source.finish(
                    new footballmarket.models.records.PlayerIdentitySyncCounters(
                        1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0),
                    5,
                    Instant.now(),
                    null);
                Player local = players.findById(original.playerId()).orElseThrow();
                local.update("Historical Name", local.getTeam(), "Forward");
              });
      PlayerIdentitySyncRun before = runs.findById(sourceId).orElseThrow();
      List<WhoScoredIdentityContext> contexts =
          candidates.findByRunIdOrderById(sourceId).getFirst().getContexts();
      // El pase histórico queda persistido como lo deja la captura: run propio ligado al
      // origen y un item por pendiente con los atributos vigentes, sin tocar el origen.
      PlayerIdentitySyncItem historical =
          transaction()
              .execute(
                  status -> {
                    PlayerIdentitySyncRun run =
                        runs.saveAndFlush(
                            new PlayerIdentitySyncRun(
                                PlayerIdentityPassType.HISTORICAL, sourceId, Instant.now()));
                    Player current = players.findById(original.playerId()).orElseThrow();
                    return items.saveAndFlush(
                        new PlayerIdentitySyncItem(
                            run.getId(),
                            current.getId(),
                            current.getName(),
                            current.getTeam().getId(),
                            current.getTeam().getName(),
                            current.getLeague().getId(),
                            current.getLeague().getName(),
                            List.of()));
                  });
      transaction()
          .executeWithoutResult(
              status ->
                  players
                      .findById(original.playerId())
                      .orElseThrow()
                      .addExternalReference(
                          ExternalProvider.WHOSCORED, original.decision().externalId()));
      WhoScoredIdentityMatcher.Decision concurrent =
          new WhoScoredIdentityMatcher.Decision(
              PlayerIdentityItemResult.RESOLVED,
              "UNIQUE_NAME_AND_TEAM",
              List.of(),
              original.decision().externalId());
      // Act
      confirmation.confirm(historical.getId(), concurrent);
      // Assert
      assertThat(items.findById(historical.getId()).orElseThrow().getResult())
          .isEqualTo(PlayerIdentityItemResult.ALREADY_REFERENCED);
      assertThat(items.findById(historical.getId()).orElseThrow().getPlayerName())
          .isEqualTo("Historical Name");
      assertThat(runs.findById(historical.getRunId()).orElseThrow().getSourceRunId())
          .isEqualTo(sourceId);
      PlayerIdentitySyncRun after = runs.findById(sourceId).orElseThrow();
      assertThat(after).usingRecursiveComparison().isEqualTo(before);
      assertThat(items.findById(original.itemId()).orElseThrow().getResult())
          .isEqualTo(PlayerIdentityItemResult.NO_MATCH);
      assertThat(items.findById(original.itemId()).orElseThrow().getPlayerName())
          .isEqualTo("Concurrent Player");
      assertThat(candidates.findByRunIdOrderById(sourceId).getFirst().getContexts())
          .isEqualTo(contexts);
    }
  }

  @Nested
  @DisplayName("Snapshot, JSONB y constraints de identidad")
  class Persistence {
    @Test
    @DisplayName(
        "El índice parcial impide una segunda identidad WhoScored incluso al eludir el Model")
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
    @DisplayName("El snapshot conserva varias referencias heredadas sin elegir ni borrar alguna")
    void snapshotsLegacyMultipleReferences() {
      // Arrange: el índice se retira solo dentro de esta transacción de testing, revertida al
      // finalizar.
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
      List<Player> snapshot = players.findIdentitySnapshot(List.of(league.getId()));
      // Assert
      assertThat(snapshot)
          .singleElement()
          .satisfies(
              local ->
                  assertThat(local.getExternalReferences())
                      .extracting(reference -> reference.getExternalId())
                      .containsExactlyInAnyOrder("780001", "780002"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName(
        "Confirmar usa el snapshot inicial sin sobrescribir atributos del catálogo cambiados después")
    void preservesSnapshotAndConcurrentCatalogueChanges() {
      // Arrange
      ConfirmationCase captured = prepareConfirmation(null, null);
      transaction()
          .executeWithoutResult(
              status -> {
                Player player = players.findById(captured.playerId()).orElseThrow();
                player.update("Renamed Player", player.getTeam(), "Midfielder");
              });
      // Act
      confirmation.confirm(captured.itemId(), captured.decision());
      // Assert
      PlayerIdentitySyncItem item = items.findById(captured.itemId()).orElseThrow();
      assertThat(item.getPlayerName()).isEqualTo("Concurrent Player");
      assertThat(item.getResult()).isEqualTo(PlayerIdentityItemResult.RESOLVED);
      // Verify
      Player player = players.findById(captured.playerId()).orElseThrow();
      assertThat(player.getName()).isEqualTo("Renamed Player");
      assertThat(player.getPosition()).isEqualTo("Midfielder");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Dos ejecuciones con el mismo ID para un jugador confirman una sola referencia")
    void serializesConcurrentSameReference() throws Exception {
      // Arrange
      ConfirmationCase first = prepareConfirmation(null, null);
      Player local = players.findById(first.playerId()).orElseThrow();
      ConfirmationCase second = prepareConfirmation(local, first.decision().externalId());
      CountDownLatch start = new CountDownLatch(1);
      try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
        Future<?> one =
            executor.submit(
                () -> {
                  start.await();
                  confirmation.confirm(first.itemId(), first.decision());
                  return null;
                });
        Future<?> two =
            executor.submit(
                () -> {
                  start.await();
                  confirmation.confirm(second.itemId(), second.decision());
                  return null;
                });
        // Act
        start.countDown();
        one.get(10, TimeUnit.SECONDS);
        two.get(10, TimeUnit.SECONDS);
      }
      // Assert
      List<PlayerIdentityItemResult> results =
          List.of(
              items.findById(first.itemId()).orElseThrow().getResult(),
              items.findById(second.itemId()).orElseThrow().getResult());
      assertThat(results)
          .containsExactlyInAnyOrder(
              PlayerIdentityItemResult.RESOLVED, PlayerIdentityItemResult.ALREADY_REFERENCED);
      // Verify
      Long leagueId =
          transaction()
              .execute(
                  status -> players.findById(first.playerId()).orElseThrow().getLeague().getId());
      List<Player> saved = players.findIdentitySnapshot(List.of(leagueId));
      assertThat(saved)
          .singleElement()
          .satisfies(player -> assertThat(player.getExternalReferences()).hasSize(1));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName(
        "Un ID pretendido por dos jugadores conserva solo un propietario y audita el conflicto")
    void protectsConcurrentOwnership() throws Exception {
      // Arrange
      ConfirmationCase first = prepareConfirmation(null, null);
      ConfirmationCase second = prepareConfirmation(null, first.decision().externalId());
      CountDownLatch start = new CountDownLatch(1);
      try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
        Future<?> one =
            executor.submit(
                () -> {
                  start.await();
                  confirmation.confirm(first.itemId(), first.decision());
                  return null;
                });
        Future<?> two =
            executor.submit(
                () -> {
                  start.await();
                  confirmation.confirm(second.itemId(), second.decision());
                  return null;
                });
        // Act
        start.countDown();
        one.get(10, TimeUnit.SECONDS);
        two.get(10, TimeUnit.SECONDS);
      }
      // Assert
      List<PlayerIdentityItemResult> results =
          List.of(
              items.findById(first.itemId()).orElseThrow().getResult(),
              items.findById(second.itemId()).orElseThrow().getResult());
      assertThat(results)
          .containsExactlyInAnyOrder(
              PlayerIdentityItemResult.RESOLVED, PlayerIdentityItemResult.CONFLICT);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName(
        "Un fallo técnico revierte referencia e item exitoso y permite confirmar otro jugador")
    void rollsBackFailedConfirmationAndContinues() {
      // Arrange
      ConfirmationCase failed = prepareConfirmation(null, null);
      ConfirmationCase successful = prepareConfirmation(null, null);
      transaction()
          .executeWithoutResult(
              status -> {
                entityManager
                    .createNativeQuery(
                        "CREATE FUNCTION identity_test_reject_reference() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.external_id = '"
                            + failed.decision().externalId()
                            + "' THEN RAISE EXCEPTION 'controlled test failure' USING ERRCODE = 'XX000'; END IF; RETURN NEW; END $$")
                    .executeUpdate();
                entityManager
                    .createNativeQuery(
                        "CREATE TRIGGER identity_test_reject_reference BEFORE INSERT ON player_external_references FOR EACH ROW EXECUTE FUNCTION identity_test_reject_reference()")
                    .executeUpdate();
              });
      try {
        // Act
        confirmation.confirm(failed.itemId(), failed.decision());
        confirmation.confirm(successful.itemId(), successful.decision());
        // Assert
        assertThat(items.findById(failed.itemId()).orElseThrow().getResult())
            .isEqualTo(PlayerIdentityItemResult.PERSISTENCE_ERROR);
        assertThat(items.findById(successful.itemId()).orElseThrow().getResult())
            .isEqualTo(PlayerIdentityItemResult.RESOLVED);
        // Verify
        transaction()
            .executeWithoutResult(
                status -> {
                  Player player = players.findById(failed.playerId()).orElseThrow();
                  assertThat(player.getExternalReferences()).isEmpty();
                });
      } finally {
        transaction()
            .executeWithoutResult(
                status -> {
                  entityManager
                      .createNativeQuery(
                          "DROP TRIGGER identity_test_reject_reference ON player_external_references")
                      .executeUpdate();
                  entityManager
                      .createNativeQuery("DROP FUNCTION identity_test_reject_reference()")
                      .executeUpdate();
                });
      }
    }

    @Test
    @DisplayName("El snapshot incluye inactivos de equipos no actuales y materializa referencias")
    void snapshotsWithoutAdditionalFilters() {
      // Arrange
      League league = leagues.save(new League("Identity League"));
      Team team = teams.save(new Team("Identity Team", league, false));
      Player player = new Player("Identity Player", team, "Forward");
      player.deactivate();
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "identity-snapshot-player");
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
    @DisplayName("Guarda contextos tipados y agrega exactamente un resultado por jugador")
    void persistsTypedAuditAndExclusiveResults() {
      // Arrange
      PlayerIdentitySyncRun run =
          runs.saveAndFlush(
              new PlayerIdentitySyncRun(PlayerIdentityPassType.CURRENT, null, Instant.now()));
      WhoScoredIdentityCandidate candidate = new WhoScoredIdentityCandidate(run.getId(), "101");
      WhoScoredIdentityContext context =
          new WhoScoredIdentityContext("José", null, "Team", 4, null, 1L, "League", null, null);
      candidate.observe(context);
      candidate.observe(context);
      candidates.saveAndFlush(candidate);
      PlayerIdentitySyncItem item =
          new PlayerIdentitySyncItem(
              run.getId(), 900001L, "José", 1L, "Team", 1L, "League", List.of());
      item.decide(
          PlayerIdentityItemResult.NO_MATCH,
          "TEAM_NOT_MATCHED",
          "Equipo distinto",
          List.of(candidate),
          null,
          Instant.now());
      items.saveAndFlush(item);
      entityManager.clear();
      // Act
      List<WhoScoredIdentityCandidate> observed = candidates.findByRunIdOrderById(run.getId());
      List<PlayerIdentitySyncItemRepository.ResultCount> counts = items.countResults(run.getId());
      // Assert
      assertThat(observed)
          .singleElement()
          .satisfies(saved -> assertThat(saved.getContexts()).containsExactly(context));
      assertThat(counts)
          .singleElement()
          .satisfies(
              count -> {
                assertThat(count.getResult()).isEqualTo(PlayerIdentityItemResult.NO_MATCH);
                assertThat(count.getAmount()).isEqualTo(1);
              });
    }

    @Test
    @DisplayName("La DB impide apropiarse de una identidad WhoScored de otro jugador")
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
  }
}
