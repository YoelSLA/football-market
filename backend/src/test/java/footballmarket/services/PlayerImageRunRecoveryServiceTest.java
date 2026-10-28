package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.records.LeagueCandidate;
import footballmarket.features.catalog.domain.records.PlayerCandidate;
import footballmarket.features.catalog.domain.records.PlayerSnapshot;
import footballmarket.features.catalog.domain.records.TeamCandidate;
import footballmarket.features.images.application.exceptions.PlayerImageSynchronizationException;
import footballmarket.integrations.TheSportsDbIntegration;
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
@org.junit.jupiter.api.Tag("integration")
class PlayerImageRunRecoveryServiceTest {
  @Autowired private PlayerImageRunRecoveryService recovery;
  @Autowired private PlayerCatalogService catalog;
  @Autowired private PlayerImageSynchronizationService synchronization;
  @MockitoBean private TheSportsDbIntegration integration;

  /**
   * Foto con liga y equipo explícitos: desde V5 el jugador solo se crea si su equipo resuelve, y el
   * enriquecimiento de imágenes omite los jugadores sin equipo resuelto.
   */
  private PlayerSnapshot photoWithTeam(String externalId, String name, boolean active) {
    PlayerCandidate player =
        new PlayerCandidate(externalId, name, "Team", "League", "Forward", null, null, "100");
    return new PlayerSnapshot(
        List.of(player),
        1,
        0,
        List.of(new LeagueCandidate("10", "League")),
        List.of(new TeamCandidate("100", "Team", "10", true)),
        List.of());
  }

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
      PlayerSnapshot snapshot = photoWithTeam("one", "Name", true);
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
