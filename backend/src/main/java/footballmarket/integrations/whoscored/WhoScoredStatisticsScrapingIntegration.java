package footballmarket.integrations.whoscored;

import com.microsoft.playwright.*;
import footballmarket.features.statistics.infraestructure.config.PlayerStatisticsProperties;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import footballmarket.integrations.whoscored.exceptions.*;
import footballmarket.integrations.whoscored.records.*;
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

  public Session openSession() {
    Playwright playwright = null;
    try {
      playwright =
          Playwright.create(
              new Playwright.CreateOptions()
                  .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
      Browser browser =
          playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
      return new Session(playwright, browser);
    } catch (RuntimeException failure) {
      if (playwright != null) {
        playwright.close();
      }
      LOG.warn(
          "No se pudo abrir el navegador visible de WhoScored: {}", failure.toString(), failure);
      throw new HeadedBrowserUnavailableException(failure.getMessage());
    }
  }

  /**
   * Procesa el resumen y las dieciséis categorías antes de devolver cualquier dato como completo.
   */
  public WhoScoredScrapedPlayer scrape(Session session, String playerId, Instant deadline) {
    if (playerId == null || !playerId.matches("[1-9][0-9]*")) {
      throw new WhoScoredStatisticsException("PROVIDER_DATA_INCONSISTENT", null);
    }
    List<WhoScoredScrapedContextRow> rows = new ArrayList<>();
    java.util.concurrent.atomic.AtomicLong calls = new java.util.concurrent.atomic.AtomicLong();
    WhoScoredStructuredDataBlock[] data = {new WhoScoredStructuredDataBlock(List.of())};
    String url = this.provider.baseUrl() + "/player/" + playerId + "/show";
    for (String section : WhoScoredScrapingSectionNavigator.SECTIONS) {
      Instant started = Instant.now();
      LOG.info("Sección {} iniciada", section);
      List<WhoScoredScrapedContextRow> parsed;
      try {
        parsed =
            this.navigator.read(
                session.browser,
                url,
                section,
                deadline,
                session.workers.get(),
                calls::incrementAndGet,
                (page, originalHtml) ->
                    section.equals(WhoScoredScrapingSectionNavigator.INITIAL)
                        ? this.summary(page, originalHtml, data)
                        : this.detail(page, originalHtml, deadline));
      } catch (RuntimeException failure) {
        // El recorrido abandona al jugador en la primera sección que falla, de modo que la
        // incidencia
        // solo es accionable si declara dónde se rompió el recorrido.
        LOG.warn(
            "Sección {} fallida tras {} ms: {}",
            section,
            Duration.between(started, Instant.now()).toMillis(),
            describe(failure),
            failure);
        if (failure instanceof WhoScoredStatisticsException known && known.getSection() == null) {
          throw new WhoScoredStatisticsException(
              known.getCode(), known.getProviderStatus(), section);
        }
        throw failure;
      }
      LOG.info(
          "Sección {} leída en {} ms: {} contextos",
          section,
          Duration.between(started, Instant.now()).toMillis(),
          parsed.size());
      rows.addAll(parsed);
    }
    return new WhoScoredScrapedPlayer(playerId, rows, data[0], calls.get(), List.of());
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
      Page page, String originalHtml, Instant deadline) {
    WhoScoredStructuredDataBlock block = this.structured.parse(originalHtml);
    List<WhoScoredScrapedContextRow> rows = new ArrayList<>();
    for (String category : WhoScoredScrapingSectionNavigator.CATEGORIES) {
      Instant started = Instant.now();
      try {
        this.navigator.chooseCategory(page, category, deadline);
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
            "Categoría {} fallida tras {} ms: {}",
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
          "Categoría {} leída en {} ms: {} contextos",
          category,
          Duration.between(started, Instant.now()).toMillis(),
          rows.size());
    }
    return List.copyOf(rows);
  }

  private static String describe(RuntimeException failure) {
    String code = failure instanceof WhoScoredStatisticsException known ? known.getCode() : null;
    return code == null ? failure.getClass().getSimpleName() : code;
  }

  /** Recursos de lote; el navegador se comparte y cada sección tiene su propio contexto. */
  public final class Session implements AutoCloseable {
    private final Playwright playwright;
    private final Browser browser;
    private final ThreadLocal<WhoScoredRequestPacer.StatisticsWorker> workers =
        ThreadLocal.withInitial(
            () ->
                WhoScoredStatisticsScrapingIntegration.this.pacer.statisticsWorker(
                    WhoScoredStatisticsScrapingIntegration.this.properties.requestIntervalMs()));

    private Session(Playwright playwright, Browser browser) {
      this.playwright = playwright;
      this.browser = browser;
    }

    @Override
    public void close() {
      synchronized (this.browser) {
        try {
          this.browser.close();
        } finally {
          this.playwright.close();
          this.workers.remove();
        }
      }
    }
  }
}
