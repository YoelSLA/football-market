package footballmarket.features.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import footballmarket.features.identity.domain.enums.PlayerIdentityRunStatus;
import footballmarket.features.identity.domain.records.PlayerIdentitySyncSummary;
import footballmarket.integrations.whoscored.WhoScoredIntegration;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import footballmarket.integrations.whoscored.exceptions.WhoScoredIntegrationException;
import footballmarket.integrations.whoscored.records.WhoScoredCatalogPage;
import footballmarket.integrations.whoscored.records.WhoScoredPaging;
import footballmarket.integrations.whoscored.records.WhoScoredPlayerIdentity;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, IdentityCatalogueFixture.class})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class PlayerIdentitySynchronizationServiceTest {
  @Autowired private PlayerIdentitySynchronizationService service;
  @Autowired private IdentityCatalogueFixture fixture;
  @MockitoBean private WhoScoredIntegration integration;
  @MockitoSpyBean private WhoScoredProperties properties;
  private final AtomicBoolean transactionDuringNetwork = new AtomicBoolean();

  @BeforeEach
  void arrangeCatalogue() {
    transactionDuringNetwork.set(false);
    List<Long> leagueIds = fixture.prepare();
    List<Integer> tournaments = List.of(2, 3, 4, 5, 22);
    List<WhoScoredProperties.Competition> bindings = new ArrayList<>();
    for (int index = 0; index < 5; index++) {
      bindings.add(
          new WhoScoredProperties.Competition(leagueIds.get(index), tournaments.get(index)));
    }
    doReturn(bindings).when(properties).competitions();
    when(integration.currentPage(anyInt(), eq(1)))
        .thenAnswer(
            invocation -> {
              transactionDuringNetwork.set(
                  transactionDuringNetwork.get()
                      || TransactionSynchronizationManager.isActualTransactionActive());
              int tournament = invocation.getArgument(0);
              int index = tournaments.indexOf(tournament) + 1;
              WhoScoredPlayerIdentity identity =
                  identity(
                      Integer.toString(1000 + index),
                      "Player " + index,
                      "Team " + index,
                      tournament);
              return page(identity);
            });
    when(integration.historicalPage(anyInt(), eq(1)))
        .thenAnswer(
            invocation -> {
              transactionDuringNetwork.set(
                  transactionDuringNetwork.get()
                      || TransactionSynchronizationManager.isActualTransactionActive());
              return emptyPage();
            });
  }

  @Nested
  @DisplayName("Sincronización completa e idempotente")
  class Synchronization {
    @Test
    @DisplayName("Resuelve los cinco jugadores y repetir conserva referencias sin red")
    void resolvesAndRepeatsSafely() {
      // Act
      PlayerIdentitySyncSummary first = service.synchronize();
      // Assert
      assertThat(first)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.COMPLETED, 5, 5, 0, 0));
      assertThat(transactionDuringNetwork.get()).isFalse();
      // Arrange
      clearInvocations(integration);
      // Act
      PlayerIdentitySyncSummary repeated = service.synchronize();
      // Assert
      assertThat(repeated).isEqualTo(first);
      // Verify
      verifyNoInteractions(integration);
    }

    @Test
    @DisplayName("El universo vacío termina sin solicitudes a ningún catálogo")
    void completesEmptyScopeWithoutNetwork() {
      // Arrange
      fixture.movePlayersOutsideScope();
      clearInvocations(integration);
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.COMPLETED, 0, 0, 0, 0));
      // Verify
      verifyNoInteractions(integration);
    }

    @Test
    @DisplayName("Datos locales inválidos quedan sin resolver y no pasan al histórico")
    void separatesLocalDataErrors() {
      // Arrange
      fixture.prepareWithFirstName("---");
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.COMPLETED, 5, 4, 0, 1));
      // Verify
      verify(integration, never()).historicalPage(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Un bloqueo conserva referencias de otras ligas y no crea un NO_MATCH falso")
    void isolatesLeagueFailure() {
      // Arrange
      when(integration.currentPage(4, 1)).thenThrow(blocked());
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.PARTIAL, 5, 4, 0, 1));
      // Verify
      verify(integration).currentPage(5, 1);
      verify(integration).currentPage(22, 1);
      verify(integration, never()).historicalPage(anyInt(), anyInt());
    }

    @Test
    @DisplayName("El fallo de todas las ligas actuales omite el histórico y devuelve FAILED")
    void skipsHistoricalAfterFailure() {
      // Arrange
      when(integration.currentPage(anyInt(), eq(1))).thenThrow(blocked());
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.FAILED, 5, 0, 0, 5));
      // Verify
      verify(integration, never()).historicalPage(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Una página posterior inválida descarta candidatos parciales de esa liga")
    void excludesIncompleteLeagueCandidates() {
      // Arrange
      WhoScoredPlayerIdentity player = identity("1001", "Player 1", "Team 1", 2);
      WhoScoredPaging paging = new WhoScoredPaging(1, 2, 3, 4, 1, 3);
      WhoScoredCatalogPage first =
          new WhoScoredCatalogPage(List.of(player, player, player), List.of(), paging, "first");
      when(integration.currentPage(2, 1)).thenReturn(first);
      when(integration.currentPage(2, 2)).thenThrow(blocked());
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.PARTIAL, 5, 4, 0, 1));
    }

    @Test
    @DisplayName("Una liga sin progreso no publica sus candidatos")
    void rejectsRepeatedPagesWithoutProgress() {
      // Arrange
      WhoScoredPlayerIdentity player = identity("1001", "Player 1", "Team 1", 2);
      WhoScoredPaging firstPaging = new WhoScoredPaging(1, 2, 3, 4, 1, 3);
      WhoScoredPaging lastPaging = new WhoScoredPaging(2, 2, 3, 4, 4, 4);
      WhoScoredCatalogPage first =
          new WhoScoredCatalogPage(List.of(player, player, player), List.of(), firstPaging, "same");
      WhoScoredCatalogPage last =
          new WhoScoredCatalogPage(List.of(player), List.of(), lastPaging, "same");
      when(integration.currentPage(2, 1)).thenReturn(first);
      when(integration.currentPage(2, 2)).thenReturn(last);
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.PARTIAL, 5, 4, 0, 1));
    }

    @Test
    @DisplayName("La interrupción conserva confirmados, restaura el flag y no inicia histórico")
    void preservesInterruption() {
      // Arrange
      when(integration.currentPage(2, 1))
          .thenThrow(new WhoScoredIntegrationException("EXECUTION_INTERRUPTED", null, true));
      try {
        // Act
        PlayerIdentitySyncSummary summary = service.synchronize();
        // Assert
        assertThat(summary)
            .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.FAILED, 5, 0, 0, 5));
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        // Verify
        verify(integration, never()).historicalPage(anyInt(), anyInt());
        verify(integration, never()).currentPage(3, 1);
      } finally {
        Thread.interrupted();
      }
    }
  }

  @Nested
  @DisplayName("Histórico automático y conteo final único")
  class Historical {
    @Test
    @DisplayName("Resuelve un pendiente histórico sin contar dos veces ni rematchear confirmados")
    void resolvesPendingOnce() {
      // Arrange
      when(integration.currentPage(2, 1)).thenReturn(emptyPage());
      WhoScoredPlayerIdentity pending = identity("2001", "Player 1", "Team 1", 2);
      when(integration.historicalPage(2, 1)).thenReturn(page(pending));
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.COMPLETED, 5, 5, 0, 0));
      assertThat(transactionDuringNetwork.get()).isFalse();
      // Verify
      verify(integration).historicalPage(2, 1);
      verify(integration).historicalPage(22, 1);
    }

    @Test
    @DisplayName("Sin coincidencia en ambos catálogos cuenta una sola ausencia final")
    void retainsFinalNoMatch() {
      // Arrange
      when(integration.currentPage(2, 1)).thenReturn(emptyPage());
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.COMPLETED, 5, 4, 1, 0));
    }

    @Test
    @DisplayName("Un histórico FAILED determina el global sin perder referencias del actual")
    void failsWhenHistoricalFails() {
      // Arrange
      when(integration.currentPage(2, 1)).thenReturn(emptyPage());
      when(integration.historicalPage(anyInt(), eq(1))).thenThrow(blocked());
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.FAILED, 5, 4, 0, 1));
    }

    @Test
    @DisplayName("Un histórico parcial determina PARTIAL aunque el actual termine completo")
    void marksPartialHistorical() {
      // Arrange
      when(integration.currentPage(2, 1)).thenReturn(emptyPage());
      when(integration.historicalPage(2, 1)).thenThrow(blocked());
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.PARTIAL, 5, 4, 0, 1));
    }

    @Test
    @DisplayName("Actual parcial conserva PARTIAL tras completar histórico de pendientes genuinos")
    void preservesPartialCurrent() {
      // Arrange
      fixture.prepareWithFirstName("---");
      when(integration.currentPage(3, 1)).thenReturn(emptyPage());
      when(integration.currentPage(4, 1)).thenThrow(blocked());
      WhoScoredPlayerIdentity pending = identity("2002", "Player 2", "Team 2", 3);
      when(integration.historicalPage(3, 1)).thenReturn(page(pending));
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.PARTIAL, 5, 3, 0, 2));
    }

    @Test
    @DisplayName("La ambigüedad global pasa al histórico y su resultado sustituye al provisional")
    void resolvesGlobalAmbiguity() {
      // Arrange
      WhoScoredPlayerIdentity duplicate = identity("7777", "Player 1", "Team 1", 3);
      when(integration.currentPage(3, 1)).thenReturn(page(duplicate));
      WhoScoredPlayerIdentity resolved = identity("2001", "Player 1", "Team 1", 2);
      when(integration.historicalPage(2, 1)).thenReturn(page(resolved));
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.COMPLETED, 5, 4, 1, 0));
    }

    @Test
    @DisplayName("Una ambigüedad histórica queda sin resolver y no como ausencia de coincidencia")
    void preservesHistoricalAmbiguity() {
      // Arrange
      when(integration.currentPage(2, 1)).thenReturn(emptyPage());
      WhoScoredPlayerIdentity first = identity("2001", "Player 1", "Team 1", 2);
      WhoScoredPlayerIdentity second = identity("2002", "Player 1", "Team 1", 2);
      WhoScoredPaging paging = new WhoScoredPaging(1, 1, 3, 2, 1, 2);
      WhoScoredCatalogPage ambiguous =
          new WhoScoredCatalogPage(List.of(first, second), List.of(), paging, "two");
      when(integration.historicalPage(2, 1)).thenReturn(ambiguous);
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.COMPLETED, 5, 4, 0, 1));
    }

    @Test
    @DisplayName("El histórico captura atributos vigentes al comenzar su pase")
    void capturesFreshHistoricalAttributes() {
      // Arrange
      when(integration.currentPage(2, 1)).thenReturn(emptyPage());
      WhoScoredPlayerIdentity last = identity("1005", "Player 5", "Team 5", 22);
      when(integration.currentPage(22, 1))
          .thenAnswer(
              invocation -> {
                fixture.prepareWithFirstName("Renamed Player");
                return page(last);
              });
      WhoScoredPlayerIdentity renamed = identity("2001", "Renamed Player", "Team 1", 2);
      when(integration.historicalPage(2, 1)).thenReturn(page(renamed));
      // Act
      PlayerIdentitySyncSummary summary = service.synchronize();
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.COMPLETED, 5, 5, 0, 0));
    }
  }

  private WhoScoredPlayerIdentity identity(String id, String name, String team, int tournament) {
    return new WhoScoredPlayerIdentity(id, name, null, team, tournament, null, null, null);
  }

  private WhoScoredCatalogPage page(WhoScoredPlayerIdentity identity) {
    WhoScoredPaging paging = new WhoScoredPaging(1, 1, 3, 1, 1, 1);
    return new WhoScoredCatalogPage(List.of(identity), List.of(), paging, identity.externalId());
  }

  private WhoScoredCatalogPage emptyPage() {
    WhoScoredPaging paging = new WhoScoredPaging(1, 0, 3, 0, 0, 0);
    return new WhoScoredCatalogPage(List.of(), List.of(), paging, "empty");
  }

  private WhoScoredIntegrationException blocked() {
    return new WhoScoredIntegrationException("PROVIDER_BLOCKED", 403, false);
  }
}
