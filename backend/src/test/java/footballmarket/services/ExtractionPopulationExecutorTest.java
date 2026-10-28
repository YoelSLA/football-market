package footballmarket.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import footballmarket.config.WhoScoredProperties;
import footballmarket.integrations.WhoScoredIntegration;
import footballmarket.integrations.WhoScoredStatisticsScrapingIntegration;
import footballmarket.integrations.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.records.*;
import footballmarket.models.records.PlayerStatisticsSyncRequest;
import footballmarket.models.records.PlayerStatisticsSyncSummary;
import footballmarket.services.exceptions.PlayerStatisticsSyncInProgressException;
import footballmarket.support.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.*;

/** Recorrido poblacional desde Service real con DB aislada y proveedor controlado. */
@SpringBootTest
@ActiveProfiles("test")
@Import({
  TestcontainersConfiguration.class,
  IdentityCatalogueFixture.class,
  StatisticsRunTestData.class
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class ExtractionPopulationExecutorTest {
  private static final AtomicReference<Instant> NOW = new AtomicReference<>();
  private static final LocalDate DATE = LocalDate.of(2026, 10, 15);
  @Autowired private PlayerStatisticsSynchronizationService service;
  @Autowired private PlayerIdentitySynchronizationService identities;
  @Autowired private IdentityCatalogueFixture fixture;
  @Autowired private StatisticsRunTestData runData;
  @MockitoBean private WhoScoredStatisticsScrapingIntegration integration;
  @MockitoBean private WhoScoredIntegration identityIntegration;
  @MockitoSpyBean private WhoScoredProperties properties;

  @TestBean(name = "statisticsClock", methodName = "clock")
  private Clock clock;

  static Clock clock() {
    return new Clock() {
      @Override
      public ZoneId getZone() {
        return ZoneOffset.UTC;
      }

      @Override
      public Clock withZone(ZoneId zone) {
        return clock();
      }

      @Override
      public Instant instant() {
        return NOW.get();
      }
    };
  }

  @BeforeEach
  void prepare() {
    NOW.set(Instant.parse("2026-10-15T23:50:00Z"));
    List<Long> leagues = fixture.prepare();
    List<Integer> tournaments = List.of(2, 3, 4, 5, 22);
    List<WhoScoredProperties.Competition> bindings = new ArrayList<>();
    for (int index = 0; index < leagues.size(); index++) {
      bindings.add(new WhoScoredProperties.Competition(leagues.get(index), tournaments.get(index)));
    }
    doReturn(bindings).when(properties).competitions();
    when(identityIntegration.currentPage(anyInt(), eq(1)))
        .thenAnswer(
            invocation -> {
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
              return new WhoScoredCatalogPage(
                  List.of(player),
                  List.of(),
                  new WhoScoredPaging(1, 1, 1, 1, 1, 1),
                  "page-" + tournament);
            });
    identities.synchronizeCurrent();
    when(integration.openSession())
        .thenReturn(mock(WhoScoredStatisticsScrapingIntegration.Session.class));
    when(integration.scrape(any(), any(), any(), any()))
        .thenAnswer(invocation -> StatisticsScrapingSamples.player(invocation.getArgument(3)));
  }

  @Nested
  @DisplayName("Instantánea completa sin corte temporal")
  class Population {
    @Test
    @DisplayName(
        "Un run reciente sin ejecución viva se recupera sin esperar un presupuesto temporal")
    void recoversRecentOrphan() {
      // Arrange
      Long orphan = runData.orphanScrapingRun(DATE);
      PlayerStatisticsSyncRequest request = new PlayerStatisticsSyncRequest(DATE);
      // Act
      PlayerStatisticsSyncSummary summary = service.synchronizeScraping(request);
      // Assert
      assertThat(summary.runId()).isNotEqualTo(orphan);
      assertThat(summary.counters().completed()).isEqualTo(5);
      assertThat(summary.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName("Cruzar medianoche y el antiguo presupuesto no corta ni cambia la fecha inicial")
    void preservesInitialDateBeyondOldBudget() {
      // Arrange
      doAnswer(
              invocation -> {
                NOW.set(Instant.parse("2026-10-16T02:00:00Z"));
                return StatisticsScrapingSamples.player(invocation.getArgument(3));
              })
          .when(integration)
          .scrape(any(), any(), any(), any());
      PlayerStatisticsSyncRequest request = new PlayerStatisticsSyncRequest(null);
      // Act
      PlayerStatisticsSyncSummary summary = service.synchronizeScraping(request);
      // Assert
      assertThat(summary.extractionDate()).isEqualTo(DATE);
      assertThat(summary.counters().selected()).isEqualTo(5);
      assertThat(summary.counters().completed()).isEqualTo(5);
      assertThat(summary.providerCalls()).isEqualTo(10);
      assertThat(summary.coverageComplete()).isTrue();
    }

    @Test
    @DisplayName("Los timeouts individuales se intentan una vez y no detienen otros pendientes")
    void continuesAfterTimeouts() {
      // Arrange
      ConcurrentMap<String, Integer> attempts = new ConcurrentHashMap<>();
      doAnswer(
              invocation -> {
                String externalId = invocation.getArgument(3);
                attempts.merge(externalId, 1, Integer::sum);
                throw new WhoScoredStatisticsException("PROVIDER_TIMEOUT", null, "initial", 1);
              })
          .when(integration)
          .scrape(any(), any(), any(), any());
      PlayerStatisticsSyncRequest request = new PlayerStatisticsSyncRequest(DATE);
      // Act
      PlayerStatisticsSyncSummary summary = service.synchronizeScraping(request);
      // Assert
      assertThat(summary.counters().selected()).isEqualTo(5);
      assertThat(summary.counters().failed()).isEqualTo(5);
      assertThat(summary.counters().pending()).isEqualTo(5);
      assertThat(summary.providerCalls()).isEqualTo(5);
      assertThat(attempts).hasSize(5);
      assertThat(attempts.values()).containsOnly(1);
    }

    @Test
    @DisplayName("Una segunda entrada no recupera un run vivo aunque avance el reloj varios días")
    void rejectsConcurrentRunUntilCleanup() throws Exception {
      // Arrange
      CountDownLatch started = new CountDownLatch(1);
      CountDownLatch release = new CountDownLatch(1);
      doAnswer(
              invocation -> {
                started.countDown();
                if (!release.await(10, TimeUnit.SECONDS)) {
                  throw new AssertionError("No se liberó el proveedor controlado");
                }
                return StatisticsScrapingSamples.player(invocation.getArgument(3));
              })
          .when(integration)
          .scrape(any(), any(), any(), any());
      PlayerStatisticsSyncRequest request = new PlayerStatisticsSyncRequest(DATE);
      try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
        Future<PlayerStatisticsSyncSummary> active =
            executor.submit(() -> service.synchronizeScraping(request));
        try {
          assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
          NOW.set(Instant.parse("2026-10-18T23:50:00Z"));
          // Act / Assert
          assertThatThrownBy(() -> service.synchronizeScraping(request))
              .isInstanceOf(PlayerStatisticsSyncInProgressException.class);
        } finally {
          release.countDown();
        }
        // Verify
        assertThat(active.get(10, TimeUnit.SECONDS).coverageComplete()).isTrue();
      }
    }
  }
}
