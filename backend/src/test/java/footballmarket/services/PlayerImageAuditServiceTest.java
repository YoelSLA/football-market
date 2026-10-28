package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.records.LeagueCandidate;
import footballmarket.features.catalog.domain.records.PlayerCandidate;
import footballmarket.features.catalog.domain.records.PlayerSnapshot;
import footballmarket.features.catalog.domain.records.TeamCandidate;
import footballmarket.features.images.application.exceptions.PlayerImageRunNotFoundException;
import footballmarket.features.images.domain.enums.PlayerImageResolutionStatus;
import footballmarket.features.images.domain.enums.PlayerImageSyncRunItemResult;
import footballmarket.features.images.domain.enums.PlayerImageSyncRunStatus;
import footballmarket.features.images.domain.records.PlayerImageSyncRunItemRecord;
import footballmarket.features.images.domain.records.PlayerImageSyncSummary;
import footballmarket.integrations.TheSportsDbIntegration;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
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
class PlayerImageAuditServiceTest {
  @Autowired private PlayerCatalogService catalog;
  @Autowired private PlayerImageSynchronizationService synchronization;
  @Autowired private PlayerImageAuditService audit;
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
  @DisplayName("Lectura de auditoría por Service")
  class Audit {
    @Test
    @DisplayName("Devuelve el resumen sin items y conserva la contabilidad del run")
    void readsSummaryWithoutEmbeddingItems() {
      catalog.applySynchronization(photoWithTeam("one", "Name", true));
      when(integration.search("Name")).thenReturn(List.of());
      PlayerImageSyncSummary finished = synchronization.synchronize(false);

      PlayerImageSyncSummary summary = audit.getRun(finished.id());
      Page<PlayerImageSyncRunItemRecord> detail = audit.getItems(finished.id(), 0, 20);

      assertThat(summary.status()).isEqualTo(PlayerImageSyncRunStatus.COMPLETED);
      assertThat(summary.counters().evaluated()).isEqualTo(detail.getTotalElements());
      assertThat(summary.counters().processed()).isEqualTo(1);
      assertThat(detail.getContent())
          .singleElement()
          .satisfies(
              item -> {
                assertThat(item.result()).isEqualTo(PlayerImageSyncRunItemResult.NOT_FOUND);
                assertThat(item.finalState()).isEqualTo(PlayerImageResolutionStatus.NOT_FOUND);
              });
    }

    @Test
    @DisplayName("Run inexistente arroja un error de aplicación")
    void missingRun() {
      assertThatThrownBy(() -> audit.getRun(99999L))
          .isInstanceOf(PlayerImageRunNotFoundException.class);
    }

    @Test
    @DisplayName("Historial e items mantienen metadatos y defaults de página")
    void listsWithPagination() {
      catalog.applySynchronization(photoWithTeam("two", "Name", true));
      when(integration.search("Name")).thenReturn(List.of());
      PlayerImageSyncSummary run = synchronization.synchronize(false);

      Page<PlayerImageSyncSummary> history = audit.getRuns(0, 20);
      Page<PlayerImageSyncRunItemRecord> detail = audit.getItems(run.id(), 0, 20);

      assertThat(history.getNumber()).isZero();
      assertThat(history.getSize()).isEqualTo(20);
      assertThat(history.getContent())
          .extracting(PlayerImageSyncSummary::id)
          .containsExactly(run.id());
      assertThat(detail.getNumber()).isZero();
      assertThat(detail.getSize()).isEqualTo(20);
    }
  }
}
