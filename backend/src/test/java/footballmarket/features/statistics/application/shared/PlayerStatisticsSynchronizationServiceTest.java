package footballmarket.features.statistics.application.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import footballmarket.features.identity.application.PlayerIdentitySynchronizationService;
import footballmarket.features.statistics.application.PlayerStatisticsSynchronizationService;
import footballmarket.features.statistics.application.exceptions.ScheduledStatisticsSyncException;
import footballmarket.features.statistics.application.exceptions.ScheduledStatisticsSyncException.Reason;
import footballmarket.features.statistics.domain.enums.ExtractionRunStatus;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncRequest;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncSummary;
import footballmarket.features.statistics.infraestructure.config.PlayerStatisticsProperties;
import footballmarket.integrations.whoscored.WhoScoredIntegration;
import footballmarket.integrations.whoscored.WhoScoredRequestPacer;
import footballmarket.integrations.whoscored.WhoScoredStatisticsIntegration;
import footballmarket.integrations.whoscored.WhoScoredStatisticsScrapingIntegration;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import footballmarket.integrations.whoscored.exceptions.HeadedBrowserUnavailableException;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedContextRow;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedMetricCell;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedPlayer;
import footballmarket.integrations.whoscored.records.WhoScoredStatisticsResponse;
import footballmarket.integrations.whoscored.records.WhoScoredStatisticsTable;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.StatisticsCloseFailureInjector;
import footballmarket.support.TestcontainersConfiguration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
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

/** Lote de scraping, reanudación, recuperación de ejecución activa y fallo por jugador. */
@SpringBootTest
@ActiveProfiles("test")
@Import({
  TestcontainersConfiguration.class,
  IdentityCatalogueFixture.class,
  footballmarket.support.StatisticsRunTestData.class,
  StatisticsCloseFailureInjector.class
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class PlayerStatisticsSynchronizationServiceTest {
  @Autowired private PlayerStatisticsSynchronizationService service;
  @Autowired private PlayerIdentitySynchronizationService identities;
  @Autowired private IdentityCatalogueFixture fixture;
  @Autowired private footballmarket.support.StatisticsRunTestData runData;
  @Autowired private StatisticsCloseFailureInjector closeFailure;
  @MockitoBean private WhoScoredStatisticsScrapingIntegration integration;
  @MockitoBean private WhoScoredIntegration identityIntegration;
  @MockitoBean private WhoScoredStatisticsIntegration jsonIntegration;
  @MockitoSpyBean private WhoScoredProperties properties;
  @MockitoSpyBean private PlayerStatisticsProperties statistics;
  private WhoScoredStatisticsScrapingIntegration.Session session;

  @AfterEach
  void clearFailureAndInterruption() {
    closeFailure.reset();
    Thread.interrupted();
  }

  @BeforeEach
  void arrangeConfirmedPlayers() {
    List<Long> leagueIds = fixture.prepare();
    List<Integer> tournaments = List.of(2, 3, 4, 5, 22);
    List<WhoScoredProperties.Competition> bindings = new ArrayList<>();
    for (int index = 0; index < 5; index++) {
      bindings.add(
          new WhoScoredProperties.Competition(leagueIds.get(index), tournaments.get(index)));
    }
    doReturn(bindings).when(properties).competitions();
    when(identityIntegration.currentPage(anyInt(), eq(1)))
        .thenAnswer(
            invocation -> {
              int tournament = invocation.getArgument(0);
              int index = tournaments.indexOf(tournament) + 1;
              return new footballmarket.integrations.whoscored.records.WhoScoredCatalogPage(
                  List.of(
                      new footballmarket.integrations.whoscored.records.WhoScoredPlayerIdentity(
                          Integer.toString(1000 + index),
                          "Player " + index,
                          null,
                          "Team " + index,
                          tournament,
                          null,
                          null,
                          null)),
                  List.of(),
                  new footballmarket.integrations.whoscored.records.WhoScoredPaging(
                      1, 1, 1, 1, 1, 1),
                  "page-" + tournament);
            });
    identities.synchronize();
    doReturn(1).when(statistics).jsonWorkers();
    when(jsonIntegration.newWorker())
        .thenReturn(mock(WhoScoredRequestPacer.StatisticsWorker.class));
    when(jsonIntegration.fetch(any(), any()))
        .thenAnswer(invocation -> jsonResponse(invocation.getArgument(0)));
    session = mock(WhoScoredStatisticsScrapingIntegration.Session.class);
    when(integration.openSession()).thenReturn(session);
    when(integration.scrape(any(), any(), any(), any()))
        .thenAnswer(invocation -> scraped(invocation.getArgument(3)));
  }

  private WhoScoredScrapedPlayer scraped(String playerId) {
    WhoScoredScrapedContextRow row =
        new WhoScoredScrapedContextRow(
            "initial",
            tournamentOf(playerId),
            "200",
            "11",
            "2025/2026",
            "2024/2025",
            List.of(
                new WhoScoredScrapedMetricCell("apps", "6"),
                new WhoScoredScrapedMetricCell("rating", "6.9033333333333333"),
                new WhoScoredScrapedMetricCell("goal", "-")),
            java.time.Instant.parse("2026-10-15T12:00:00Z"));
    return new WhoScoredScrapedPlayer(
        playerId,
        List.of(row),
        new footballmarket.integrations.whoscored.records.WhoScoredStructuredDataBlock(List.of()),
        18,
        List.of());
  }

  private PlayerStatisticsSyncSummary run() {
    return service.synchronizeScraping(
        new PlayerStatisticsSyncRequest(LocalDate.now(java.time.ZoneOffset.UTC)));
  }

  private WhoScoredStatisticsResponse jsonResponse(String playerId) {
    Instant observedAt = Instant.parse("2026-10-15T12:00:00Z");
    WhoScoredStatisticsTable table =
        new WhoScoredStatisticsTable(
            "summary/all",
            tournamentOf(playerId),
            "200",
            "2026",
            "2026/27",
            null,
            List.of(
                new WhoScoredStatisticsTable.Cell("apps", "6"),
                new WhoScoredStatisticsTable.Cell("rating", "6.9")),
            observedAt);
    return new WhoScoredStatisticsResponse(playerId, List.of(table), observedAt);
  }

  @Nested
  @DisplayName("Fachada de sincronización programada sin cambios al contrato manual")
  class ScheduledSynchronization {
    @Test
    @DisplayName("El doble fallo scraping automático conserva el error original y el de cierre")
    void scrapingAutomaticoComunicaDobleFallo() {
      // Arrange
      HeadedBrowserUnavailableException primary =
          new HeadedBrowserUnavailableException("detalle privado del navegador");
      when(integration.openSession()).thenThrow(primary);
      closeFailure.failNextClose();
      PlayerStatisticsSyncRequest request = new PlayerStatisticsSyncRequest(null);

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeScrapingScheduled(request))
          .isInstanceOfSatisfying(
              ScheduledStatisticsSyncException.class,
              failure -> {
                assertThat(failure.getReason()).isEqualTo(Reason.HEADED_BROWSER_UNAVAILABLE);
                assertThat(failure.getCause()).isSameAs(primary);
                assertThat(failure.getSuppressed()).hasSize(1);
                assertThat(failure.getMessage()).doesNotContain("detalle privado");
              });
    }

    @Test
    @DisplayName(
        "El doble fallo scraping manual conserva su excepción original con cierre suprimido")
    void scrapingManualConservaContratoAnteDobleFallo() {
      // Arrange
      HeadedBrowserUnavailableException primary = new HeadedBrowserUnavailableException();
      when(integration.openSession()).thenThrow(primary);
      closeFailure.failNextClose();
      PlayerStatisticsSyncRequest request = new PlayerStatisticsSyncRequest(null);

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeScraping(request))
          .isSameAs(primary)
          .isInstanceOfSatisfying(
              HeadedBrowserUnavailableException.class,
              failure -> assertThat(failure.getSuppressed()).hasSize(1));
    }

    @Test
    @DisplayName("Un fallo de persistencia al cerrar no se comunica como campaña completada")
    void falloDeCierreSinErrorPrevio() {
      // Arrange
      closeFailure.failNextClose();

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeJsonScheduled())
          .isInstanceOfSatisfying(
              ScheduledStatisticsSyncException.class,
              failure -> {
                assertThat(failure.getReason()).isEqualTo(Reason.CLOSE_FAILED);
                assertThat(failure.getCause()).isNotNull();
                assertThat(failure.getSuppressed()).isEmpty();
              });

      // Verify
      assertThatThrownBy(() -> service.synchronizeJson())
          .isInstanceOf(
              footballmarket.features.statistics.application.exceptions
                  .PlayerStatisticsSyncInProgressException.class);
    }

    @Test
    @DisplayName("Un fallo inesperado JSON cierra la campaña y conserva las confirmaciones previas")
    void falloInesperadoPermiteReanudarManual() {
      // Arrange
      IllegalStateException primary = new IllegalStateException("fallo privado del proveedor");
      doAnswer(
              invocation -> {
                String playerId = invocation.getArgument(0);
                if ("1002".equals(playerId)) {
                  throw primary;
                }
                return jsonResponse(playerId);
              })
          .when(jsonIntegration)
          .fetch(any(), any());

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeJsonScheduled())
          .isInstanceOfSatisfying(
              ScheduledStatisticsSyncException.class,
              failure -> {
                assertThat(failure.getReason()).isEqualTo(Reason.EXECUTION_FAILED);
                assertThat(failure.getCause()).isSameAs(primary);
                assertThat(failure.getSuppressed()).isEmpty();
              });

      // Verify: la entrada manual vuelve a trabajar solo sobre los cuatro pendientes.
      doAnswer(invocation -> jsonResponse(invocation.getArgument(0)))
          .when(jsonIntegration)
          .fetch(any(), any());
      PlayerStatisticsSyncSummary resumed = service.synchronizeJson();
      assertThat(resumed.counters().selected()).isEqualTo(4);
      assertThat(resumed.counters().completed()).isEqualTo(4);
      assertThat(resumed.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName(
        "La interrupción del coordinador se conserva y el cierre permite relanzar manualmente")
    void interrupcionCoordinadorCierraCampana() {
      // Arrange
      Thread coordinator = Thread.currentThread();
      AtomicBoolean interruptOnce = new AtomicBoolean(true);
      doAnswer(
              invocation -> {
                if (interruptOnce.getAndSet(false)) {
                  coordinator.interrupt();
                  throw new IllegalStateException("interrupción controlada");
                }
                return jsonResponse(invocation.getArgument(0));
              })
          .when(jsonIntegration)
          .fetch(any(), any());

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeJsonScheduled())
          .isInstanceOfSatisfying(
              ScheduledStatisticsSyncException.class,
              failure -> assertThat(failure.getReason()).isEqualTo(Reason.EXECUTION_INTERRUPTED));
      assertThat(Thread.currentThread().isInterrupted()).isTrue();

      // Verify: limpiar únicamente el indicador del test para poder ejercer el relanzamiento.
      Thread.interrupted();
      doAnswer(invocation -> jsonResponse(invocation.getArgument(0)))
          .when(jsonIntegration)
          .fetch(any(), any());
      PlayerStatisticsSyncSummary resumed = service.synchronizeJson();
      assertThat(resumed.status()).isEqualTo(ExtractionRunStatus.COMPLETED);
      assertThat(resumed.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName("Si también falla el cierre conserva el error principal y comunica ambos fallos")
    void falloDeCierreNoOcultaErrorPrincipal() {
      // Arrange
      IllegalStateException primary = new IllegalStateException("fallo privado original");
      doThrow(primary).when(jsonIntegration).fetch(any(), any());
      closeFailure.failNextClose();

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeJsonScheduled())
          .isInstanceOfSatisfying(
              ScheduledStatisticsSyncException.class,
              failure -> {
                assertThat(failure.getReason()).isEqualTo(Reason.EXECUTION_FAILED);
                assertThat(failure.getCause()).isSameAs(primary);
                assertThat(failure.getSuppressed()).hasSize(1);
              });

      // Verify: no se promete disponibilidad inmediata cuando no se pudo confirmar el cierre.
      assertThatThrownBy(() -> service.synchronizeJson())
          .isInstanceOf(
              footballmarket.features.statistics.application.exceptions
                  .PlayerStatisticsSyncInProgressException.class);
    }

    @Test
    @DisplayName(
        "Tras fallo automático scraping la entrada manual conserva sus errores y se recupera")
    void scrapingManualDisponibleTrasFalloAutomatico() {
      // Arrange
      when(integration.openSession()).thenThrow(new HeadedBrowserUnavailableException());
      PlayerStatisticsSyncRequest request = new PlayerStatisticsSyncRequest(null);

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeScrapingScheduled(request))
          .isInstanceOf(HeadedBrowserUnavailableException.class);

      // Verify
      assertThatThrownBy(() -> service.synchronizeScraping(request))
          .isInstanceOf(HeadedBrowserUnavailableException.class);
      doReturn(session).when(integration).openSession();
      PlayerStatisticsSyncSummary manual = service.synchronizeScraping(request);
      assertThat(manual.status()).isEqualTo(ExtractionRunStatus.COMPLETED);
      assertThat(manual.counters().completed()).isEqualTo(5);
      assertThat(manual.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName("Sin confirmaciones JSON el fallo global conserva su causa de proveedor")
    void jsonInterrumpidoPorProveedor() {
      // Arrange
      doThrow(new WhoScoredStatisticsException("PROVIDER_TRANSPORT_ERROR", 429))
          .when(jsonIntegration)
          .fetch(any(), any());

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeJsonScheduled())
          .isInstanceOfSatisfying(
              ScheduledStatisticsSyncException.class,
              failure -> assertThat(failure.getReason()).isEqualTo(Reason.PROVIDER_UNAVAILABLE));
    }

    @Test
    @DisplayName("JSON sin confirmaciones por fallos individuales comunica FAILED")
    void jsonFallidoComunicaEstado() {
      // Arrange
      doThrow(new WhoScoredStatisticsException("PROVIDER_TIMEOUT", null))
          .when(jsonIntegration)
          .fetch(any(), any());

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeJsonScheduled())
          .isInstanceOfSatisfying(
              ScheduledStatisticsSyncException.class,
              failure -> assertThat(failure.getReason()).isEqualTo(Reason.FAILED));
    }

    @Test
    @DisplayName("Scraping completo conserva confirmaciones al repetir la entrada manual")
    void scrapingCompletoConservaEntradaManual() {
      // Arrange
      PlayerStatisticsSyncRequest request = new PlayerStatisticsSyncRequest(null);

      // Act
      boolean partial = service.synchronizeScrapingScheduled(request);

      // Assert
      assertThat(partial).isFalse();
      // Verify
      PlayerStatisticsSyncSummary manual = run();
      assertThat(manual.counters().selected()).isZero();
      assertThat(manual.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName("JSON completo devuelve ausencia de parcialidad y conserva la entrada manual")
    void jsonCompletoConservaEntradaManual() {
      // Act
      boolean partial = service.synchronizeJsonScheduled();

      // Assert
      assertThat(partial).isFalse();
      // Verify
      PlayerStatisticsSyncSummary manual = service.synchronizeJson();
      assertThat(manual.status()).isEqualTo(ExtractionRunStatus.COMPLETED);
      assertThat(manual.counters().selected()).isZero();
      assertThat(manual.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName("Un corte global JSON se comunica aunque existan confirmaciones parciales")
    void jsonParcialNoOcultaCorteGlobal() {
      // Arrange
      doAnswer(
              invocation -> {
                String playerId = invocation.getArgument(0);
                if ("1002".equals(playerId)) {
                  throw new WhoScoredStatisticsException("PROVIDER_BLOCKED", 403);
                }
                return jsonResponse(playerId);
              })
          .when(jsonIntegration)
          .fetch(any(), any());

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeJsonScheduled())
          .isInstanceOfSatisfying(
              ScheduledStatisticsSyncException.class,
              failure -> assertThat(failure.getReason()).isEqualTo(Reason.PROVIDER_UNAVAILABLE));

      // Verify: las confirmaciones anteriores siguen disponibles en la misma campaña.
      doAnswer(invocation -> jsonResponse(invocation.getArgument(0)))
          .when(jsonIntegration)
          .fetch(any(), any());
      PlayerStatisticsSyncSummary resumed = service.synchronizeJson();
      assertThat(resumed.counters().selected()).isEqualTo(4);
      assertThat(resumed.status()).isEqualTo(ExtractionRunStatus.COMPLETED);
      assertThat(resumed.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName("La entrada manual JSON conserva su resumen parcial ante el corte global")
    void jsonManualConservaResumenParcial() {
      // Arrange
      doAnswer(
              invocation -> {
                String playerId = invocation.getArgument(0);
                if ("1002".equals(playerId)) {
                  throw new WhoScoredStatisticsException("PROVIDER_BLOCKED", 403);
                }
                return jsonResponse(playerId);
              })
          .when(jsonIntegration)
          .fetch(any(), any());

      // Act
      PlayerStatisticsSyncSummary manual = service.synchronizeJson();

      // Assert
      assertThat(manual.status()).isEqualTo(ExtractionRunStatus.PARTIAL);
      assertThat(manual.counters().completed()).isEqualTo(1);
      assertThat(manual.counters().failed()).isEqualTo(1);
      assertThat(manual.counters().pending()).isEqualTo(4);
    }

    @Test
    @DisplayName("Un fallo individual JSON permite continuar y devuelve parcialidad")
    void jsonParcialIndividualContinua() {
      // Arrange
      doAnswer(
              invocation -> {
                String playerId = invocation.getArgument(0);
                if ("1002".equals(playerId)) {
                  throw new WhoScoredStatisticsException("PROVIDER_TIMEOUT", null);
                }
                return jsonResponse(playerId);
              })
          .when(jsonIntegration)
          .fetch(any(), any());

      // Act
      boolean partial = service.synchronizeJsonScheduled();

      // Assert
      assertThat(partial).isTrue();
      // Verify
      doAnswer(invocation -> jsonResponse(invocation.getArgument(0)))
          .when(jsonIntegration)
          .fetch(any(), any());
      PlayerStatisticsSyncSummary resumed = service.synchronizeJson();
      assertThat(resumed.counters().selected()).isEqualTo(1);
      assertThat(resumed.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName("Un resultado FAILED se traduce sin convertirlo en una interrupción")
    void scrapingFallidoComunicaEstado() {
      // Arrange
      doThrow(new WhoScoredStatisticsException("PROVIDER_BLOCKED", 403))
          .when(integration)
          .scrape(any(), any(), any(), any());
      PlayerStatisticsSyncRequest request = new PlayerStatisticsSyncRequest(null);

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeScrapingScheduled(request))
          .isInstanceOfSatisfying(
              ScheduledStatisticsSyncException.class,
              failure -> assertThat(failure.getReason()).isEqualTo(Reason.FAILED));
    }

    @Test
    @DisplayName("La interrupción global de scraping se comunica mediante un motivo seguro")
    void scrapingInterrumpidoComunicaCausa() {
      // Arrange
      doThrow(new WhoScoredStatisticsException("EXTRACTION_INTERRUPTED", null))
          .when(integration)
          .scrape(any(), any(), any(), any());
      PlayerStatisticsSyncRequest request = new PlayerStatisticsSyncRequest(null);

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeScrapingScheduled(request))
          .isInstanceOfSatisfying(
              ScheduledStatisticsSyncException.class,
              failure -> assertThat(failure.getReason()).isEqualTo(Reason.EXECUTION_INTERRUPTED));
    }

    @Test
    @DisplayName("Una interrupción JSON no se oculta tras confirmaciones anteriores")
    void jsonInterrumpidoNoContinua() {
      // Arrange
      doAnswer(
              invocation -> {
                String playerId = invocation.getArgument(0);
                if ("1002".equals(playerId)) {
                  throw new WhoScoredStatisticsException("EXTRACTION_INTERRUPTED", null);
                }
                return jsonResponse(playerId);
              })
          .when(jsonIntegration)
          .fetch(any(), any());

      // Act / Assert
      assertThatThrownBy(() -> service.synchronizeJsonScheduled())
          .isInstanceOfSatisfying(
              ScheduledStatisticsSyncException.class,
              failure -> assertThat(failure.getReason()).isEqualTo(Reason.EXECUTION_INTERRUPTED));
    }

    @Test
    @DisplayName("Un parcial individual de scraping no se convierte en fallo global")
    void scrapingParcialContinua() {
      // Arrange
      doAnswer(
              invocation -> {
                String playerId = invocation.getArgument(3);
                if ("1002".equals(playerId)) {
                  throw new WhoScoredStatisticsException("PROVIDER_BLOCKED", 403);
                }
                return scraped(playerId);
              })
          .when(integration)
          .scrape(any(), any(), any(), any());
      PlayerStatisticsSyncRequest request = new PlayerStatisticsSyncRequest(null);

      // Act
      boolean partial = service.synchronizeScrapingScheduled(request);

      // Assert
      assertThat(partial).isTrue();
    }
  }

  @Nested
  @DisplayName("Lote de scraping sobre la población elegible")
  class Batch {
    @Test
    @DisplayName(
        "Recuperar un run huérfano conserva confirmaciones y no abre navegador sin pendientes")
    void orphanRecoveryPreservesCompletedPlayers() {
      // Arrange
      PlayerStatisticsSyncSummary completed = run();
      runData.orphanScrapingRun(completed.extractionDate());
      when(integration.openSession()).thenThrow(new HeadedBrowserUnavailableException());
      // Act
      PlayerStatisticsSyncSummary summary = run();
      // Assert
      assertThat(summary.counters().selected()).isZero();
      assertThat(summary.providerCalls()).isZero();
      assertThat(summary.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName(
        "Sin valores numéricos el contexto resuelto queda pendiente y admite reconfirmación")
    void noDataConContextoResuelto() {
      // Arrange
      doAnswer(
              invocation ->
                  footballmarket.support.StatisticsScrapingSamples.player(
                      invocation.getArgument(3),
                      List.of(new WhoScoredScrapedMetricCell("goal", "-"))))
          .when(integration)
          .scrape(any(), any(), any(), any());
      // Act
      PlayerStatisticsSyncSummary summary = run();

      // Assert
      assertThat(summary.status()).isEqualTo(ExtractionRunStatus.COMPLETED);
      assertThat(summary.counters().eligible()).isEqualTo(5);
      assertThat(summary.counters().excludedWithoutReference()).isZero();
      assertThat(summary.counters().selected()).isEqualTo(5);
      assertThat(summary.counters().completed()).isZero();
      assertThat(summary.counters().withoutData()).isEqualTo(5);
      assertThat(summary.counters().pending()).isEqualTo(5);
      assertThat(summary.coverageComplete()).isFalse();
      // Verify: nueva petición reconfirma las mismas filas nulas sin colisión.
      doAnswer(invocation -> scraped(invocation.getArgument(3)))
          .when(integration)
          .scrape(any(), any(), any(), any());
      PlayerStatisticsSyncSummary reconfirmed = run();
      assertThat(reconfirmed.counters().completed()).isEqualTo(5);
      assertThat(reconfirmed.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName("Sin límite explícito el lote recorre toda la población elegible del día")
    void recorrePoblacionCompleta() {
      // Act
      PlayerStatisticsSyncSummary summary = run();

      // Assert
      assertThat(summary.counters().selected()).isEqualTo(5);
      assertThat(summary.counters().completed()).isEqualTo(5);
      assertThat(summary.counters().pending()).isZero();
      assertThat(summary.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName("Un fallo puntual deja al jugador como fallido y no invalida los demás")
    void falloPuntualNoInvalidaElResto() {
      // Arrange
      // doThrow: con when() el doble se invoca con null y se ejecuta la respuesta del arrange.
      doThrow(new WhoScoredStatisticsException("PROVIDER_BLOCKED", 403))
          .when(integration)
          .scrape(any(), any(), any(), any());

      // Act
      PlayerStatisticsSyncSummary summary = run();

      // Assert
      assertThat(summary.status()).isEqualTo(ExtractionRunStatus.FAILED);
      assertThat(summary.counters().selected()).isEqualTo(5);
      assertThat(summary.counters().failed()).isEqualTo(5);
      assertThat(summary.coverageComplete()).isFalse();
    }

    @Test
    @DisplayName("Un fallo y una confirmación en el mismo lote terminan en estado parcial")
    void estadoParcialConFallosYConfirmaciones() {
      // Arrange
      doAnswer(
              invocation ->
                  "1001".equals(invocation.getArgument(3, String.class))
                      ? scraped("1001")
                      : throwBlocked())
          .when(integration)
          .scrape(any(), any(), any(), any());

      // Act
      PlayerStatisticsSyncSummary summary = run();

      // Assert
      assertThat(summary.status()).isEqualTo(ExtractionRunStatus.PARTIAL);
      assertThat(summary.counters().completed()).isGreaterThanOrEqualTo(1);
      assertThat(summary.counters().failed()).isGreaterThanOrEqualTo(1);
      assertThat(summary.counters().pending()).isGreaterThan(0);
    }

    private WhoScoredScrapedPlayer throwBlocked() {
      throw new WhoScoredStatisticsException("PROVIDER_BLOCKED", 403);
    }

    @Test
    @DisplayName("Un relanzamiento no duplica lo confirmado y reanuda los pendientes")
    void reanudaSinDuplicar() {
      // Arrange
      doAnswer(
              invocation ->
                  "1001".equals(invocation.getArgument(3, String.class))
                      ? throwBlocked()
                      : scraped(invocation.getArgument(3)))
          .when(integration)
          .scrape(any(), any(), any(), any());
      PlayerStatisticsSyncSummary first = run();
      doAnswer(invocation -> scraped(invocation.getArgument(3)))
          .when(integration)
          .scrape(any(), any(), any(), any());

      // Act
      PlayerStatisticsSyncSummary segunda = run();

      // Assert
      assertThat(segunda.counters().selected()).isEqualTo(1);
      assertThat(segunda.counters().completed()).isEqualTo(1);
      assertThat(segunda.counters().pending()).isZero();
      assertThat(first.counters().completed()).isEqualTo(4);
      assertThat(first.counters().pending()).isEqualTo(1);
      assertThat(first.counters().completed() + segunda.counters().completed()).isEqualTo(5);
      assertThat(segunda.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName("Sin pendientes el lote cierra completo sin abrir el navegador")
    void sinPendientesNoAbreNavegador() {
      // Arrange
      run();
      when(integration.openSession()).thenThrow(new HeadedBrowserUnavailableException());

      // Act
      PlayerStatisticsSyncSummary summary = run();

      // Assert
      assertThat(summary.status()).isEqualTo(ExtractionRunStatus.COMPLETED);
      assertThat(summary.counters().selected()).isZero();
      assertThat(summary.providerCalls()).isZero();
      assertThat(summary.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName(
        "La ausencia de navegador se propaga como error controlado sin dejar ejecución activa")
    void navegadorNoDisponible() {
      // Arrange
      when(integration.openSession()).thenThrow(new HeadedBrowserUnavailableException());

      // Act
      assertThatThrownBy(() -> run()).isInstanceOf(HeadedBrowserUnavailableException.class);

      // Verify
      // doReturn evita invocar el stub que lanza la excepción durante el propio re-stubbing.
      doReturn(session).when(integration).openSession();
      PlayerStatisticsSyncSummary posterior = run();
      assertThat(posterior.status()).isEqualTo(ExtractionRunStatus.COMPLETED);
      assertThat(posterior.counters().selected()).isEqualTo(5);
    }

    @Test
    @DisplayName("Una fecha futura se rechaza antes de abrir cualquier ejecución")
    void rechazaFechaFutura() {
      // Act / Assert
      assertThatThrownBy(
              () ->
                  service.synchronizeScraping(
                      new PlayerStatisticsSyncRequest(
                          LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1))))
          .isInstanceOf(
              footballmarket.features.statistics.application.exceptions
                  .PlayerStatisticsSynchronizationException.class);
    }
  }

  private String tournamentOf(String providerPlayerId) {
    return footballmarket.support.StatisticsScrapingSamples.tournamentOf(providerPlayerId);
  }
}
