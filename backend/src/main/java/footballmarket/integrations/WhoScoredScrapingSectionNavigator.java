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

  /**
   * Margen para considerar que la sección se abrió. Cubre tanto el retraso con que el proveedor
   * engancha su manejador de pestañas como el de la carga de la tabla.
   */
  private static final long CLICK_GRACE_MILLIS = 8_000L;

  /**
   * Intentos de recarga por categoría.
   *
   * <p>El proveedor degrada su tabla de detalle en ventanas de minutos durante las que una recarga
   * puede tardar más que el presupuesto de lectura. Descartar al jugador entero por una de esas
   * ventanas pierde métricas ya disponibles del resto de categorías.
   */
  private static final int CATEGORY_ATTEMPTS = 3;

  /** Segunda oportunidad de carga de la tabla de detalle. */
  private static final int DETAIL_ATTEMPTS = 2;

  private static final long DETAIL_WAIT_MS = 45_000L;

  private final PlayerStatisticsProperties properties;
  private final WhoScoredRequestPacer pacer;

  public WhoScoredScrapingSectionNavigator(
      PlayerStatisticsProperties properties, WhoScoredRequestPacer pacer) {
    this.properties = properties;
    this.pacer = pacer;
  }

  /**
   * El callback lee el DOM antes de cerrar el contexto; solo transporte y timeout admiten un retry.
   *
   * <p>Las pestañas no se abren por la URL: el hash deja la página en su estado inicial. La sección
   * se activa con un clic sobre su pestaña, que es lo que dispara la carga perezosa. En la tabla de
   * detalle la carga la completa el filtro de categorías, que es lo primero que se espera.
   */
  public <T> T read(
      Browser browser,
      String url,
      String section,
      Instant deadline,
      WhoScoredRequestPacer.StatisticsWorker worker,
      Runnable navigationStarted,
      java.util.function.BiFunction<Page, String, T> reader) {
    String tab = section.equals(INITIAL) ? "summary" : "detailed";
    for (int attempt = 1; attempt <= 2; attempt++) {
      try {
        synchronized (browser) {
          return this.pacer.executeStatistics(
              worker,
              deadline,
              () -> {
                try (BrowserContext context = browser.newContext()) {
                  Page page = context.newPage();
                  page.setDefaultTimeout(this.timeout(deadline));
                  page.setDefaultNavigationTimeout(this.timeout(deadline));
                  navigationStarted.run();
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
                                      domain ->
                                          host.equals(domain) || host.endsWith("." + domain))) {
                            route.abort();
                          } else {
                            route.resume();
                          }
                        });
                  }
                  Response response =
                      page.navigate(
                          url,
                          new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                  if (response == null) {
                    throw new WhoScoredStatisticsUnavailableException(
                        "PROVIDER_TRANSPORT_ERROR", null);
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
                  this.openTab(page, tab);
                  if (section.equals(INITIAL)) {
                    this.awaitTable(page, container(section), deadline);
                  } else {
                    this.awaitDetailFilter(page, deadline);
                  }
                  if (deadline != null && !Instant.now().isBefore(deadline)) {
                    throw new WhoScoredStatisticsException("EXTRACTION_INTERRUPTED", null);
                  }
                  return reader.apply(page, response.text());
                }
              });
        }
      } catch (WhoScoredStatisticsUnavailableException unavailable) {
        if (attempt == 2 || unavailable.getProviderStatus() != null) {
          throw unavailable;
        }
      } catch (com.microsoft.playwright.TimeoutError timeout) {
        if (attempt == 2) {
          throw new WhoScoredStatisticsUnavailableException("PROVIDER_TIMEOUT", null);
        }
      } catch (PlaywrightException transport) {
        if (attempt == 2) {
          throw new WhoScoredStatisticsUnavailableException("PROVIDER_TRANSPORT_ERROR", null);
        }
      }
    }
    throw new WhoScoredStatisticsUnavailableException("PROVIDER_TRANSPORT_ERROR", null);
  }

  /**
   * Elige una categoría de la tabla de detalle y el modo de acumulados, y espera su tabla.
   *
   * <p>Es la interacción que hace una persona sobre el desplegable. El proveedor sustituye el
   * contenido de la tabla mediante una petición propia, así que la lectura solo es válida cuando el
   * contenido renderizado ha cambiado. Cuando esa recarga no llega, la sección completa no se
   * pierde: se vuelve a elegir otra categoría y luego la pedida, porque un valor que ya estaba
   * seleccionado no genera un cambio nuevo que el proveedor pueda atender.
   */
  public void chooseCategory(Page page, String category, Instant deadline) {
    for (int attempt = 1; ; attempt++) {
      try {
        this.select(page, "#category", category, deadline);
        this.select(page, "#statsAccumulationType", "2", deadline);
        this.awaitTable(page, container(DETAILED), deadline);
        return;
      } catch (com.microsoft.playwright.TimeoutError slow) {
        if (attempt >= CATEGORY_ATTEMPTS) {
          throw slow;
        }
        LOG.warn("Categoría {} sin recargar en el intento {}; se reintenta", category, attempt);
        this.select(page, "#category", otherCategory(category), deadline);
      }
    }
  }

  /** Una categoría distinta de la pedida, para forzar un cambio real en el desplegable. */
  private static String otherCategory(String category) {
    for (String candidate : CATEGORIES) {
      if (!candidate.equals(category)) {
        return candidate;
      }
    }
    throw new WhoScoredStatisticsStructureChangedException();
  }

  public static String container(String section) {
    return "#statistics-table-" + (section.equals(INITIAL) ? "summary" : "detailed");
  }

  /**
   * Espera a que la tabla de la sección exista con sus columnas técnicas.
   *
   * <p>Si expira, registra qué había realmente en el DOM: si el contenedor existe pero está vacío,
   * la sección no llegó a abrirse; si tiene tabla sin columnas, el proveedor cambió la forma.
   */
  public void awaitTable(Page page, String selector, Instant deadline) {
    page.setDefaultTimeout(this.timeout(deadline));
    try {
      page.waitForFunction(
          "selector => !!document.querySelector(selector + ' table thead [data-stat-name]')",
          selector,
          new Page.WaitForFunctionOptions().setTimeout(this.timeout(deadline)));
    } catch (com.microsoft.playwright.TimeoutError expired) {
      LOG.warn("Tabla no disponible en {}: {}", selector, state(page, selector));
      throw expired;
    }
  }

  /**
   * Activa una pestaña de estadísticas.
   *
   * <p>El clic se emite una sola vez y se le concede margen. El proveedor carga la tabla mediante
   * una petición propia y cada clic adicional cancela la que tiene en marcha, de modo que insistir
   * sobre la pestaña mientras carga deja el panel vacío. Solo si tras ese margen la sección sigue
   * sin abrir, que es el caso en que el clic se perdió, se emite un único evento dirigido al
   * elemento.
   */
  private void openTab(Page page, String tab) {
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
      LOG.info("Pestaña {} sin abrir tras el clic; se emite el evento dirigido", tab);
      link.dispatchEvent("click");
    }
  }

  /** Si el panel de la sección sigue oculto tras el clic. */
  private static boolean panelVisibleWithin(Page page, String tab, long milliseconds) {
    try {
      page.waitForFunction(
          "tab => { const d = document.querySelector('#player-tournament-stats-' + tab);"
              + " return !!d && d.style.display !== 'none'; }",
          tab,
          new Page.WaitForFunctionOptions().setTimeout((double) milliseconds));
      return true;
    } catch (com.microsoft.playwright.TimeoutError notVisible) {
      return false;
    }
  }

  /**
   * *Espera a que la tabla de detalle esté disponible, con una segunda oportunidad acotada.*
   *
   * <p>El proveedor abre el panel y en ventanas de degradación su carga no llega. Como el panel ya
   * está visible, repetir el clic durante la carga cancelaría la petición en marcha, así que la
   * segunda oportunidad se concede solo tras agotar la espera completa, cuando ya no hay nada en
   * vuelo que cancelar, y se emite el evento dirigido al elemento para que la carga vuelva a
   * dispararse.
   */
  private void awaitDetailFilter(Page page, Instant deadline) {
    for (int attempt = 1; ; attempt++) {
      try {
        this.awaitSelector(page, "#statistics-filter-detailed #category", deadline);
        return;
      } catch (com.microsoft.playwright.TimeoutError notLoaded) {
        if (attempt >= DETAIL_ATTEMPTS) {
          throw notLoaded;
        }
        LOG.warn(
            "Tabla de detalle sin contenido tras {} ms; se reintenta su carga", DETAIL_WAIT_MS);
        page.locator("#player-tournament-stats-options a[href='#player-tournament-stats-detailed']")
            .first()
            .dispatchEvent("click");
      }
    }
  }

  /**
   * Espera a que un control del filtro exista.
   *
   * <p>El filtro de la tabla de detalle se puebla junto con la tabla, pero no en el mismo instante:
   * consultarlo demasiado pronto da cero coincidencias y se confundía con un cambio de estructura.
   */
  private void awaitSelector(Page page, String selector, Instant deadline) {
    page.setDefaultTimeout(this.timeout(deadline));
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
   * <p>El proveedor sustituye el contenido de la tabla mediante una petición propia y su atributo
   * {@code data-fwsc} no cambia siempre, así que la espera se apoya en el contenido renderizado.
   */
  private void select(Page page, String selector, String value, Instant deadline) {
    page.setDefaultTimeout(this.timeout(deadline));
    Locator control = page.locator("#statistics-filter-detailed " + selector);
    if (control.count() != 1) {
      throw new WhoScoredStatisticsStructureChangedException();
    }
    if (!value.equals(control.inputValue())) {
      String previous = page.locator(container(DETAILED)).innerHTML();
      control.selectOption(value);
      page.waitForFunction(
          "previous => { const t = document.querySelector('#statistics-table-detailed');"
              + " return !!t && t.innerHTML !== previous"
              + " && !!t.querySelector('table thead [data-stat-name]'); }",
          previous,
          new Page.WaitForFunctionOptions().setTimeout(this.timeout(deadline)));
    }
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
          + (page.locator("#statistics-filter-detailed #category").count() == 1);
    } catch (RuntimeException unreadable) {
      return "estado ilegible";
    }
  }

  private double timeout(Instant deadline) {
    long remaining =
        deadline == null
            ? this.properties.navigationTimeoutMs()
            : Duration.between(Instant.now(), deadline).toMillis();
    if (remaining <= 0) {
      throw new WhoScoredStatisticsException("EXTRACTION_INTERRUPTED", null);
    }
    return Math.max(1, Math.min(this.properties.navigationTimeoutMs(), remaining));
  }
}
