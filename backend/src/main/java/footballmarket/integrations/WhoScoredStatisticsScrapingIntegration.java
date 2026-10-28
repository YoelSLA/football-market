package footballmarket.integrations;

import com.microsoft.playwright.*;
import footballmarket.config.PlayerStatisticsProperties;
import footballmarket.config.WhoScoredProperties;
import footballmarket.integrations.exceptions.*;
import footballmarket.integrations.records.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Adaptación headed de WhoScored con recursos creados bajo demanda, nunca al arrancar Spring. */
@Component
public class WhoScoredStatisticsScrapingIntegration {
  private static final Logger LOG =
      LoggerFactory.getLogger(WhoScoredStatisticsScrapingIntegration.class);

  private final WhoScoredProperties provider;
  private final PlayerStatisticsProperties properties;
  private final WhoScoredScrapingSectionNavigator navigator;
  private final WhoScoredScrapingTableParser tables;
  private final WhoScoredStructuredDataParser structured;
  private final WhoScoredRequestPacer pacer;

  public WhoScoredStatisticsScrapingIntegration(
      WhoScoredProperties provider,
      PlayerStatisticsProperties properties,
      WhoScoredScrapingSectionNavigator navigator,
      WhoScoredScrapingTableParser tables,
      WhoScoredStructuredDataParser structured,
      WhoScoredRequestPacer pacer) {
    this.provider = provider;
    this.properties = properties;
    this.navigator = navigator;
    this.tables = tables;
    this.structured = structured;
    this.pacer = pacer;
  }

  /**
   * Comprueba de forma temprana que el navegador headed arranca y deja la sesión sin recursos.
   *
   * <p>Cada worker crea su propio navegador cuando lo necesita. El sondeo evita abrir el run y
   * seleccionar jugadores cuando no hay entorno gráfico, y sus recursos se liberan antes de
   * recorrer nada.
   */
  public Session openSession() {
    BrowserHandle probe;
    try {
      probe = this.openBrowser();
    } catch (RuntimeException failure) {
      LOG.warn(
          "No se pudo abrir el navegador visible de WhoScored: {}", failure.toString(), failure);
      throw new HeadedBrowserUnavailableException(failure.getMessage());
    }
    try (probe) {
      LOG.info("Navegador headed disponible; se libera hasta el primer worker");
    }
    return new Session();
  }

  /**
   * Abre una instancia de Playwright y su Chromium headed.
   *
   * <p>Playwright Java está confinado al hilo que la crea: cada worker mantiene la suya y la cierra
   * en ese mismo hilo.
   */
  private BrowserHandle openBrowser() {
    Playwright playwright = null;
    try {
      playwright =
          Playwright.create(
              new Playwright.CreateOptions()
                  .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
      Browser browser =
          playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
      return new BrowserHandle(playwright, browser);
    } catch (RuntimeException failure) {
      if (playwright != null) {
        try {
          playwright.close();
        } catch (RuntimeException cleanup) {
          failure.addSuppressed(cleanup);
        }
      }
      throw failure;
    }
  }

  /** Recursos de navegador de un hilo; se usan y se cierran siempre en él. */
  private record BrowserHandle(Playwright playwright, Browser browser) implements AutoCloseable {
    @Override
    public void close() {
      RuntimeException failure = null;
      try {
        this.browser.close();
      } catch (RuntimeException error) {
        failure = error;
      }
      try {
        this.playwright.close();
      } catch (RuntimeException error) {
        if (failure == null) {
          failure = error;
        } else {
          failure.addSuppressed(error);
        }
      }
      if (failure != null) {
        throw failure;
      }
    }
  }

  /**
   * Procesa el resumen y las dieciséis categorías antes de devolver cualquier dato como completo.
   */
  public WhoScoredScrapedPlayer scrape(Session session, String playerId) {
    return this.scrape(session, null, null, playerId);
  }

  /** Scrapea un jugador y adjunta a los logs los identificadores locales de la campaña. */
  public WhoScoredScrapedPlayer scrape(
      Session session, Long runId, Long localPlayerId, String playerId) {
    if (playerId == null || !playerId.matches("[1-9][0-9]*")) {
      throw new WhoScoredStatisticsException("PROVIDER_DATA_INCONSISTENT", null);
    }
    long playerStarted = System.nanoTime();
    String diagnostic = diagnostic(runId, localPlayerId);
    List<WhoScoredScrapedContextRow> rows = new ArrayList<>();
    java.util.concurrent.atomic.AtomicLong calls = new java.util.concurrent.atomic.AtomicLong();
    WhoScoredStructuredDataBlock[] data = {new WhoScoredStructuredDataBlock(List.of())};
    String url = this.provider.baseUrl() + "/players/" + playerId + "/show";
    for (String section : WhoScoredScrapingSectionNavigator.SECTIONS) {
      Instant started = Instant.now();
      LOG.info("{} Sección {} iniciada", diagnostic, section);
      List<WhoScoredScrapedContextRow> parsed;
      try {
        parsed =
            this.navigator.read(
                session.browser(),
                url,
                section,
                runId,
                localPlayerId,
                session.workers.get(),
                calls::incrementAndGet,
                (page, originalHtml) ->
                    section.equals(WhoScoredScrapingSectionNavigator.INITIAL)
                        ? this.summary(page, originalHtml, data)
                        : this.detail(page, originalHtml, diagnostic));
      } catch (RuntimeException failure) {
        // El recorrido abandona al jugador en la primera sección que falla, de modo que la
        // incidencia
        // solo es accionable si declara dónde se rompió el recorrido.
        LOG.warn(
            "{} Sección {} fallida tras {} ms: {}",
            diagnostic,
            section,
            Duration.between(started, Instant.now()).toMillis(),
            describe(failure),
            failure);
        if (failure instanceof WhoScoredStatisticsException known) {
          WhoScoredStatisticsException reported =
              new WhoScoredStatisticsException(
                  known.getCode(),
                  known.getProviderStatus(),
                  known.getSection() == null ? section : known.getSection(),
                  calls.get());
          reported.initCause(known);
          throw reported;
        }
        throw failure;
      }
      LOG.info(
          "{} Sección {} leída en {} ms: {} contextos",
          diagnostic,
          section,
          Duration.between(started, Instant.now()).toMillis(),
          parsed.size());
      rows.addAll(parsed);
    }
    LOG.info(
        "{} Jugador scrapeado en {} ms: {} secciones, {} filas, {} llamadas observadas",
        diagnostic,
        Duration.ofNanos(System.nanoTime() - playerStarted).toMillis(),
        WhoScoredScrapingSectionNavigator.SECTIONS.size(),
        rows.size(),
        calls.get());
    return new WhoScoredScrapedPlayer(playerId, rows, data[0], calls.get(), List.of());
  }

  private static String diagnostic(Long runId, Long localPlayerId) {
    return "run="
        + (runId == null ? "n/a" : runId)
        + " player="
        + (localPlayerId == null ? "n/a" : localPlayerId)
        + " worker="
        + Thread.currentThread().getName();
  }

  /** Resumen de la carga inicial: contexto de temporada y totales. */
  private List<WhoScoredScrapedContextRow> summary(
      Page page, String originalHtml, WhoScoredStructuredDataBlock[] data) {
    WhoScoredStructuredDataBlock block = this.structured.parse(originalHtml);
    data[0] = block;
    return this.tables.parse(
        page,
        WhoScoredScrapingSectionNavigator.container(WhoScoredScrapingSectionNavigator.INITIAL),
        WhoScoredScrapingSectionNavigator.INITIAL,
        block,
        Instant.now());
  }

  /**
   * Tabla de detalle completa: recorre las dieciséis categorías sobre la misma carga.
   *
   * <p>Cada categoría es una interacción sobre el desplegable, como la haría una persona, y su
   * tabla se lee cuando el proveedor ha repoblado el contenido. La sección se declara con la
   * categoría que falló para que la incidencia diga dónde se rompió.
   */
  private List<WhoScoredScrapedContextRow> detail(
      Page page, String originalHtml, String diagnostic) {
    WhoScoredStructuredDataBlock block = this.structured.parse(originalHtml);
    List<WhoScoredScrapedContextRow> rows = new ArrayList<>();
    for (String category : WhoScoredScrapingSectionNavigator.CATEGORIES) {
      Instant started = Instant.now();
      long categoryStarted = System.nanoTime();
      try {
        this.navigator.chooseCategory(page, category, diagnostic);
        rows.addAll(
            this.tables.parse(
                page,
                WhoScoredScrapingSectionNavigator.container(
                    WhoScoredScrapingSectionNavigator.DETAILED),
                category,
                block,
                Instant.now()));
      } catch (RuntimeException failure) {
        LOG.warn(
            "{} categoría={} fallida tras {} ms: {}",
            diagnostic,
            category,
            Duration.between(started, Instant.now()).toMillis(),
            describe(failure),
            failure);
        if (failure instanceof WhoScoredStatisticsException known && known.getSection() == null) {
          throw new WhoScoredStatisticsException(
              known.getCode(), known.getProviderStatus(), category);
        }
        throw failure;
      }
      LOG.info(
          "{} categoría={} leída en {} ms: {} contextos",
          diagnostic,
          category,
          Duration.ofNanos(System.nanoTime() - categoryStarted).toMillis(),
          rows.size());
    }
    return List.copyOf(rows);
  }

  private static String describe(RuntimeException failure) {
    String code = failure instanceof WhoScoredStatisticsException known ? known.getCode() : null;
    return code == null ? failure.getClass().getSimpleName() : code;
  }

  /**
   * Recursos del lote: un navegador por hilo de worker y el ritmo estadístico de ese worker.
   *
   * <p>Playwright Java no admite el uso concurrente de una misma instancia, así que la concurrencia
   * la garantiza una instancia confinada a cada hilo, no un bloqueo compartido. Los recursos de un
   * worker se liberan en su propio hilo al terminar el lote.
   */
  public final class Session implements AutoCloseable {
    private final ThreadLocal<BrowserHandle> handles = new ThreadLocal<>();
    private final java.util.concurrent.atomic.AtomicInteger liveBrowsers =
        new java.util.concurrent.atomic.AtomicInteger();
    private final ThreadLocal<WhoScoredRequestPacer.StatisticsWorker> workers =
        ThreadLocal.withInitial(
            () ->
                WhoScoredStatisticsScrapingIntegration.this.pacer.statisticsWorker(
                    WhoScoredStatisticsScrapingIntegration.this.properties.requestIntervalMs()));

    private Session() {}

    /** Navegador del hilo llamante, creado la primera vez que ese worker lo necesita. */
    Browser browser() {
      BrowserHandle handle = this.handles.get();
      if (handle == null) {
        try {
          handle = WhoScoredStatisticsScrapingIntegration.this.openBrowser();
        } catch (RuntimeException unavailable) {
          throw new HeadedBrowserUnavailableException(null);
        }
        this.handles.set(handle);
        this.liveBrowsers.incrementAndGet();
      }
      return handle.browser();
    }

    /** Libera los recursos del hilo llamante; debe invocarse en el hilo del worker. */
    public void releaseWorker() {
      this.workers.remove();
      BrowserHandle handle = this.handles.get();
      if (handle == null) {
        return;
      }
      this.handles.remove();
      this.liveBrowsers.decrementAndGet();
      handle.close();
    }

    @Override
    public void close() {
      // El hilo de la petición no posee navegadores de worker; solo se libera por si acaso.
      this.releaseWorker();
      int open = this.liveBrowsers.get();
      if (open != 0) {
        LOG.warn("Quedaron {} navegadores de scraping sin cerrar al cerrar la sesión", open);
      }
    }
  }
}
