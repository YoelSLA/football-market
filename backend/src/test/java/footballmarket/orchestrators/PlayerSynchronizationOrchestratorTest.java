package footballmarket.orchestrators;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.services.FootballDataPlayerService;
import footballmarket.services.PlayerCatalogService;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class PlayerSynchronizationOrchestratorTest {
  private final FootballDataPlayerService source = mock(FootballDataPlayerService.class);
  private final PlayerCatalogService catalog = mock(PlayerCatalogService.class);
  private final PlayerSynchronizationOrchestrator orchestrator =
      new PlayerSynchronizationOrchestrator(source, catalog);

  @Nested
  @DisplayName("Coordinación de la sincronización")
  class Synchronization {
    @Test
    @DisplayName("No modifica el catálogo cuando falla la obtención de jugadores")
    void evitaCambiosLocalesSiFallaLaObtencion() {
      // Arrange
      when(source.fetchSnapshot()).thenThrow(new FootballDataUnavailableException());

      // Act / Assert
      assertThatThrownBy(orchestrator::synchronize)
          .isInstanceOf(FootballDataUnavailableException.class);

      // Verify
      verifyNoInteractions(catalog);
    }

    @Test
    @DisplayName(
        "Obtiene únicamente la foto de Football-Data y la entrega sin consultas de enriquecimiento")
    void aplicaElConjuntoCompletoUnaSolaVez() {
      // Arrange
      PlayerSnapshot snapshot = new PlayerSnapshot(List.of(), 0, 0);
      PlayerSynchronizationResult result = new PlayerSynchronizationResult(0, 0, 0, 1, 0);
      when(source.fetchSnapshot()).thenReturn(snapshot);
      when(catalog.applySynchronization(snapshot)).thenReturn(result);

      // Act
      assertThat(orchestrator.synchronize()).isSameAs(result);

      // Verify
      InOrder order = inOrder(source, catalog);
      order.verify(source).fetchSnapshot();
      order.verify(catalog).applySynchronization(snapshot);
      order.verifyNoMoreInteractions();
      verifyNoMoreInteractions(source, catalog);
    }
  }

  @Nested
  @DisplayName("Sincronización concurrente")
  class Concurrency {
    @Test
    @DisplayName("Dos sincronizaciones concurrentes no intercalan obtención y aplicación")
    void serializaObtencionYAplicacionEntreSolicitudesConcurrentes() throws Exception {
      // Arrange
      CountDownLatch entered = new CountDownLatch(1);
      CountDownLatch release = new CountDownLatch(1);
      CountDownLatch attempted = new CountDownLatch(1);
      AtomicInteger outstanding = new AtomicInteger();
      PlayerSnapshot snapshot = new PlayerSnapshot(List.of(), 0, 0);
      PlayerSynchronizationResult result = new PlayerSynchronizationResult(0, 0, 0, 0, 0);
      when(source.fetchSnapshot())
          .thenAnswer(
              invocation -> {
                assertThat(outstanding.incrementAndGet()).isEqualTo(1);
                entered.countDown();
                assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                return snapshot;
              });
      when(catalog.applySynchronization(any()))
          .thenAnswer(
              invocation -> {
                outstanding.decrementAndGet();
                return result;
              });

      // Act / Assert
      try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
        Future<PlayerSynchronizationResult> first = executor.submit(orchestrator::synchronize);
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        Future<PlayerSynchronizationResult> second =
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

      // Verify
      verify(catalog, times(2)).applySynchronization(any());
    }
  }
}
