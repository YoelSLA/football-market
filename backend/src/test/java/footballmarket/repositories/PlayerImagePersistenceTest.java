package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import footballmarket.integrations.TheSportsDbIntegration;
import footballmarket.models.Player;
import footballmarket.models.PlayerImageResolution;
import footballmarket.models.PlayerImageSyncRun;
import footballmarket.models.PlayerImageSyncRunItem;
import footballmarket.models.enums.PlayerImageResolutionStatus;
import footballmarket.models.enums.PlayerImageSyncRunItemResult;
import footballmarket.models.enums.PlayerImageSyncRunStatus;
import footballmarket.models.enums.PlayerProvider;
import footballmarket.models.records.PlayerImageSyncSummary;
import footballmarket.services.PlayerImageAuditService;
import footballmarket.services.PlayerImageRunRecoveryService;
import footballmarket.services.PlayerImageSynchronizationService;
import footballmarket.services.exceptions.PlayerImageResolutionPersistenceException;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "the-sports-db.audit-retention-days=30")
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class PlayerImagePersistenceTest {
  @Autowired private PlayerRepository players;
  @Autowired private PlayerExternalReferenceRepository references;
  @Autowired private PlayerImageResolutionRepository resolutions;
  @Autowired private PlayerImageSyncRunRepository runs;
  @Autowired private PlayerImageSyncRunItemRepository items;
  @Autowired private PlatformTransactionManager manager;
  @Autowired private PlayerImageSynchronizationService synchronization;
  @Autowired private PlayerImageAuditService audit;
  @Autowired private PlayerImageRunRecoveryService recovery;
  @MockitoBean private TheSportsDbIntegration integration;

  @Nested
  @DisplayName("Persistencia de imágenes y auditoría")
  class Persistence {
    @Test
    @DisplayName("Recorre todos los jugadores por ID sin depender del estado active")
    void traversesAllPlayers() {
      TransactionTemplate transaction = new TransactionTemplate(manager);
      Player first =
          transaction.execute(
              status -> {
                Player player = new Player("One", "Team", "League", "Forward");
                return players.saveAndFlush(player);
              });
      Player second =
          transaction.execute(
              status -> {
                Player player = new Player("Two", "Team", "League", "Forward");
                player.deactivate();
                return players.saveAndFlush(player);
              });
      assertThat(players.findFirstByIdGreaterThanOrderByIdAsc(0L))
          .hasValueSatisfying(player -> assertThat(player.getId()).isEqualTo(first.getId()));
      assertThat(players.findFirstByIdGreaterThanOrderByIdAsc(first.getId()))
          .hasValueSatisfying(player -> assertThat(player.getId()).isEqualTo(second.getId()));
      assertThat(players.findFirstByIdGreaterThanOrderByIdAsc(second.getId())).isEmpty();
    }

    @Test
    @DisplayName("La resolución persiste por jugador conservando imágenes y referencias")
    void persistsResolutionSeparately() {
      TransactionTemplate transaction = new TransactionTemplate(manager);
      Player player =
          transaction.execute(
              status -> {
                Player created = new Player("Name", "Team", "League", "Forward");
                created.addExternalReference(PlayerProvider.THE_SPORTS_DB, "123");
                created.applyImages("https://thesportsdb.com/a", "https://thesportsdb.com/b");
                players.saveAndFlush(created);
                resolutions.saveAndFlush(new PlayerImageResolution(created));
                return created;
              });
      assertThat(resolutions.findById(player.getId()))
          .hasValueSatisfying(
              resolution ->
                  assertThat(resolution.getStatus())
                      .isEqualTo(PlayerImageResolutionStatus.PENDING));
      assertThat(players.findById(player.getId()))
          .hasValueSatisfying(
              saved -> {
                assertThat(saved.getImageUrl()).isEqualTo("https://thesportsdb.com/a");
                assertThat(saved.getFallbackImageUrl()).isEqualTo("https://thesportsdb.com/b");
              });
    }

    @Test
    @DisplayName("Una ejecución no admite dos items para el mismo jugador")
    void rejectsDuplicateItem() {
      TransactionTemplate transaction = new TransactionTemplate(manager);
      Player player =
          transaction.execute(
              status -> players.saveAndFlush(new Player("Name", "Team", "League", "Forward")));
      PlayerImageSyncRun run =
          transaction.execute(
              status ->
                  runs.saveAndFlush(
                      new PlayerImageSyncRun(false, Instant.parse("2026-10-01T00:00:00Z"))));
      transaction.executeWithoutResult(
          status ->
              items.saveAndFlush(
                  new PlayerImageSyncRunItem(run, player, PlayerImageResolutionStatus.PENDING)));
      assertThatThrownBy(
              () ->
                  transaction.executeWithoutResult(
                      status ->
                          items.saveAndFlush(
                              new PlayerImageSyncRunItem(
                                  run, player, PlayerImageResolutionStatus.PENDING))))
          .isInstanceOf(DataIntegrityViolationException.class);
      assertThat(
              items
                  .findByRunIdOrderByIdAsc(
                      run.getId(), org.springframework.data.domain.PageRequest.of(0, 20))
                  .getTotalElements())
          .isEqualTo(1);
    }

    @Test
    @DisplayName("La referencia externa dura aunque la resolución quede sin imagen")
    void referenceOutlivesMissingImage() {
      TransactionTemplate transaction = new TransactionTemplate(manager);
      Player player =
          transaction.execute(
              status -> {
                Player created = new Player("Name", "Team", "League", "Forward");
                created.addExternalReference(PlayerProvider.THE_SPORTS_DB, "123");
                players.saveAndFlush(created);
                PlayerImageResolution resolution = new PlayerImageResolution(created);
                resolution.recordAttempt(PlayerImageResolutionStatus.NOT_FOUND, Instant.now());
                resolutions.saveAndFlush(resolution);
                return created;
              });
      assertThat(resolutions.findById(player.getId()))
          .hasValueSatisfying(
              resolution ->
                  assertThat(resolution.getStatus())
                      .isEqualTo(PlayerImageResolutionStatus.NOT_FOUND));
      assertThat(players.findById(player.getId()))
          .hasValueSatisfying(saved -> assertThat(saved.getImageUrl()).isNull());
      assertThat(references.findByProviderAndExternalId(PlayerProvider.THE_SPORTS_DB, "123"))
          .hasValueSatisfying(
              reference -> assertThat(reference.getPlayer().getId()).isEqualTo(player.getId()));
    }

    @Test
    @DisplayName("Los items confirmados se conservan incrementalmente y se borran junto con el run")
    void retainsConfirmedItemsAndDeletesWithoutOrphans() {
      TransactionTemplate transaction = new TransactionTemplate(manager);
      Player player =
          transaction.execute(
              status -> players.saveAndFlush(new Player("Name", "Team", "League", "Forward")));
      PlayerImageSyncRun run =
          transaction.execute(
              status ->
                  runs.saveAndFlush(
                      new PlayerImageSyncRun(false, Instant.parse("2026-10-01T00:00:00Z"))));
      transaction.executeWithoutResult(
          status -> {
            PlayerImageSyncRunItem item =
                new PlayerImageSyncRunItem(run, player, PlayerImageResolutionStatus.PENDING);
            item.finish(
                PlayerImageResolutionStatus.NOT_FOUND,
                PlayerImageSyncRunItemResult.NOT_FOUND,
                null,
                false,
                false,
                false,
                false,
                "Sin resultados",
                Instant.now());
            items.saveAndFlush(item);
          });
      assertThat(
              items
                  .findByRunIdOrderByIdAsc(
                      run.getId(), org.springframework.data.domain.PageRequest.of(0, 20))
                  .getTotalElements())
          .isEqualTo(1);
      transaction.executeWithoutResult(
          status -> {
            items.deleteByRunId(run.getId());
            runs.deleteById(run.getId());
          });
      assertThat(
              items
                  .findByRunIdOrderByIdAsc(
                      run.getId(), org.springframework.data.domain.PageRequest.of(0, 20))
                  .getTotalElements())
          .isZero();
    }

    @Test
    @DisplayName("Repara la resolución faltante durante la sincronización sin API de preparación")
    void repairsMissingResolution() {
      TransactionTemplate transaction = new TransactionTemplate(manager);
      Player player =
          transaction.execute(
              status -> players.saveAndFlush(new Player("Name", "Team", "League", "Forward")));
      assertThat(resolutions.findById(player.getId())).isEmpty();
      when(integration.search("Name")).thenReturn(List.of());

      PlayerImageSyncSummary summary = synchronization.synchronize(false);

      assertThat(summary.counters().evaluated()).isEqualTo(1);
      assertThat(summary.counters().processed()).isEqualTo(1);
      assertThat(resolutions.findById(player.getId()))
          .hasValueSatisfying(
              resolution -> {
                assertThat(resolution.getStatus()).isEqualTo(PlayerImageResolutionStatus.NOT_FOUND);
                assertThat(resolution.getLastAttemptAt()).isNotNull();
              });
      assertThat(
              items
                  .findByRunIdOrderByIdAsc(
                      summary.id(), org.springframework.data.domain.PageRequest.of(0, 20))
                  .getContent())
          .singleElement()
          .satisfies(
              item -> {
                assertThat(item.getPreviousState()).isEqualTo(PlayerImageResolutionStatus.PENDING);
                assertThat(item.getFinalState()).isEqualTo(PlayerImageResolutionStatus.NOT_FOUND);
              });
    }

    @Test
    @DisplayName("Historial e items respetan páginas de 20 y ordenan los empates por ID")
    void pagesHistoryAndItems() {
      // Arrange
      TransactionTemplate transaction = new TransactionTemplate(manager);
      Instant started = Instant.now().minusSeconds(60);
      Player player =
          transaction.execute(
              status -> players.saveAndFlush(new Player("Name", "Team", "League", "Forward")));
      PlayerImageSyncRun first =
          transaction.execute(status -> runs.saveAndFlush(new PlayerImageSyncRun(false, started)));
      PlayerImageSyncRun second =
          transaction.execute(
              status -> {
                PlayerImageSyncRun run = runs.saveAndFlush(new PlayerImageSyncRun(false, started));
                items.saveAndFlush(
                    new PlayerImageSyncRunItem(run, player, PlayerImageResolutionStatus.PENDING));
                return run;
              });

      // Act
      Page<PlayerImageSyncRun> history =
          runs.findAllByOrderByStartedAtDescIdDesc(PageRequest.of(0, 20));
      Page<PlayerImageSyncRunItem> detail =
          items.findByRunIdOrderByIdAsc(second.getId(), PageRequest.of(0, 20));

      // Assert
      assertThat(history.getContent())
          .extracting(PlayerImageSyncRun::getId)
          .containsExactly(second.getId(), first.getId());
      assertThat(history.getSize()).isEqualTo(20);
      assertThat(detail.getSize()).isEqualTo(20);
      assertThat(detail.getContent())
          .extracting(PlayerImageSyncRunItem::getPlayer)
          .extracting(Player::getId)
          .containsExactly(player.getId());
    }

    @Test
    @DisplayName("Retención borra ejecuciones vencidas e items, pero conserva datos del jugador")
    void retainsPlayerDataWhenPurgingHistory() {
      // Arrange
      TransactionTemplate transaction = new TransactionTemplate(manager);
      Player player =
          transaction.execute(
              status -> {
                Player created = new Player("Name", "Team", "League", "Forward");
                created.addExternalReference(PlayerProvider.THE_SPORTS_DB, "123");
                created.applyImages("https://thesportsdb.com/photo", null);
                players.saveAndFlush(created);
                resolutions.saveAndFlush(new PlayerImageResolution(created));
                return created;
              });
      PlayerImageSyncRun old =
          transaction.execute(
              status -> {
                Instant start = Instant.now().minusSeconds(86400L * 32);
                PlayerImageSyncRun run = runs.saveAndFlush(new PlayerImageSyncRun(false, start));
                items.saveAndFlush(
                    new PlayerImageSyncRunItem(run, player, PlayerImageResolutionStatus.PENDING));
                run.complete(start.plusSeconds(60));
                return run;
              });
      PlayerImageSyncRun recent =
          transaction.execute(
              status -> {
                Instant start = Instant.now().minusSeconds(60);
                PlayerImageSyncRun run = runs.saveAndFlush(new PlayerImageSyncRun(false, start));
                run.complete(start.plusSeconds(10));
                return run;
              });

      // Act
      Page<footballmarket.models.records.PlayerImageSyncSummary> history = audit.getRuns(0, 20);

      // Assert
      assertThat(history.getContent())
          .extracting(footballmarket.models.records.PlayerImageSyncSummary::id)
          .containsExactly(recent.getId());
      assertThat(runs.existsById(old.getId())).isFalse();
      assertThat(items.findByRunIdOrderByIdAsc(old.getId(), PageRequest.of(0, 20))).isEmpty();
      assertThat(resolutions.existsById(player.getId())).isTrue();
      assertThat(references.findByProviderAndExternalId(PlayerProvider.THE_SPORTS_DB, "123"))
          .isPresent();
      assertThat(players.findById(player.getId()))
          .hasValueSatisfying(
              saved -> assertThat(saved.getImageUrl()).isEqualTo("https://thesportsdb.com/photo"));
    }

    @Test
    @DisplayName(
        "La recuperación interrumpe solo items inconclusos y conserva contadores e imágenes")
    void recoversOpenRunWithoutChangingFinishedItems() {
      // Arrange: esta prueba de persistencia puede preparar directamente un run huérfano.
      TransactionTemplate transaction = new TransactionTemplate(manager);
      Instant started = Instant.parse("2020-01-01T00:00:00Z");
      Instant finished = started.plusSeconds(1);
      Player first =
          transaction.execute(
              status -> {
                Player created = new Player("First", "Team", "League", "Forward");
                created.applyImages("https://thesportsdb.com/photo", null);
                players.saveAndFlush(created);
                PlayerImageResolution resolution = new PlayerImageResolution(created);
                resolution.recordAttempt(PlayerImageResolutionStatus.FOUND, finished);
                resolutions.saveAndFlush(resolution);
                return created;
              });
      Player second =
          transaction.execute(
              status -> {
                Player created =
                    players.saveAndFlush(new Player("Second", "Team", "League", "Forward"));
                PlayerImageResolution resolution = new PlayerImageResolution(created);
                resolution.recordAttempt(PlayerImageResolutionStatus.RETRYABLE_ERROR, finished);
                resolutions.saveAndFlush(resolution);
                return created;
              });
      Long runId =
          transaction.execute(
              status -> {
                PlayerImageSyncRun run = runs.saveAndFlush(new PlayerImageSyncRun(false, started));
                run.evaluate();
                PlayerImageSyncRunItem completed =
                    new PlayerImageSyncRunItem(run, first, PlayerImageResolutionStatus.PENDING);
                completed.finish(
                    PlayerImageResolutionStatus.FOUND,
                    PlayerImageSyncRunItemResult.FOUND,
                    null,
                    true,
                    true,
                    false,
                    false,
                    "Imagen asociada",
                    finished);
                items.saveAndFlush(completed);
                run.result(PlayerImageSyncRunItemResult.FOUND, null, true, false);
                run.evaluate();
                items.saveAndFlush(
                    new PlayerImageSyncRunItem(
                        run, second, PlayerImageResolutionStatus.RETRYABLE_ERROR));
                runs.flush();
                return run.getId();
              });

      // Act
      recovery.recover();
      PlayerImageSyncRun recovered = runs.findById(runId).orElseThrow();
      List<PlayerImageSyncRunItem> recoveredItems =
          items.findByRunIdOrderByIdAsc(runId, PageRequest.of(0, 20)).getContent();
      recovery.recover();

      // Assert
      assertThat(recovered.getStatus()).isEqualTo(PlayerImageSyncRunStatus.FAILED);
      assertThat(recovered.getFailureReason()).isEqualTo("INTERRUPTED_BY_RESTART");
      assertThat(recovered.getFinishedAt()).isNotNull();
      assertThat(recovered.counters().evaluated()).isEqualTo(2);
      assertThat(recovered.counters().processed()).isEqualTo(1);
      assertThat(recovered.counters().found()).isEqualTo(1);
      assertThat(recovered.counters().failed()).isZero();
      assertThat(recovered.counters().interrupted()).isEqualTo(1);
      assertThat(recoveredItems).hasSize(2);
      PlayerImageSyncRunItem completed = recoveredItems.get(0);
      assertThat(completed.getResult()).isEqualTo(PlayerImageSyncRunItemResult.FOUND);
      assertThat(completed.getFinalState()).isEqualTo(PlayerImageResolutionStatus.FOUND);
      assertThat(completed.getFinishedAt()).isEqualTo(finished);
      PlayerImageSyncRunItem interrupted = recoveredItems.get(1);
      assertThat(interrupted.getResult()).isEqualTo(PlayerImageSyncRunItemResult.INTERRUPTED);
      assertThat(interrupted.getPreviousState())
          .isEqualTo(PlayerImageResolutionStatus.RETRYABLE_ERROR);
      assertThat(interrupted.getFinalState())
          .isEqualTo(PlayerImageResolutionStatus.RETRYABLE_ERROR);
      assertThat(interrupted.getFinishedAt()).isNotNull();
      assertThat(interrupted.isErrorOccurred()).isTrue();
      assertThat(resolutions.findById(second.getId()))
          .hasValueSatisfying(
              resolution ->
                  assertThat(resolution.getStatus())
                      .isEqualTo(PlayerImageResolutionStatus.RETRYABLE_ERROR));
      assertThat(players.findById(first.getId()))
          .hasValueSatisfying(
              player ->
                  assertThat(player.getImageUrl()).isEqualTo("https://thesportsdb.com/photo"));
      PlayerImageSyncRun unchanged = runs.findById(runId).orElseThrow();
      assertThat(unchanged.getFinishedAt()).isEqualTo(recovered.getFinishedAt());
      assertThat(unchanged.counters()).isEqualTo(recovered.counters());
      List<PlayerImageSyncRunItem> unchangedItems =
          items.findByRunIdOrderByIdAsc(runId, PageRequest.of(0, 20)).getContent();
      assertThat(unchangedItems)
          .extracting(PlayerImageSyncRunItem::getResult)
          .containsExactly(
              PlayerImageSyncRunItemResult.FOUND, PlayerImageSyncRunItemResult.INTERRUPTED);
      assertThat(unchangedItems.get(0).getFinishedAt()).isEqualTo(finished);
      assertThat(unchangedItems.get(1).getFinishedAt()).isEqualTo(interrupted.getFinishedAt());
    }

    @Test
    @DisplayName("Un error durante la recuperación deja el run abierto y revierte items parciales")
    void recoveryFailureRollsBackCurrentRun() {
      // Arrange: la segunda resolución falta para provocar un error persistente controlado.
      TransactionTemplate transaction = new TransactionTemplate(manager);
      Player first =
          transaction.execute(
              status -> {
                Player created =
                    players.saveAndFlush(new Player("First", "Team", "League", "Forward"));
                resolutions.saveAndFlush(new PlayerImageResolution(created));
                return created;
              });
      Player second =
          transaction.execute(
              status -> players.saveAndFlush(new Player("Second", "Team", "League", "Forward")));
      Long runId =
          transaction.execute(
              status -> {
                PlayerImageSyncRun run =
                    runs.saveAndFlush(
                        new PlayerImageSyncRun(false, Instant.parse("2020-01-01T00:00:00Z")));
                run.evaluate();
                items.saveAndFlush(
                    new PlayerImageSyncRunItem(run, first, PlayerImageResolutionStatus.PENDING));
                run.evaluate();
                items.saveAndFlush(
                    new PlayerImageSyncRunItem(run, second, PlayerImageResolutionStatus.PENDING));
                runs.flush();
                return run.getId();
              });

      // Act / Assert
      assertThatThrownBy(recovery::recover)
          .isInstanceOf(PlayerImageResolutionPersistenceException.class);

      // Verify: la transacción del run no deja ni contadores ni items parciales.
      PlayerImageSyncRun run = runs.findById(runId).orElseThrow();
      assertThat(run.getStatus()).isEqualTo(PlayerImageSyncRunStatus.RUNNING);
      assertThat(run.counters().interrupted()).isZero();
      assertThat(items.findByRunIdOrderByIdAsc(runId, PageRequest.of(0, 20)).getContent())
          .hasSize(2)
          .allSatisfy(
              item -> {
                assertThat(item.getResult()).isNull();
                assertThat(item.getFinishedAt()).isNull();
              });
    }
  }
}
