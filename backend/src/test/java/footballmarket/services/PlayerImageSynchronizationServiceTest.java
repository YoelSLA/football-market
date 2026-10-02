package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import footballmarket.integrations.TheSportsDbIntegration;
import footballmarket.integrations.TheSportsDbIntegration.PlayerData;
import footballmarket.integrations.exceptions.InvalidTheSportsDbResponseException;
import footballmarket.integrations.exceptions.TheSportsDbUnavailableException;
import footballmarket.models.Player;
import footballmarket.models.enums.PlayerImageSyncRunStatus;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerImageSyncSummary;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.services.exceptions.PlayerImageSyncInProgressException;
import footballmarket.services.exceptions.PlayerImageSynchronizationException;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.LocalDate;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class PlayerImageSynchronizationServiceTest {
  @Autowired private PlayerImageSynchronizationService synchronizationService;
  @Autowired private PlayerCatalogService catalogService;
  @MockitoBean private TheSportsDbIntegration integration;

  @Nested
  @DisplayName("Sincronización manual de imágenes")
  class ManualSynchronization {
    @Test
    @DisplayName("Registra identidad aun sin foto y termina COMPLETED con NOT_FOUND")
    void persistsIdentityWithoutImage() {
      PlayerCandidate candidate =
          new PlayerCandidate("a", "Name", "Team", "League", "Forward", null, null);
      PlayerSnapshot snapshot = new PlayerSnapshot(List.of(candidate), 1, 0);
      catalogService.applySynchronization(snapshot);
      PlayerData result =
          new PlayerData("123", "Name", null, "Team", "Soccer", null, null, null, null);
      when(integration.search("Name")).thenReturn(List.of(result));

      PlayerImageSyncSummary summary = synchronizationService.synchronize(false);

      assertThat(summary.status()).isEqualTo(PlayerImageSyncRunStatus.COMPLETED);
      assertThat(summary.counters().evaluated()).isEqualTo(1);
      assertThat(summary.counters().processed()).isEqualTo(1);
      assertThat(summary.counters().notFound()).isEqualTo(1);
      verify(integration).search("Name");
    }

    @Test
    @DisplayName("Una imagen válida se persiste y se expone por el catálogo local")
    void persistsLocalImage() {
      PlayerCandidate candidate =
          new PlayerCandidate("b", "Name", "Team", "League", "Forward", null, null);
      PlayerSnapshot snapshot = new PlayerSnapshot(List.of(candidate), 1, 0);
      catalogService.applySynchronization(snapshot);
      PlayerData result =
          new PlayerData(
              "123",
              "Name",
              null,
              "Team",
              "Soccer",
              null,
              null,
              "https://thesportsdb.com/cutout",
              null);
      when(integration.search("Name")).thenReturn(List.of(result));

      PlayerImageSyncSummary summary = synchronizationService.synchronize(false);

      assertThat(summary.status()).isEqualTo(PlayerImageSyncRunStatus.COMPLETED);
      assertThat(summary.counters().found()).isEqualTo(1);
      assertThat(catalogService.getActivePlayers(0, 20).getContent())
          .extracting(Player::getImageUrl)
          .containsExactly("https://thesportsdb.com/cutout");
    }

    @Test
    @DisplayName("Omite NOT_FOUND dentro de su ventana sin volver a consultar al proveedor")
    void skipsRecentNotFound() {
      PlayerCandidate candidate =
          new PlayerCandidate("c", "Name", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      when(integration.search("Name")).thenReturn(List.of());
      synchronizationService.synchronize(false);

      PlayerImageSyncSummary skipped = synchronizationService.synchronize(false);

      assertThat(skipped.counters().evaluated()).isEqualTo(1);
      assertThat(skipped.counters().processed()).isZero();
      assertThat(skipped.counters().skippedRetryWindow()).isEqualTo(1);
      verify(integration, times(1)).search("Name");
    }

    @Test
    @DisplayName("La búsqueda no consulta a un jugador inactivo al empezar su turno")
    void excludesInactivePlayer() {
      PlayerCandidate active =
          new PlayerCandidate("d", "Active", "Team", "League", "Forward", null, null);
      PlayerCandidate inactive =
          new PlayerCandidate("e", "Inactive", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(active, inactive), 2, 0));
      catalogService.applySynchronization(new PlayerSnapshot(List.of(active), 1, 0));
      when(integration.search("Active")).thenReturn(List.of());

      PlayerImageSyncSummary result = synchronizationService.synchronize(true);

      assertThat(result.counters().evaluated()).isEqualTo(1);
      assertThat(result.counters().processed()).isEqualTo(1);
      verify(integration, never()).search("Inactive");
    }

    @Test
    @DisplayName("Una referencia resuelta se consulta por ID aun al forzar el refresco")
    void neverRematchesPersistedIdentity() {
      PlayerCandidate candidate =
          new PlayerCandidate("f", "Name", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      PlayerData photo =
          new PlayerData(
              "123",
              "Name",
              null,
              "Team",
              "Soccer",
              null,
              null,
              "https://thesportsdb.com/old",
              null);
      when(integration.search("Name")).thenReturn(List.of(photo));
      synchronizationService.synchronize(false);
      PlayerData refresh =
          new PlayerData(
              "123",
              "Name",
              null,
              "Team",
              "Soccer",
              null,
              null,
              "https://thesportsdb.com/new",
              null);
      when(integration.lookup("123")).thenReturn(List.of(refresh));

      PlayerImageSyncSummary result = synchronizationService.synchronize(true);

      assertThat(result.counters().found()).isEqualTo(1);
      verify(integration, times(1)).search("Name");
      verify(integration).lookup("123");
    }

    @Test
    @DisplayName("Un lookup vacío conserva la imagen anterior y cuenta NOT_FOUND")
    void emptyLookupPreservesImage() {
      PlayerCandidate candidate =
          new PlayerCandidate("g", "Name", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      PlayerData photo =
          new PlayerData(
              "123",
              "Name",
              null,
              "Team",
              "Soccer",
              null,
              null,
              "https://thesportsdb.com/old",
              null);
      when(integration.search("Name")).thenReturn(List.of(photo));
      synchronizationService.synchronize(false);
      when(integration.lookup("123")).thenReturn(List.of());

      PlayerImageSyncSummary result = synchronizationService.synchronize(true);

      assertThat(result.counters().notFound()).isEqualTo(1);
      assertThat(result.status()).isEqualTo(PlayerImageSyncRunStatus.COMPLETED);
      assertThat(catalogService.getActivePlayers(0, 20).getContent())
          .extracting(Player::getImageUrl)
          .containsExactly("https://thesportsdb.com/old");
    }

    @Test
    @DisplayName("Una identidad contradictoria por ID falla individualmente sin borrar la imagen")
    void incompatibleIdentityIsIndividualFailure() {
      PlayerCandidate candidate =
          new PlayerCandidate("h", "Name", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      PlayerData original =
          new PlayerData(
              "123",
              "Name",
              null,
              "Team",
              "Soccer",
              null,
              null,
              "https://thesportsdb.com/old",
              null);
      when(integration.search("Name")).thenReturn(List.of(original));
      synchronizationService.synchronize(false);
      PlayerData incompatible =
          new PlayerData(
              "123",
              "Other",
              null,
              "Team",
              "Soccer",
              null,
              null,
              "https://thesportsdb.com/other",
              null);
      when(integration.lookup("123")).thenReturn(List.of(incompatible));

      PlayerImageSyncSummary result = synchronizationService.synchronize(true);

      assertThat(result.status()).isEqualTo(PlayerImageSyncRunStatus.PARTIAL);
      assertThat(result.counters().failed()).isEqualTo(1);
      assertThat(catalogService.getActivePlayers(0, 20).getContent())
          .extracting(Player::getImageUrl)
          .containsExactly("https://thesportsdb.com/old");
    }

    @Test
    @DisplayName("La ausencia de fecha y nacionalidad del proveedor no contradice al jugador")
    void absentOptionalIdentityIsNotContradiction() {
      PlayerCandidate candidate =
          new PlayerCandidate(
              "h2", "Name", "Team", "League", "Forward", LocalDate.of(1990, 1, 1), "Spain");
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      PlayerData initial =
          new PlayerData("123", "Name", null, "Team", "Soccer", null, null, null, null);
      when(integration.search("Name")).thenReturn(List.of(initial));
      synchronizationService.synchronize(false);
      when(integration.lookup("123")).thenReturn(List.of(initial));

      PlayerImageSyncSummary result = synchronizationService.synchronize(true);

      assertThat(result.status()).isEqualTo(PlayerImageSyncRunStatus.COMPLETED);
      assertThat(result.counters().failed()).isZero();
      assertThat(result.counters().notFound()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tres reintentos luego del inicial agotan un error temporal")
    void exhaustsDefaultRetries() {
      PlayerCandidate candidate =
          new PlayerCandidate("i", "Name", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      when(integration.search("Name")).thenThrow(new TheSportsDbUnavailableException());

      PlayerImageSyncSummary result = synchronizationService.synchronize(false);

      assertThat(result.status()).isEqualTo(PlayerImageSyncRunStatus.PARTIAL);
      assertThat(result.counters().retryableErrors()).isEqualTo(1);
      verify(integration, times(4)).search("Name");
    }

    @Test
    @DisplayName("Un resultado temporal agotado vuelve a ser elegible en el run siguiente")
    void retryableErrorEligibleAgain() {
      PlayerCandidate candidate =
          new PlayerCandidate("j", "Name", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      PlayerData recovered =
          new PlayerData(
              "123",
              "Name",
              null,
              "Team",
              "Soccer",
              null,
              null,
              "https://thesportsdb.com/new",
              null);
      when(integration.search("Name"))
          .thenThrow(new TheSportsDbUnavailableException())
          .thenThrow(new TheSportsDbUnavailableException())
          .thenThrow(new TheSportsDbUnavailableException())
          .thenThrow(new TheSportsDbUnavailableException())
          .thenReturn(List.of(recovered));
      synchronizationService.synchronize(false);

      PlayerImageSyncSummary result = synchronizationService.synchronize(false);

      assertThat(result.counters().found()).isEqualTo(1);
      assertThat(result.counters().processed()).isEqualTo(1);
      verify(integration, times(5)).search("Name");
    }

    @Test
    @DisplayName("Un error permanente solo es elegible de nuevo con force")
    void permanentlyFailedOnlyWithForce() {
      PlayerCandidate candidate =
          new PlayerCandidate("k", "Name", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      when(integration.search("Name")).thenThrow(new InvalidTheSportsDbResponseException());
      PlayerImageSyncSummary failure = synchronizationService.synchronize(false);
      PlayerImageSyncSummary skipped = synchronizationService.synchronize(false);

      assertThat(failure.status()).isEqualTo(PlayerImageSyncRunStatus.PARTIAL);
      assertThat(skipped.counters().skippedFailed()).isEqualTo(1);
      assertThat(skipped.counters().processed()).isZero();
      synchronizationService.synchronize(true);
      verify(integration, times(2)).search("Name");
    }

    @Test
    @DisplayName("La segunda invocación concurrente falla sin crear otro run")
    void rejectsConcurrentExecution() throws Exception {
      PlayerCandidate candidate =
          new PlayerCandidate("l", "Name", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      CountDownLatch started = new CountDownLatch(1);
      CountDownLatch release = new CountDownLatch(1);
      doAnswer(
              invocation -> {
                started.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) {
                  throw new AssertionError("La sincronización no fue liberada");
                }
                return List.of();
              })
          .when(integration)
          .search("Name");
      CompletableFuture<PlayerImageSyncSummary> first =
          CompletableFuture.supplyAsync(() -> synchronizationService.synchronize(false));
      try {
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> synchronizationService.synchronize(false))
            .isInstanceOf(PlayerImageSyncInProgressException.class);
      } finally {
        release.countDown();
      }
      assertThat(first.get(5, TimeUnit.SECONDS).status())
          .isEqualTo(PlayerImageSyncRunStatus.COMPLETED);
      verify(integration, times(1)).search("Name");
    }

    @Test
    @DisplayName("Un conflicto de referencia falla solo al segundo jugador y deja PARTIAL")
    void conflictIsIndividual() {
      PlayerCandidate first =
          new PlayerCandidate("p", "First", "Team", "League", "Forward", null, null);
      PlayerCandidate second =
          new PlayerCandidate("q", "Second", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(first, second), 2, 0));
      PlayerData firstResult =
          new PlayerData("shared", "First", null, "Team", "Soccer", null, null, null, null);
      PlayerData secondResult =
          new PlayerData("shared", "Second", null, "Team", "Soccer", null, null, null, null);
      when(integration.search("First")).thenReturn(List.of(firstResult));
      when(integration.search("Second")).thenReturn(List.of(secondResult));

      PlayerImageSyncSummary result = synchronizationService.synchronize(false);

      assertThat(result.status()).isEqualTo(PlayerImageSyncRunStatus.PARTIAL);
      assertThat(result.counters().failed()).isEqualTo(1);
      assertThat(result.counters().conflicts()).isEqualTo(1);
      assertThat(result.counters().processed()).isEqualTo(2);
    }

    @Test
    @DisplayName("No cancela una evaluación iniciada si el jugador se inactiva durante la consulta")
    void finishesEvaluationDespiteDeactivation() {
      PlayerCandidate candidate =
          new PlayerCandidate("r", "Name", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      PlayerData response =
          new PlayerData(
              "123",
              "Name",
              null,
              "Team",
              "Soccer",
              null,
              null,
              "https://thesportsdb.com/photo",
              null);
      doAnswer(
              invocation -> {
                catalogService.applySynchronization(new PlayerSnapshot(List.of(), 0, 0));
                return List.of(response);
              })
          .when(integration)
          .search("Name");

      PlayerImageSyncSummary result = synchronizationService.synchronize(false);

      assertThat(result.status()).isEqualTo(PlayerImageSyncRunStatus.COMPLETED);
      assertThat(result.counters().evaluated()).isEqualTo(1);
      assertThat(result.counters().processed()).isEqualTo(1);
      assertThat(result.counters().found()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ante un fallo global conserva la imagen confirmada del primer jugador")
    void globalFailurePreservesConfirmedPlayers() {
      PlayerCandidate first =
          new PlayerCandidate("s", "First", "Team", "League", "Forward", null, null);
      PlayerCandidate second =
          new PlayerCandidate("t", "Second", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(first, second), 2, 0));
      PlayerData photo =
          new PlayerData(
              "123",
              "First",
              null,
              "Team",
              "Soccer",
              null,
              null,
              "https://thesportsdb.com/photo",
              null);
      when(integration.search("First")).thenReturn(List.of(photo));
      when(integration.search("Second")).thenThrow(new RuntimeException("Fallo inesperado"));

      assertThatThrownBy(() -> synchronizationService.synchronize(false))
          .isInstanceOf(PlayerImageSynchronizationException.class);
      assertThat(catalogService.getActivePlayers(0, 20).getContent())
          .extracting(Player::getImageUrl)
          .containsExactly("https://thesportsdb.com/photo", null);
    }

    @Test
    @DisplayName("Un refresco con error transitorio conserva la imagen válida previa")
    void transientFailurePreservesExistingImage() {
      PlayerCandidate candidate =
          new PlayerCandidate("m", "Name", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      PlayerData initial =
          new PlayerData(
              "123",
              "Name",
              null,
              "Team",
              "Soccer",
              null,
              null,
              "https://thesportsdb.com/old",
              null);
      when(integration.search("Name")).thenReturn(List.of(initial));
      synchronizationService.synchronize(false);
      when(integration.lookup("123")).thenThrow(new TheSportsDbUnavailableException());

      PlayerImageSyncSummary result = synchronizationService.synchronize(true);

      assertThat(result.counters().retryableErrors()).isEqualTo(1);
      assertThat(catalogService.getActivePlayers(0, 20).getContent())
          .extracting(Player::getImageUrl)
          .containsExactly("https://thesportsdb.com/old");
      verify(integration, times(4)).lookup("123");
    }

    @Test
    @DisplayName("Una imagen inválida no es error técnico y conserva FOUND anterior")
    void invalidImageDoesNotBecomePermanentError() {
      PlayerCandidate candidate =
          new PlayerCandidate("n", "Name", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      PlayerData initial =
          new PlayerData(
              "123",
              "Name",
              null,
              "Team",
              "Soccer",
              null,
              null,
              "https://thesportsdb.com/old",
              null);
      when(integration.search("Name")).thenReturn(List.of(initial));
      synchronizationService.synchronize(false);
      PlayerData noUsableImage =
          new PlayerData("123", "Name", null, "Team", "Soccer", null, null, null, null);
      when(integration.lookup("123")).thenReturn(List.of(noUsableImage));

      PlayerImageSyncSummary result = synchronizationService.synchronize(true);

      assertThat(result.status()).isEqualTo(PlayerImageSyncRunStatus.COMPLETED);
      assertThat(result.counters().found()).isEqualTo(1);
      verify(integration, times(1)).lookup("123");
    }
  }

  @Nested
  @DisplayName("Cantidad de reintentos configurable")
  @TestPropertySource(properties = "the-sports-db.max-retries=1")
  class ConfiguredRetries {
    @Test
    @DisplayName("Con máximo uno realiza dos solicitudes y deja error temporal")
    void limitsRetries() {
      PlayerCandidate candidate =
          new PlayerCandidate("o", "Name", "Team", "League", "Forward", null, null);
      catalogService.applySynchronization(new PlayerSnapshot(List.of(candidate), 1, 0));
      when(integration.search("Name")).thenThrow(new TheSportsDbUnavailableException());

      PlayerImageSyncSummary result = synchronizationService.synchronize(false);

      assertThat(result.counters().retryableErrors()).isEqualTo(1);
      verify(integration, times(2)).search("Name");
    }
  }
}
