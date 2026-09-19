package footballmarket.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import footballmarket.integrations.footballdata.FootballDataUnavailableException;
import footballmarket.models.PlayerSnapshot;
import footballmarket.models.PlayerSynchronizationResult;
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
    when(source.fetchSnapshot()).thenThrow(new FootballDataUnavailableException());
    assertThatThrownBy(orchestrator::synchronize)
        .isInstanceOf(FootballDataUnavailableException.class);
    verifyNoInteractions(catalog);
  }

  @Test
  void appliesCompleteSnapshotExactlyOnce() {
    var snapshot = new PlayerSnapshot(List.of(), 0, 0);
    var result = new PlayerSynchronizationResult(0, 0, 0, 1, 0);
    when(source.fetchSnapshot()).thenReturn(snapshot);
    when(catalog.applySynchronization(snapshot)).thenReturn(result);
    assertThat(orchestrator.synchronize()).isSameAs(result);
    var order = inOrder(source, catalog);
    order.verify(source).fetchSnapshot();
    order.verify(catalog).applySynchronization(snapshot);
    order.verifyNoMoreInteractions();
  }

  @Test
  void serializesFetchAndApplyAcrossConcurrentRequests() throws Exception {
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var attempted = new CountDownLatch(1);
    var outstanding = new AtomicInteger();
    when(source.fetchSnapshot())
        .thenAnswer(
            invocation -> {
              assertThat(outstanding.incrementAndGet()).isEqualTo(1);
              entered.countDown();
              assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
              return new PlayerSnapshot(List.of(), 0, 0);
            });
    when(catalog.applySynchronization(any()))
        .thenAnswer(
            invocation -> {
              outstanding.decrementAndGet();
              return new PlayerSynchronizationResult(0, 0, 0, 0, 0);
            });
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(orchestrator::synchronize);
      assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
      var second =
          executor.submit(
              () -> {
                attempted.countDown();
                return orchestrator.synchronize();
              });
      assertThat(attempted.await(5, TimeUnit.SECONDS)).isTrue();
      release.countDown();
      first.get(5, TimeUnit.SECONDS);
      second.get(5, TimeUnit.SECONDS);
    } finally {
      release.countDown();
    }
    verify(catalog, times(2)).applySynchronization(any());
  }
}
