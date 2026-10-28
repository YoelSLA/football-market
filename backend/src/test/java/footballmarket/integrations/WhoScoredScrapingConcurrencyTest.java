package footballmarket.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.playwright.*;
import footballmarket.config.*;
import footballmarket.support.WhoScoredStatisticsPageStub;
import java.time.Clock;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Concurrencia real entre los workers del scraping.
 *
 * <p>Playwright Java está confinado al hilo que lo crea, así que la sesión entrega un navegador a
 * cada worker y el recorrido no se serializa. El doble detiene cada navegación en una barrera: si
 * dos workers no pudieran estar en vuelo a la vez, la barrera vencería.
 */
@org.junit.jupiter.api.Tag("lightweight")
class WhoScoredScrapingConcurrencyTest {
  private static final String HTML = "<html><body>estadísticas</body></html>";

  private PlayerStatisticsProperties properties() {
    return new PlayerStatisticsProperties(4, 4, 1, 20000, 20000, false, true);
  }

  private WhoScoredRequestPacer pacer() {
    AtomicLong time = new AtomicLong();
    return new WhoScoredRequestPacer(1, time::get, milliseconds -> {}, Clock.systemUTC());
  }

  private WhoScoredScrapingSectionNavigator navigator(WhoScoredRequestPacer pacer) {
    return new WhoScoredScrapingSectionNavigator(properties(), pacer);
  }

  private WhoScoredStatisticsScrapingIntegration integration(WhoScoredRequestPacer pacer) {
    WhoScoredProperties provider = mock(WhoScoredProperties.class);
    when(provider.baseUrl()).thenReturn("https://provider.example");
    return new WhoScoredStatisticsScrapingIntegration(
        provider,
        properties(),
        navigator(pacer),
        new WhoScoredScrapingTableParser(),
        new WhoScoredStructuredDataParser(),
        pacer);
  }

  /**
   * Activa el mock estático de Playwright en el hilo llamante.
   *
   * <p>{@link MockedStatic} tiene alcance por hilo: registrarlo solo en el hilo de la petición deja
   * a los workers llamando al {@code Playwright.create} real. Cada worker registra el suyo antes de
   * pedir su navegador.
   */
  private MockedStatic<Playwright> mockPlaywright(Playwright playwright) {
    MockedStatic<Playwright> factory = mockStatic(Playwright.class);
    factory
        .when(() -> Playwright.create(any(Playwright.CreateOptions.class)))
        .thenReturn(playwright);
    return factory;
  }

  /** Página cuya navegación espera a que el otro worker también esté navegando. */
  private Page enqueueBlockingPage(WhoScoredStatisticsPageStub stub, CyclicBarrier barrier) {
    Page page = stub.enqueue(HTML, 200);
    Response response = mock(Response.class);
    when(response.status()).thenReturn(200);
    when(response.text()).thenReturn(HTML);
    when(page.navigate(anyString(), any(Page.NavigateOptions.class)))
        .thenAnswer(
            invocation -> {
              barrier.await(5, TimeUnit.SECONDS);
              return response;
            });
    configureSummary(page);
    return page;
  }

  /** DOM mínimo que el recorrido del resumen consulta antes de entregar el documento. */
  private void configureSummary(Page page) {
    Locator options = mock(Locator.class);
    when(options.count()).thenReturn(1);
    when(page.locator("#player-tournament-stats-options")).thenReturn(options);
    Locator tab = mock(Locator.class);
    when(tab.count()).thenReturn(1);
    when(tab.first()).thenReturn(tab);
    when(page.locator(startsWith("#player-tournament-stats-options a"))).thenReturn(tab);
  }

  private String read(
      WhoScoredScrapingSectionNavigator navigator,
      WhoScoredStatisticsScrapingIntegration.Session session,
      WhoScoredRequestPacer pacer) {
    try {
      return navigator.read(
          session.browser(),
          "https://provider.example/players/123/show",
          WhoScoredScrapingSectionNavigator.INITIAL,
          pacer.statisticsWorker(1),
          () -> {},
          (page, html) -> html);
    } finally {
      session.releaseWorker();
    }
  }

  private String readWithMockedPlaywright(
      WhoScoredScrapingSectionNavigator navigator,
      WhoScoredStatisticsScrapingIntegration.Session session,
      WhoScoredRequestPacer pacer,
      Playwright playwright) {
    try (MockedStatic<Playwright> factory = mockPlaywright(playwright)) {
      return read(navigator, session, pacer);
    }
  }

  private Thread worker(
      WhoScoredStatisticsScrapingIntegration.Session session,
      Playwright playwright,
      CyclicBarrier barrier,
      AtomicReference<Browser> seen,
      AtomicReference<Throwable> failure) {
    Thread thread =
        new Thread(
            () -> {
              try (MockedStatic<Playwright> factory = mockPlaywright(playwright)) {
                Browser first = session.browser();
                Browser again = session.browser();
                seen.set(first);
                if (first != again) {
                  throw new AssertionError("El hilo recibió dos navegadores distintos");
                }
                barrier.await(5, TimeUnit.SECONDS);
                session.releaseWorker();
              } catch (Throwable thrown) {
                failure.compareAndSet(null, thrown);
              }
            });
    thread.setDaemon(true);
    return thread;
  }

  @Nested
  @DisplayName("Lectura simultánea de dos workers")
  class ParallelReads {
    @Test
    @DisplayName("Dos workers recorren el resumen a la vez, cada uno con su navegador")
    void readsConcurrentlyWithDistinctBrowsers() throws Exception {
      // Arrange
      WhoScoredRequestPacer pacer = pacer();
      WhoScoredScrapingSectionNavigator sectionNavigator = navigator(pacer);
      WhoScoredStatisticsScrapingIntegration scraping = integration(pacer);
      WhoScoredStatisticsPageStub first = new WhoScoredStatisticsPageStub();
      WhoScoredStatisticsPageStub second = new WhoScoredStatisticsPageStub();
      CyclicBarrier barrier = new CyclicBarrier(2);
      enqueueBlockingPage(first, barrier);
      enqueueBlockingPage(second, barrier);
      Browser probe = mock(Browser.class);
      Queue<Browser> launches =
          new ConcurrentLinkedQueue<>(List.of(probe, first.browser(), second.browser()));
      Playwright playwright = mock(Playwright.class);
      BrowserType chromium = mock(BrowserType.class);
      when(playwright.chromium()).thenReturn(chromium);
      when(chromium.launch(any(BrowserType.LaunchOptions.class)))
          .thenAnswer(invocation -> launches.remove());
      try (MockedStatic<Playwright> factory = mockPlaywright(playwright)) {
        try (WhoScoredStatisticsScrapingIntegration.Session session = scraping.openSession();
            ExecutorService executor = Executors.newFixedThreadPool(2)) {
          // Act
          Future<String> left =
              executor.submit(
                  () -> readWithMockedPlaywright(sectionNavigator, session, pacer, playwright));
          Future<String> right =
              executor.submit(
                  () -> readWithMockedPlaywright(sectionNavigator, session, pacer, playwright));
          // Assert
          assertThat(left.get(10, TimeUnit.SECONDS)).isEqualTo(HTML);
          assertThat(right.get(10, TimeUnit.SECONDS)).isEqualTo(HTML);
        }
        // Verify
        assertThat(launches).isEmpty();
        ArgumentCaptor<BrowserType.LaunchOptions> launch =
            ArgumentCaptor.forClass(BrowserType.LaunchOptions.class);
        verify(chromium, times(3)).launch(launch.capture());
        assertThat(launch.getAllValues())
            .allSatisfy(options -> assertThat(options.headless).isFalse());
        verify(probe).close();
        verify(first.browser()).close();
        verify(second.browser()).close();
        verify(playwright, times(3)).close();
      }
    }
  }

  @Nested
  @DisplayName("Ciclo de vida del navegador por hilo")
  class WorkerLifecycle {
    @Test
    @DisplayName("Cada hilo conserva su navegador y libera solo el que creó")
    void isolatesBrowserPerThread() throws Exception {
      // Arrange
      WhoScoredRequestPacer pacer = pacer();
      WhoScoredStatisticsScrapingIntegration scraping = integration(pacer);
      Browser probe = mock(Browser.class);
      Browser firstBrowser = mock(Browser.class);
      Browser secondBrowser = mock(Browser.class);
      Queue<Browser> launches =
          new ConcurrentLinkedQueue<>(List.of(probe, firstBrowser, secondBrowser));
      Playwright playwright = mock(Playwright.class);
      BrowserType chromium = mock(BrowserType.class);
      when(playwright.chromium()).thenReturn(chromium);
      when(chromium.launch(any(BrowserType.LaunchOptions.class)))
          .thenAnswer(invocation -> launches.remove());
      try (MockedStatic<Playwright> factory = mockPlaywright(playwright)) {
        try (WhoScoredStatisticsScrapingIntegration.Session session = scraping.openSession()) {
          CyclicBarrier barrier = new CyclicBarrier(2);
          AtomicReference<Browser> firstSeen = new AtomicReference<>();
          AtomicReference<Browser> secondSeen = new AtomicReference<>();
          AtomicReference<Throwable> failure = new AtomicReference<>();
          Thread first = worker(session, playwright, barrier, firstSeen, failure);
          Thread second = worker(session, playwright, barrier, secondSeen, failure);
          // Act
          first.start();
          second.start();
          first.join(10_000);
          second.join(10_000);
          // Assert
          assertThat(failure.get()).isNull();
          assertThat(first.isAlive()).isFalse();
          assertThat(second.isAlive()).isFalse();
          assertThat(firstSeen.get()).isNotNull().isNotSameAs(secondSeen.get());
        }
        // Verify
        assertThat(launches).isEmpty();
        verify(probe).close();
        verify(firstBrowser).close();
        verify(secondBrowser).close();
        verify(playwright, times(3)).close();
      }
    }
  }
}
