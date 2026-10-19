package footballmarket.services;

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

import footballmarket.config.PlayerStatisticsProperties;
import footballmarket.config.WhoScoredProperties;
import footballmarket.integrations.WhoScoredIntegration;
import footballmarket.integrations.WhoScoredStatisticsScrapingIntegration;
import footballmarket.integrations.exceptions.HeadedBrowserUnavailableException;
import footballmarket.integrations.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.records.WhoScoredScrapedContextRow;
import footballmarket.integrations.records.WhoScoredScrapedMetricCell;
import footballmarket.integrations.records.WhoScoredScrapedPlayer;
import footballmarket.models.enums.ExtractionRunStatus;
import footballmarket.models.records.PlayerStatisticsSyncRequest;
import footballmarket.models.records.PlayerStatisticsSyncSummary;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.LocalDate;
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

/** Lote de scraping, reanudación, recuperación de ejecución activa y fallo por jugador. */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, IdentityCatalogueFixture.class})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class PlayerStatisticsSynchronizationServiceTest {
  @Autowired private PlayerStatisticsSynchronizationService service;
  @Autowired private PlayerStatisticsCoverageService coverage;
  @Autowired private PlayerIdentitySynchronizationService identities;
  @Autowired private IdentityCatalogueFixture fixture;
  @Autowired private footballmarket.repositories.PlayerExternalReferenceRepository references;
  @Autowired private footballmarket.repositories.PlayerRepository players;
  @MockitoBean private WhoScoredStatisticsScrapingIntegration integration;
  @MockitoBean private WhoScoredIntegration identityIntegration;
  @MockitoSpyBean private WhoScoredProperties properties;
  @MockitoSpyBean private PlayerStatisticsProperties statistics;
  private WhoScoredStatisticsScrapingIntegration.Session session;

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
              return new footballmarket.integrations.records.WhoScoredCatalogPage(
                  List.of(
                      new footballmarket.integrations.records.WhoScoredPlayerIdentity(
                          Integer.toString(1000 + index),
                          "Player " + index,
                          null,
                          "Team " + index,
                          tournament,
                          null,
                          null,
                          null)),
                  List.of(),
                  new footballmarket.integrations.records.WhoScoredPaging(1, 1, 1, 1, 1, 1),
                  "page-" + tournament);
            });
    doReturn(1_800_000L).when(statistics).scrapingBatchTimeoutMs();
    identities.synchronizeCurrent();
    session = mock(WhoScoredStatisticsScrapingIntegration.Session.class);
    when(integration.openSession()).thenReturn(session);
    when(integration.scrape(any(), any(), any()))
        .thenAnswer(invocation -> scraped(invocation.getArgument(1)));
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
        new footballmarket.integrations.records.WhoScoredStructuredDataBlock(List.of()),
        18,
        List.of());
  }

  private PlayerStatisticsSyncSummary run(Integer maxPlayers) {
    return service.synchronizeScraping(
        new PlayerStatisticsSyncRequest(LocalDate.now(java.time.ZoneOffset.UTC), maxPlayers));
  }

  @Nested
  @DisplayName("Lote de scraping sobre la población elegible")
  class Batch {
    @Test
    @DisplayName("Un máximo de un jugador procesa un único pendiente y deja el resto pendiente")
    void limitaASoloUnJugador() {
      // Act
      PlayerStatisticsSyncSummary summary = run(1);

      // Assert
      assertThat(summary.status()).isEqualTo(ExtractionRunStatus.COMPLETED);
      assertThat(summary.counters().eligible()).isEqualTo(5);
      assertThat(summary.counters().excludedWithoutReference()).isZero();
      assertThat(summary.counters().selected()).isEqualTo(1);
      assertThat(summary.counters().completed()).isEqualTo(1);
      assertThat(summary.counters().pending()).isEqualTo(4);
      assertThat(summary.coverageComplete()).isFalse();
    }

    @Test
    @DisplayName("Sin límite explícito el lote recorre toda la población elegible del día")
    void recorrePoblacionCompleta() {
      // Act
      PlayerStatisticsSyncSummary summary = run(null);

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
          .scrape(any(), any(), any());

      // Act
      PlayerStatisticsSyncSummary summary = run(null);

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
                  "1001".equals(invocation.getArgument(1, String.class))
                      ? scraped("1001")
                      : throwBlocked())
          .when(integration)
          .scrape(any(), any(), any());

      // Act
      PlayerStatisticsSyncSummary summary = run(null);

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
      run(2);

      // Act
      PlayerStatisticsSyncSummary segunda = run(2);

      // Assert
      assertThat(segunda.counters().selected()).isEqualTo(2);
      assertThat(segunda.counters().completed()).isEqualTo(2);
      assertThat(segunda.counters().pending()).isEqualTo(1);
      assertThat(
              coverage
                  .coverage(
                      segunda.extractionDate(), footballmarket.models.enums.MetricSource.SCRAPING)
                  .completedPlayers())
          .isEqualTo(4);
    }

    @Test
    @DisplayName("Sin pendientes el lote cierra completo sin abrir el navegador")
    void sinPendientesNoAbreNavegador() {
      // Arrange
      run(null);
      when(integration.openSession()).thenThrow(new HeadedBrowserUnavailableException());

      // Act
      PlayerStatisticsSyncSummary summary = run(null);

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
      assertThatThrownBy(() -> run(1)).isInstanceOf(HeadedBrowserUnavailableException.class);

      // Verify
      // doReturn evita invocar el stub que lanza la excepción durante el propio re-stubbing.
      doReturn(session).when(integration).openSession();
      PlayerStatisticsSyncSummary posterior = run(1);
      assertThat(posterior.status()).isEqualTo(ExtractionRunStatus.COMPLETED);
      assertThat(posterior.counters().selected()).isEqualTo(1);
    }

    @Test
    @DisplayName("Una fecha futura se rechaza antes de abrir cualquier ejecución")
    void rechazaFechaFutura() {
      // Act / Assert
      assertThatThrownBy(
              () ->
                  service.synchronizeScraping(
                      new PlayerStatisticsSyncRequest(
                          LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1), 1)))
          .isInstanceOf(
              footballmarket.services.exceptions.PlayerStatisticsSynchronizationException.class);
    }
  }

  private String tournamentOf(String providerPlayerId) {
    Long playerId =
        references
            .findByProviderAndExternalId(
                footballmarket.models.enums.ExternalProvider.WHOSCORED, providerPlayerId)
            .orElseThrow()
            .getPlayer()
            .getId();
    Long leagueId = players.findVisibleById(playerId).orElseThrow().getTeam().getLeague().getId();
    return String.valueOf(
        properties.competitions().stream()
            .filter(competition -> competition.leagueId().equals(leagueId))
            .findFirst()
            .orElseThrow()
            .tournamentId());
  }
}
