package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import footballmarket.integrations.TheSportsDbIntegration;
import footballmarket.models.enums.PlayerImageResolutionStatus;
import footballmarket.models.enums.PlayerImageSyncRunItemResult;
import footballmarket.models.enums.PlayerImageSyncRunStatus;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerImageSyncRunItemRecord;
import footballmarket.models.records.PlayerImageSyncSummary;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.services.exceptions.PlayerImageRunNotFoundException;
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
class PlayerImageAuditServiceTest {
  @Autowired private PlayerCatalogService catalog;
  @Autowired private PlayerImageSynchronizationService synchronization;
  @Autowired private PlayerImageAuditService audit;
  @MockitoBean private TheSportsDbIntegration integration;

  @Nested
  @DisplayName("Lectura de auditoría por Service")
  class Audit {
    @Test
    @DisplayName("Devuelve el resumen sin items y conserva la contabilidad del run")
    void readsSummaryWithoutEmbeddingItems() {
      PlayerCandidate player =
          new PlayerCandidate("one", "Name", "Team", "League", "Forward", null, null);
      catalog.applySynchronization(new PlayerSnapshot(List.of(player), 1, 0));
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
      PlayerCandidate player =
          new PlayerCandidate("two", "Name", "Team", "League", "Forward", null, null);
      catalog.applySynchronization(new PlayerSnapshot(List.of(player), 1, 0));
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
