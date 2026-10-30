package footballmarket.integrations;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import footballmarket.config.PlayerStatisticsProperties;
import footballmarket.integrations.exceptions.*;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Carga cada sección del recorrido en su propio contexto de navegador.
 *
 * <p>Las dieciséis categorías de detalle comparten una única carga de la tabla de detalle: cambiar
 * de categoría recarga solo su contenido. Abrir un contexto por categoría multiplicaba las
 * navegaciones y hacía la lectura dependiente de que cada carga en frío completara su tabla, lo que
 * se maniaba como fallos intermitentes en la tercera o cuarta categoría.
 */
@Component
public class WhoScoredScrapingSectionNavigator {
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(WhoScoredScrapingSectionNavigator.class);

  public static final String INITIAL = "initial";
  public static final String DETAILED = "detailed";

  /** Secciones que requieren su propia carga del documento del proveedor. */
  public static final List<String> SECTIONS = List.of(INITIAL, DETAILED);

  /** Categorias del desplegable de detalle, en el orden publicado por el proveedor. */
  public static final List<String> CATEGORIES =
      List.of(
          "aerial",
          "assists",
          "blocks",
          "cards",
          "clearances",
          "dribbles",
          "fouls",
          "goals",
          "interception",
          "key-passes",
          "offsides",
          "passes",
          "possession-loss",
          "saves",
          "shots",
          "tackles");

  /** Margen para confirmar selección y visibilidad tras el clic, posterior a la preparación. */
  private static final long CLICK_GRACE_MILLIS = 8_000L;

  private final PlayerStatisticsProperties properties;
  private final WhoScoredRequestPacer pacer;

  public WhoScoredScrapingSectionNavigator(
      PlayerStatisticsProperties properties, WhoScoredRequestPacer pacer) {
    this.properties = properties;
    this.pacer = pacer;
  }

  /**
   * El callback lee el DOM antes de cerrar el contexto; no se reintenta ninguna operación.
   *
   * <p>Las pestañas no se abren por la URL: el hash deja la página en su estado inicial. La sección
   * se activa con un clic sobre su pestaña, que es lo que dispara la carga perezosa. En la tabla de
   * detalle la carga la completa el filtro de categorías, que es lo primero que se espera.
   *
   * <p>El {@code Browser} recibido pertenece al hilo llamante y no se comparte: la concurrencia
   * entre workers la garantiza una instancia por worker, así que el recorrido no debe envolverse en
   * ningún bloqueo compartido.
   */
  public <T> T read(
      Browser browser,
      String url,
      String section,
      WhoScoredRequestPacer.StatisticsWorker worker,
      Runnable navigationStarted,
      java.util.function.BiFunction<Page, String, T> reader) {
    return this.read(browser, url, section, null, null, worker, navigationStarted, reader);
  }

  /** Lee una sección y registra sus fases correlacionadas con los identificadores locales. */
  public <T> T read(
      Browser browser,
      String url,
      String section,
      Long runId,
      Long playerId,
      WhoScoredRequestPacer.StatisticsWorker worker,
      Runnable navigationStarted,
      java.util.function.BiFunction<Page, String, T> reader) {
    String tab = section.equals(INITIAL) ? "summary" : "detailed";
    String diagnostic = diagnostic(runId, playerId, section);
    int attemptNumber = 1;
    long attemptStarted = System.nanoTime();
    try {
      return this.pacer.executeStatistics(
          worker,
          null,
          () -> {
            String[] phase = {"open-context"};
            long phaseStarted = System.nanoTime();
            try (BrowserContext context = browser.newContext()) {
              Page page = context.newPage();
              page.setDefaultTimeout(this.properties.navigationTimeoutMs());
              page.setDefaultNavigationTimeout(this.properties.navigationTimeoutMs());
              phase[0] = "navigate";
              phaseStarted = System.nanoTime();
              if (this.properties.filterThirdPartyResources()) {
                page.route(
                    "**/*",
                    route -> {
                      String host = java.net.URI.create(route.request().url()).getHost();
                      if (host != null
                          && List.of(
                                  "doubleclick.net",
                                  "googlesyndication.com",
                                  "google-analytics.com")
                              .stream()
                              .anyMatch(
                                  domain -> host.equals(domain) || host.endsWith("." + domain))) {
                        route.abort();
                      } else {
                        route.resume();
                      }
                    });
              }
              navigationStarted.run();
              Response response =
                  page.navigate(
                      url,
                      new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
              LOG.info(
                  "{} fase=navigate intento={} duraciónMs={} status={}",
                  diagnostic,
                  attemptNumber,
                  elapsedMillis(phaseStarted),
                  response == null ? "null" : response.status());
              if (response == null) {
                throw new WhoScoredStatisticsUnavailableException("PROVIDER_TRANSPORT_ERROR", null);
              }
              int status = response.status();
              if (status == 429) {
                this.pacer.defer(response.headerValue("retry-after"));
                throw new WhoScoredStatisticsRateLimitException(status);
              }
              if (status == 403 || status == 404) {
                throw new WhoScoredStatisticsException(
                    status == 403 ? "PROVIDER_BLOCKED" : "PLAYER_NOT_FOUND", status);
              }
              if (status >= 400) {
                throw new WhoScoredStatisticsException("PROVIDER_TRANSPORT_ERROR", status);
              }
              if (page.locator("#player-tournament-stats-options").count() != 1) {
                throw new WhoScoredStatisticsStructureChangedException();
              }
              phase[0] = "open-tab";
              phaseStarted = System.nanoTime();
              this.openTab(page, tab);
              LOG.info(
                  "{} fase=open-tab intento={} duraciónMs={}",
                  diagnostic,
                  attemptNumber,
                  elapsedMillis(phaseStarted));
              phase[0] = section.equals(INITIAL) ? "await-summary-table" : "await-detail-filter";
              phaseStarted = System.nanoTime();
              if (section.equals(INITIAL)) {
                this.awaitTable(page, container(section));
              } else {
                this.awaitSelector(page, "#statistics-filter-detailed #category");
              }
              LOG.info(
                  "{} fase={} intento={} duraciónMs={}",
                  diagnostic,
                  phase[0],
                  attemptNumber,
                  elapsedMillis(phaseStarted));
              phase[0] = "parse-document";
              phaseStarted = System.nanoTime();
              T result = reader.apply(page, response.text());
              LOG.info(
                  "{} fase=parse-document intento={} duraciónMs={}",
                  diagnostic,
                  attemptNumber,
                  elapsedMillis(phaseStarted));
              return result;
            } catch (RuntimeException failure) {
              LOG.warn(
                  "{} fase={} intento={} falló tras {} ms: {}",
                  diagnostic,
                  phase[0],
                  attemptNumber,
                  elapsedMillis(attemptStarted),
                  failure.getClass().getSimpleName());
              throw failure;
            }
          });
    } catch (WhoScoredStatisticsUnavailableException unavailable) {
      LOG.warn(
          "{} intento={} falló tras {} ms: {}",
          diagnostic,
          attemptNumber,
          elapsedMillis(attemptStarted),
          unavailable.getCode());
      throw unavailable;
    } catch (com.microsoft.playwright.TimeoutError timeout) {
      LOG.warn(
          "{} intento={} agotó timeout tras {} ms",
          diagnostic,
          attemptNumber,
          elapsedMillis(attemptStarted));
      throw new WhoScoredStatisticsUnavailableException("PROVIDER_TIMEOUT", null);
    } catch (PlaywrightException transport) {
      LOG.warn(
          "{} intento={} falló transporte tras {} ms",
          diagnostic,
          attemptNumber,
          elapsedMillis(attemptStarted));
      throw new WhoScoredStatisticsException(
          browser.isConnected() ? "PROVIDER_TRANSPORT_ERROR" : "HEADED_BROWSER_UNAVAILABLE", null);
    }
  }

  private static long elapsedMillis(long started) {
    return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
  }

  private static String diagnostic(Long runId, Long playerId, String section) {
    return "run="
        + (runId == null ? "n/a" : runId)
        + " player="
        + (playerId == null ? "n/a" : playerId)
        + " worker="
        + Thread.currentThread().getName()
        + " section="
        + section;
  }

  /**
   * Elige una categoría de detalle y espera su tabla en el modo nativo por partido.
   *
   * <p>Es la interacción que hace una persona sobre el desplegable. El proveedor sustituye el
   * contenido de la tabla mediante una petición propia. Se espera un renderizado posterior al
   * cambio, aunque publique los mismos valores. Si no llega se falla sin recarga ni retry.
   */
  public void chooseCategory(Page page, String category) {
    this.chooseCategory(page, category, "run=n/a player=n/a");
  }

  /** Conserva el recorrido y añade correlación a la operación concreta del filtro. */
  public void chooseCategory(Page page, String category, String diagnostic) {
    requireNativePerMatch(page);
    this.select(page, "#category", category, diagnostic);
    this.awaitTable(page, container(DETAILED));
    requireNativePerMatch(page);
  }

  /**
   * Un selector cambiado no acredita unidades distintas; no se interpreta otro modo como nativo.
   */
  private static void requireNativePerMatch(Page page) {
    Locator accumulation = page.locator("#statistics-filter-detailed #statsAccumulationType");
    if (accumulation.count() != 1 || !"0".equals(accumulation.inputValue())) {
      throw new WhoScoredStatisticsStructureChangedException();
    }
  }

  public static String container(String section) {
    return "#statistics-table-" + (section.equals(INITIAL) ? "summary" : "detailed");
  }

  /**
   * Espera la tabla con columnas técnicas, cuerpo e indicador de carga oculto.
   *
   * <p>Si expira, registra qué había realmente en el DOM: si el contenedor existe pero está vacío,
   * la sección no llegó a abrirse; si tiene tabla sin columnas, el proveedor cambió la forma.
   */
  public void awaitTable(Page page, String selector) {
    page.setDefaultTimeout(this.properties.navigationTimeoutMs());
    try {
      page.waitForFunction(
          """
          selector => {
            const loading = document.querySelector(selector + '-loading');
            return !!document.querySelector(selector + ' table thead [data-stat-name]')
              && !!document.querySelector(selector + ' table tbody')
              && (!loading || !loading.getClientRects().length
                  || getComputedStyle(loading).visibility === 'hidden');
          }
          """,
          selector,
          new Page.WaitForFunctionOptions().setTimeout(this.properties.navigationTimeoutMs()));
    } catch (com.microsoft.playwright.TimeoutError expired) {
      LOG.warn("Tabla no disponible en {}: {}", selector, state(page, selector));
      throw expired;
    }
  }

  /**
   * Activa una pestaña de estadísticas.
   *
   * <p>Espera el resumen renderizado y las pestañas en su estado inicial antes del único clic. No
   * usa la visibilidad transitoria del HTML sin inicializar como prueba de activación. El proveedor
   * carga la tabla mediante una petición propia y cada clic adicional cancela la que tiene en
   * marcha, de modo que insistir sobre la pestaña mientras carga deja el panel vacío. Si no abre se
   * falla sin otro clic.
   */
  void openTab(Page page, String tab) {
    page.waitForFunction(
        """
        () => {
          const visible = e => !!e && !!e.getClientRects().length
            && getComputedStyle(e).visibility !== 'hidden';
          const summary = document.querySelector('#player-tournament-stats-summary');
          const detail = document.querySelector('#player-tournament-stats-detailed');
          const selected = document.querySelector(
            '#player-tournament-stats-options a.selected[href="#player-tournament-stats-summary"]');
          return !!selected && visible(summary) && !!detail && !visible(detail)
            && !!summary.querySelector('table thead [data-stat-name]')
            && !!summary.querySelector('table tbody')
            && !visible(document.querySelector('#statistics-table-summary-loading'));
        }
        """,
        null,
        new Page.WaitForFunctionOptions()
            .setTimeout((double) this.properties.navigationTimeoutMs()));
    Locator link =
        page.locator(
                "#player-tournament-stats-options a[href='#player-tournament-stats-" + tab + "']")
            .first();
    if (link.count() != 1) {
      throw new WhoScoredStatisticsStructureChangedException();
    }
    try {
      link.scrollIntoViewIfNeeded(new Locator.ScrollIntoViewIfNeededOptions().setTimeout(2000));
    } catch (com.microsoft.playwright.TimeoutError offscreen) {
      // La barra de pestañas puede estar plegada: se sigue intentando el clic forzado.
    }
    link.click(new Locator.ClickOptions().setForce(true));
    if (!panelVisibleWithin(page, tab, CLICK_GRACE_MILLIS)) {
      throw new WhoScoredStatisticsUnavailableException("PROVIDER_TIMEOUT", null);
    }
  }

  /** Confirma selección del enlace y visibilidad CSS real del panel tras el clic. */
  private static boolean panelVisibleWithin(Page page, String tab, long milliseconds) {
    try {
      page.waitForFunction(
          """
          tab => {
            const panel = document.querySelector('#player-tournament-stats-' + tab);
            const link = document.querySelector(
              '#player-tournament-stats-options a[href="#player-tournament-stats-' + tab + '"]');
            return !!link?.classList.contains('selected') && !!panel
              && !!panel.getClientRects().length && getComputedStyle(panel).visibility !== 'hidden';
          }
          """,
          tab,
          new Page.WaitForFunctionOptions().setTimeout((double) milliseconds));
      return true;
    } catch (com.microsoft.playwright.TimeoutError notVisible) {
      return false;
    }
  }

  /**
   * Espera a que un control del filtro exista.
   *
   * <p>El filtro de la tabla de detalle se puebla junto con la tabla, pero no en el mismo instante:
   * consultarlo demasiado pronto da cero coincidencias y se confundía con un cambio de estructura.
   */
  private void awaitSelector(Page page, String selector) {
    page.setDefaultTimeout(this.properties.navigationTimeoutMs());
    try {
      page.waitForSelector(
          selector, new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));
    } catch (com.microsoft.playwright.TimeoutError expired) {
      LOG.warn("Control no disponible {}: {}", selector, state(page, container(DETAILED)));
      throw expired;
    }
  }

  /**
   * Cambia un filtro y espera la tabla recargada.
   *
   * <p>El observer se arma antes de emitir el cambio: reemplazar nodos por otros con HTML idéntico
   * también es un renderizado. No bastan el valor del selector, cambios de atributos ni quitar
   * filas. La tabla inicial se espera antes de observar para no aceptar su carga pendiente como
   * respuesta al filtro.
   */
  private void select(Page page, String selector, String value, String diagnostic) {
    page.setDefaultTimeout(this.properties.navigationTimeoutMs());
    Locator control = page.locator("#statistics-filter-detailed " + selector);
    if (control.count() != 1) {
      throw new WhoScoredStatisticsStructureChangedException();
    }
    if (!value.equals(control.inputValue())) {
      this.awaitTable(page, container(DETAILED));
      long started = System.nanoTime();
      String before = safeControlValue(control.inputValue());
      JSHandle render =
          page.evaluateHandle(
              """
              () => {
                const container = document.querySelector('#statistics-table-detailed');
                const state = { rendered: false };
                const dataRow = node => {
                  const row = node.nodeType === Node.ELEMENT_NODE
                    ? node.closest('tr') : node.parentElement?.closest('tr');
                  const columns = container.querySelectorAll('table thead th').length;
                  return columns > 1 && row?.querySelectorAll(':scope > td').length === columns;
                };
                state.observer = new MutationObserver(records => {
                  state.rendered ||= records.some(record =>
                    [...record.addedNodes].some(node =>
                      (node.nodeType === Node.ELEMENT_NODE
                        && (node.matches('table, tbody')
                            || !!node.querySelector('table, tbody')
                            || (node.matches('tr, td') && dataRow(node))))
                      || (node.nodeType === Node.TEXT_NODE
                          && !!node.parentElement?.closest('tbody td') && dataRow(node)))
                    || (record.type === 'characterData'
                        && !!record.target.parentElement?.closest('tbody td')
                        && dataRow(record.target)));
                });
                state.observer.observe(container, { childList: true, subtree: true,
                  characterData: true });
                return state;
              }
              """);
      try {
        control.selectOption(value);
        page.waitForFunction(
            """
            ([state, selector, value]) => {
              const container = document.querySelector('#statistics-table-detailed');
              const control = document.querySelector('#statistics-filter-detailed ' + selector);
              const loading = document.querySelector('#statistics-table-detailed-loading');
              return state.rendered && control?.value === value
                && !!container?.querySelector('table thead [data-stat-name]')
                && !!container?.querySelector('table tbody')
                && (!loading || !loading.getClientRects().length
                    || getComputedStyle(loading).visibility === 'hidden');
            }
            """,
            List.of(render, selector, value),
            new Page.WaitForFunctionOptions().setTimeout(this.properties.navigationTimeoutMs()));
        LOG.info(
            "{} filtro={} solicitado={} evento=filter-rendered duraciónMs={}",
            diagnostic,
            selector,
            value,
            elapsedMillis(started));
      } catch (com.microsoft.playwright.TimeoutError expired) {
        LOG.warn(
            "{} worker={} filtro={} anterior={} solicitado={} duraciónMs={} estado={}",
            diagnostic,
            Thread.currentThread().getName(),
            selector,
            before,
            value,
            elapsedMillis(started),
            state(page, container(DETAILED)));
        throw expired;
      } finally {
        releaseObserver(render, diagnostic);
      }
    }
  }

  /** La limpieza no oculta un fallo de selección o espera; el contexto también se cierra. */
  private static void releaseObserver(JSHandle render, String diagnostic) {
    try {
      try {
        render.evaluate("state => state.observer.disconnect()");
      } finally {
        render.dispose();
      }
    } catch (PlaywrightException cleanup) {
      LOG.warn("{} evento=filter-observer-cleanup-failed", diagnostic);
    }
  }

  private static String safeControlValue(String value) {
    return value != null && value.matches("[a-z0-9-]{1,40}") ? value : "unavailable";
  }

  /** Estado observable del contenedor, sin volcar el documento externo. */
  private static String state(Page page, String selector) {
    try {
      Locator container = page.locator(selector);
      int columns = container.locator("table thead [data-stat-name]").count();
      return "contenedor="
          + container.count()
          + " longitud="
          + (container.count() == 1 ? container.innerHTML().length() : -1)
          + " tablas="
          + container.locator("table").count()
          + " columnas="
          + columns
          + " categoria="
          + (page.locator("#statistics-filter-detailed #category").count() == 1)
          + " filtros="
          + page.evaluate(
              "() => ['category', 'statsAccumulationType'].map(id => {"
                  + " const c = document.querySelector('#statistics-filter-detailed #' + id);"
                  + " const v = c ? c.value : '';"
                  + " return id + ':' + (/^[a-z0-9-]{1,40}$/.test(v) ? v : 'unavailable');"
                  + " }).join(',')");
    } catch (RuntimeException unreadable) {
      return "estado ilegible";
    }
  }
}
