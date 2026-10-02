package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import footballmarket.integrations.TheSportsDbIntegration;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.services.exceptions.PlayerImageSynchronizationException;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class PlayerImageRunRecoveryServiceTest {
  @Autowired private PlayerImageRunRecoveryService recovery;
  @Autowired private PlayerCatalogService catalog;
  @Autowired private PlayerImageSynchronizationService synchronization;
  @MockitoBean private TheSportsDbIntegration integration;

  @Nested
  @DisplayName("Recuperación de ejecuciones anteriores")
  class Recovery {
    @Test
    @DisplayName("Sin ejecuciones abiertas, recuperar dos veces concluye normalmente")
    void emptyRecoveryIsIdempotent() {
      // Act / Assert
      assertThatCode(
              () -> {
                recovery.recover();
                recovery.recover();
              })
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Con una ejecución abierta, recuperar dos veces concluye normalmente")
    void openRecoveryIsIdempotent() throws Exception {
      // Arrange: se abre un run a través de la API productiva, sin acceso a persistencia.
      PlayerCandidate player =
          new PlayerCandidate("one", "Name", "Team", "League", "Forward", null, null);
      PlayerSnapshot snapshot = new PlayerSnapshot(List.of(player), 1, 0);
      catalog.applySynchronization(snapshot);
      CountDownLatch entered = new CountDownLatch(1);
      CountDownLatch release = new CountDownLatch(1);
      when(integration.search("Name"))
          .thenAnswer(
              invocation -> {
                entered.countDown();
                if (!release.await(10, TimeUnit.SECONDS)) {
                  throw new AssertionError("No se liberó la consulta simulada");
                }
                return List.of();
              });
      CompletableFuture<Void> running =
          CompletableFuture.runAsync(
              () -> {
                try {
                  synchronization.synchronize(false);
                } catch (PlayerImageSynchronizationException ignored) {
                  // La solicitud simulada termina después de que la recuperación cerró el run.
                }
              });
      try {
        if (!entered.await(10, TimeUnit.SECONDS)) {
          throw new AssertionError("No se abrió el item antes de iniciar la recuperación");
        }

        // Act / Assert: el estado durable se comprueba exclusivamente en Repository.
        assertThatCode(
                () -> {
                  recovery.recover();
                  recovery.recover();
                })
            .doesNotThrowAnyException();
      } finally {
        release.countDown();
        running.get(10, TimeUnit.SECONDS);
      }
    }
  }
}
