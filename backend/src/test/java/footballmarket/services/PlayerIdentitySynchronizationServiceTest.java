package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import footballmarket.features.identity.application.PlayerIdentitySynchronizationService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@org.junit.jupiter.api.Tag("integration")
class PlayerIdentitySynchronizationServiceTest {
  @Autowired private PlayerIdentitySynchronizationService service;
  @Autowired private IdentityCatalogueFixture fixture;
  @MockitoBean private WhoScoredIntegration integration;
  @MockitoSpyBean private WhoScoredProperties properties;
  private final java.util.concurrent.atomic.AtomicBoolean transactionDuringNetwork =
      new java.util.concurrent.atomic.AtomicBoolean();

  @BeforeEach
  void arrangeCatalogue() {
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
              WhoScoredPlayerIdentity player =
                  new WhoScoredPlayerIdentity(
                      Integer.toString(1000 + index),
                      "Player " + index,
                      null,
                      "Team " + index,
                      tournament,
                      null,
                      null,
                      null);
              WhoScoredPaging paging = new WhoScoredPaging(1, 1, 3, 1, 1, 1);
              return new WhoScoredCatalogPage(
                  List.of(player), List.of(), paging, "page-" + tournament);
            });
  }

  @Nested
  @DisplayName("Pase histórico sobre pendientes del origen")
  class Historical {
    @Test
    @DisplayName("Resuelve solo NO_MATCH y AMBIGUOUS y repetir queda vacío sin red")
    void resolvesEligibleAndRepeatsEmpty() {
      // Arrange
      when(integration.currentPage(2, 1)).thenReturn(emptyPage());
      WhoScoredPlayerIdentity duplicate = identity("7777", "Player 2", "Team 2", 4);
      when(integration.currentPage(4, 1)).thenReturn(page(duplicate));
      PlayerIdentitySyncSummary source = service.synchronizeCurrent();
      when(integration.historicalPage(anyInt(), eq(1)))
          .thenAnswer(
              invocation -> {
                transactionDuringNetwork.set(
                    transactionDuringNetwork.get()
                        || TransactionSynchronizationManager.isActualTransactionActive());
                int tournament = invocation.getArgument(0);
                int index = List.of(2, 3, 4, 5, 22).indexOf(tournament) + 1;
                return page(
                    identity(
                        Integer.toString(2000 + index),
                        "Player " + index,
                        "Team " + index,
                        tournament));
              });
      // Act
      PlayerIdentitySyncSummary historical = service.synchronizeHistorical(source.id());
      // Assert
      assertThat(source.counters().ambiguous()).isEqualTo(1);
      assertThat(source.counters().noMatch()).isEqualTo(2);
      assertThat(historical.sourceRunId()).isEqualTo(source.id());
      assertThat(historical.passType().name()).isEqualTo("HISTORICAL");
      assertThat(historical.counters().selectedPlayers()).isEqualTo(3);
      assertThat(historical.counters().resolved()).isEqualTo(3);
      assertThat(historical.counters().pendingPlayers()).isZero();
      assertThat(transactionDuringNetwork.get()).isFalse();
      // Arrange
      clearInvocations(integration);
      // Act
      PlayerIdentitySyncSummary repeated = service.synchronizeHistorical(source.id());
      // Assert
      assertThat(repeated.id()).isNotEqualTo(historical.id());
      assertThat(repeated.status()).isEqualTo(PlayerIdentityRunStatus.COMPLETED);
      assertThat(repeated.counters().selectedPlayers()).isZero();
      assertThat(repeated.leagues())
          .allSatisfy(scope -> assertThat(scope.getStatus().name()).isEqualTo("SKIPPED"));
      // Verify
      verifyNoInteractions(integration);
    }

    @Test
    @DisplayName("Acepta un actual parcial y excluye errores de descubrimiento y datos locales")
    void selectsOnlyGenuinePendingFromPartial() {
      // Arrange
      fixture.prepareWithFirstName("---");
      when(integration.currentPage(3, 1)).thenReturn(emptyPage());
      when(integration.currentPage(4, 1))
          .thenThrow(new WhoScoredIntegrationException("PROVIDER_BLOCKED", 403, false));
      PlayerIdentitySyncSummary source = service.synchronizeCurrent();
      when(integration.historicalPage(anyInt(), eq(1))).thenReturn(emptyPage());
      // Act
      PlayerIdentitySyncSummary historical = service.synchronizeHistorical(source.id());
      // Assert
      assertThat(source.status()).isEqualTo(PlayerIdentityRunStatus.PARTIAL);
      assertThat(historical.counters().selectedPlayers()).isEqualTo(1);
      assertThat(historical.counters().noMatch()).isEqualTo(1);
      assertThat(historical.counters().localDataErrors()).isZero();
      assertThat(historical.counters().discoveryErrors()).isZero();
    }

    @Test
    @DisplayName("Excluye pendientes resueltos por otro actual posterior")
    void excludesReferencesConfirmedAfterSource() {
      // Arrange
      when(integration.currentPage(2, 1)).thenReturn(emptyPage());
      PlayerIdentitySyncSummary source = service.synchronizeCurrent();
      WhoScoredPlayerIdentity player = identity("1001", "Player 1", "Team 1", 2);
      when(integration.currentPage(2, 1)).thenReturn(page(player));
      service.synchronizeCurrent();
      clearInvocations(integration);
      // Act
      PlayerIdentitySyncSummary historical = service.synchronizeHistorical(source.id());
      // Assert
      assertThat(historical.counters().selectedPlayers()).isZero();
      // Verify
      verifyNoInteractions(integration);
    }

    @Test
    @DisplayName(
        "Captura atributos nuevos al iniciar el histórico sin reutilizar el snapshot actual")
    void capturesFreshAttributes() {
      // Arrange
      when(integration.currentPage(2, 1)).thenReturn(emptyPage());
      PlayerIdentitySyncSummary source = service.synchronizeCurrent();
      fixture.prepareWithFirstName("Renamed Player");
      when(integration.historicalPage(anyInt(), eq(1))).thenReturn(emptyPage());
      WhoScoredPlayerIdentity renamed = identity("2001", "Renamed Player", "Team 1", 2);
      when(integration.historicalPage(2, 1)).thenReturn(page(renamed));
      // Act
      PlayerIdentitySyncSummary historical = service.synchronizeHistorical(source.id());
      // Assert
      assertThat(historical.counters().resolved()).isEqualTo(1);
      assertThat(historical.counters().selectedPlayers()).isEqualTo(1);
    }

    @Test
    @DisplayName("Conserva ambigüedad y conflictos y aísla una liga histórica fallida")
    void preservesUnresolvedOutcomesAndFailures() {
      // Arrange
      when(integration.currentPage(anyInt(), eq(1))).thenReturn(emptyPage());
      PlayerIdentitySyncSummary source = service.synchronizeCurrent();
      when(integration.historicalPage(anyInt(), eq(1))).thenReturn(emptyPage());
      WhoScoredPlayerIdentity first = identity("2001", "Player 1", "Team 1", 2);
      WhoScoredPlayerIdentity second = identity("2002", "Player 1", "Team 1", 2);
      WhoScoredPaging paging = new WhoScoredPaging(1, 1, 3, 2, 1, 2);
      when(integration.historicalPage(2, 1))
          .thenReturn(new WhoScoredCatalogPage(List.of(first, second), List.of(), paging, "two"));
      when(integration.historicalPage(3, 1))
          .thenThrow(new WhoScoredIntegrationException("PROVIDER_BLOCKED", 403, false));
      // Act
      PlayerIdentitySyncSummary historical = service.synchronizeHistorical(source.id());
      // Assert
      assertThat(historical.status()).isEqualTo(PlayerIdentityRunStatus.PARTIAL);
      assertThat(historical.counters().ambiguous()).isEqualTo(1);
      assertThat(historical.counters().discoveryErrors()).isEqualTo(1);
      assertThat(historical.counters().noMatch()).isEqualTo(3);
      assertThat(historical.counters().resolved()).isZero();
    }

    @Test
    @DisplayName("Rechaza origen ausente o histórico antes de consultar el proveedor")
    void rejectsInvalidSourceWithoutNetwork() {
      // Arrange
      PlayerIdentitySyncSummary source = service.synchronizeCurrent();
      PlayerIdentitySyncSummary historical = service.synchronizeHistorical(source.id());
      clearInvocations(integration);
      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeHistorical(Long.MAX_VALUE))
          .isInstanceOf(
              footballmarket.features.identity.application.exceptions
                  .PlayerIdentityRunNotFoundException.class);
      assertThatThrownBy(() -> service.synchronizeHistorical(historical.id()))
          .isInstanceOf(
              footballmarket.features.identity.application.exceptions
                  .PlayerIdentitySourceNotEligibleException.class);
      // Verify
      verifyNoInteractions(integration);
    }
  }

  private WhoScoredPlayerIdentity identity(String id, String name, String team, int tournament) {
    return new WhoScoredPlayerIdentity(id, name, null, team, tournament, null, null, null);
  }

  private WhoScoredCatalogPage page(WhoScoredPlayerIdentity player) {
    WhoScoredPaging paging = new WhoScoredPaging(1, 1, 3, 1, 1, 1);
    return new WhoScoredCatalogPage(List.of(player), List.of(), paging, player.externalId());
  }

  private WhoScoredCatalogPage emptyPage() {
    WhoScoredPaging paging = new WhoScoredPaging(1, 0, 3, 0, 0, 0);
    return new WhoScoredCatalogPage(List.of(), List.of(), paging, "empty");
  }

  @Nested
  @DisplayName("Pase actual durable e idempotente")
  class Current {
    @Test
    @DisplayName("Una liga sin progreso no publica candidatos y conserva el diagnóstico propio")
    void rejectsRepeatedPagesWithoutProgress() {
      // Arrange
      WhoScoredPlayerIdentity player =
          new WhoScoredPlayerIdentity("1001", "Player 1", null, "Team 1", 2, null, null, null);
      WhoScoredPaging firstPaging = new WhoScoredPaging(1, 2, 3, 4, 1, 3);
      WhoScoredPaging finalPaging = new WhoScoredPaging(2, 2, 3, 4, 4, 4);
      WhoScoredCatalogPage first =
          new WhoScoredCatalogPage(
              List.of(player, player, player), List.of(), firstPaging, "repeated");
      WhoScoredCatalogPage last =
          new WhoScoredCatalogPage(List.of(player), List.of(), finalPaging, "repeated");
      when(integration.currentPage(2, 1)).thenReturn(first);
      when(integration.currentPage(2, 2)).thenReturn(last);
      // Act
      PlayerIdentitySyncSummary summary = service.synchronizeCurrent();
      // Assert
      assertThat(summary.counters().discoveredUnique()).isEqualTo(4);
      assertThat(summary.leagues())
          .filteredOn(league -> league.getTournamentId() == 2)
          .singleElement()
          .satisfies(league -> assertThat(league.getErrorCode()).isEqualTo("PROVIDER_NO_PROGRESS"));
    }

    @Test
    @DisplayName(
        "Sin jugadores en las cinco ligas termina vacío sin red aunque existan jugadores fuera del ámbito")
    void completesEmptyScopeWithoutNetwork() {
      // Arrange
      fixture.movePlayersOutsideScope();
      clearInvocations(integration);
      // Act
      PlayerIdentitySyncSummary summary = service.synchronizeCurrent();
      // Assert
      assertThat(summary.status()).isEqualTo(PlayerIdentityRunStatus.COMPLETED);
      assertThat(summary.counters().selectedPlayers()).isZero();
      assertThat(summary.counters().pendingPlayers()).isZero();
      assertThat(summary.leagues())
          .allSatisfy(league -> assertThat(league.getStatus().name()).isEqualTo("SKIPPED"));
      // Verify
      verifyNoInteractions(integration);
    }

    @Test
    @DisplayName("Datos locales de solo signos generan error propio sin degradar la finalización")
    void separatesLocalDataErrors() {
      // Arrange
      fixture.prepareWithFirstName("---");
      // Act
      PlayerIdentitySyncSummary summary = service.synchronizeCurrent();
      // Assert
      assertThat(summary.status()).isEqualTo(PlayerIdentityRunStatus.COMPLETED);
      assertThat(summary.counters().localDataErrors()).isEqualTo(1);
      assertThat(summary.counters().resolved()).isEqualTo(4);
      assertThat(summary.counters().noMatch()).isZero();
    }

    @Test
    @DisplayName("Considera candidatos de otra liga y conserva la ambigüedad global")
    void evaluatesCandidatesAcrossLeagues() {
      // Arrange
      WhoScoredPlayerIdentity secondIdentity =
          new WhoScoredPlayerIdentity("7777", "Player 1", null, "Team 1", 3, null, null, null);
      WhoScoredPaging paging = new WhoScoredPaging(1, 1, 3, 1, 1, 1);
      WhoScoredCatalogPage page =
          new WhoScoredCatalogPage(List.of(secondIdentity), List.of(), paging, "global-second");
      when(integration.currentPage(3, 1)).thenReturn(page);
      // Act
      PlayerIdentitySyncSummary summary = service.synchronizeCurrent();
      // Assert
      assertThat(summary.status()).isEqualTo(PlayerIdentityRunStatus.COMPLETED);
      assertThat(summary.counters().ambiguous()).isEqualTo(1);
      assertThat(summary.counters().noMatch()).isEqualTo(1);
      assertThat(summary.counters().resolved()).isEqualTo(3);
    }

    @Test
    @DisplayName("Una página posterior inválida elimina todos los candidatos parciales de esa liga")
    void excludesIncompleteLeagueCandidates() {
      // Arrange
      WhoScoredPlayerIdentity player =
          new WhoScoredPlayerIdentity("1001", "Player 1", null, "Team 1", 2, null, null, null);
      WhoScoredPaging paging = new WhoScoredPaging(1, 2, 3, 4, 1, 3);
      WhoScoredCatalogPage page =
          new WhoScoredCatalogPage(
              List.of(player, player, player), List.of(), paging, "partial-first");
      when(integration.currentPage(2, 1)).thenReturn(page);
      when(integration.currentPage(2, 2))
          .thenThrow(new WhoScoredIntegrationException("PROVIDER_INVALID_RESPONSE", 200, false));
      // Act
      PlayerIdentitySyncSummary summary = service.synchronizeCurrent();
      // Assert
      assertThat(summary.counters().discoveredUnique()).isEqualTo(4);
      assertThat(summary.counters().resolved()).isEqualTo(4);
      assertThat(summary.counters().discoveryErrors()).isEqualTo(1);
      assertThat(summary.status()).isEqualTo(PlayerIdentityRunStatus.PARTIAL);
    }

    @Test
    @DisplayName("Resuelve coincidencias únicas y repetir crea otro run sin duplicar referencias")
    void resolvesAndRepeatsSafely() {
      // Act
      PlayerIdentitySyncSummary first = service.synchronizeCurrent();
      PlayerIdentitySyncSummary repeated = service.synchronizeCurrent();
      // Assert
      assertThat(first.status()).isEqualTo(PlayerIdentityRunStatus.COMPLETED);
      assertThat(first.counters().resolved()).isEqualTo(5);
      assertThat(first.counters().pendingPlayers()).isZero();
      assertThat(transactionDuringNetwork.get()).isFalse();
      assertThat(repeated.id()).isNotEqualTo(first.id());
      assertThat(repeated.counters().resolved()).isZero();
      assertThat(repeated.counters().alreadyReferenced()).isEqualTo(5);
    }

    @Test
    @DisplayName("Un bloqueo de una liga conserva las referencias de las demás")
    void isolatesLeagueFailure() {
      // Arrange
      when(integration.currentPage(4, 1))
          .thenThrow(new WhoScoredIntegrationException("PROVIDER_BLOCKED", 403, false));
      // Act
      PlayerIdentitySyncSummary summary = service.synchronizeCurrent();
      // Assert
      assertThat(summary.status()).isEqualTo(PlayerIdentityRunStatus.PARTIAL);
      assertThat(summary.counters().resolved()).isEqualTo(4);
      assertThat(summary.counters().discoveryErrors()).isEqualTo(1);
      assertThat(summary.counters().providerErrors()).isEqualTo(1);
      assertThat(summary.counters().noMatch()).isZero();
      assertThat(summary.counters().pendingPlayers()).isZero();
      // Verify
      verify(integration).currentPage(eq(5), eq(1));
      verify(integration).currentPage(eq(22), eq(1));
    }
  }
}
