package footballmarket.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.orchestrators.PlayerSynchronizationOrchestrator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class PlayerSynchronizationOrchestratorTest {
  private final FootballDataPlayerService source = mock(FootballDataPlayerService.class);
  private final PlayerCatalogService catalog = mock(PlayerCatalogService.class);
  private final PlayerSynchronizationOrchestrator orchestrator =
      new PlayerSynchronizationOrchestrator(source, catalog);

  @Test
  void failureBeforeCompletionNeverAppliesLocalChanges() {
    when(this.source.fetchSnapshot()).thenThrow(new FootballDataUnavailableException());
    assertThatThrownBy(this.orchestrator::synchronize)
        .isInstanceOf(FootballDataUnavailableException.class);
    verifyNoInteractions(this.catalog);
  }

  @Test
  void appliesCompleteSnapshotExactlyOnce() {
    var snapshot = new PlayerSnapshot(List.of(), 0, 0);
    var result = new PlayerSynchronizationResult(0, 0, 0, 1, 0);
    when(this.source.fetchSnapshot()).thenReturn(snapshot);
    when(this.catalog.applySynchronization(snapshot)).thenReturn(result);
    assertThat(this.orchestrator.synchronize()).isSameAs(result);
    var order = inOrder(this.source, this.catalog);
    order.verify(this.source).fetchSnapshot();
    order.verify(this.catalog).applySynchronization(snapshot);
    order.verifyNoMoreInteractions();
  }

  @Test
  void serializesFetchAndApplyAcrossConcurrentRequests() throws Exception {
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var attempted = new CountDownLatch(1);
    var outstanding = new AtomicInteger();
    when(this.source.fetchSnapshot())
        .thenAnswer(
            invocation -> {
              assertThat(outstanding.incrementAndGet()).isEqualTo(1);
              entered.countDown();
              assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
              return new PlayerSnapshot(List.of(), 0, 0);
            });
    when(this.catalog.applySynchronization(any()))
        .thenAnswer(
            invocation -> {
              outstanding.decrementAndGet();
              return new PlayerSynchronizationResult(0, 0, 0, 0, 0);
            });
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(this.orchestrator::synchronize);
      assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
      var second =
          executor.submit(
              () -> {
                attempted.countDown();
                return this.orchestrator.synchronize();
              });
      assertThat(attempted.await(5, TimeUnit.SECONDS)).isTrue();
      release.countDown();
      first.get(5, TimeUnit.SECONDS);
      second.get(5, TimeUnit.SECONDS);
    } finally {
      release.countDown();
    }
    verify(this.catalog, times(2)).applySynchronization(any());
  }
}
