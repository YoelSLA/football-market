package footballmarket.integrations;

import static org.assertj.core.api.Assertions.*;

import com.microsoft.playwright.*;
import footballmarket.config.PlayerStatisticsProperties;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Regresión opt-in del DOM real contra HTML en memoria, sin acceso a WhoScored ni a la red. */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "FOOTBALL_MARKET_DOM_TESTS", matches = "true")
class WhoScoredScrapingDomTest {
  private static final String TABLE = "#statistics-table-detailed";
  private static final String HTML =
      """
      <!doctype html>
      <html><body>
        <div id="statistics-filter-detailed">
          <select id="category"><option value="aerial">Aéreo</option></select>
          <select id="statsAccumulationType">
            <option value="0">Por partido</option><option value="2">Total</option>
          </select>
        </div>
        <div id="statistics-table-detailed-loading" style="display:none">Cargando</div>
        <div id="statistics-table-detailed"><table>
          <thead><tr><th>Contexto</th><th data-stat-name="Apps">Apps</th></tr></thead>
          <tbody><tr><td>Liga local</td><td>6</td></tr></tbody>
        </table></div>
        <script>
          window.changes = 0;
          window.finalized = false;
        </script>
      </body></html>
      """;

  private Playwright playwright;
  private Browser browser;
  private BrowserContext context;
  private Page page;

  @BeforeEach
  void openLocalBrowser() {
    // Arrange: exige Chromium ya instalado; no descarga ni accede a proveedores.
    playwright =
        Playwright.create(
            new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    context = browser.newContext();
    context.route("**/*", Route::abort);
    page = context.newPage();
    page.setContent(HTML);
  }

  @AfterEach
  void closeLocalBrowser() {
    try {
      if (context != null) {
        context.close();
      }
    } finally {
      try {
        if (browser != null) {
          browser.close();
        }
      } finally {
        if (playwright != null) {
          playwright.close();
        }
      }
    }
  }

  private WhoScoredScrapingSectionNavigator navigator(int timeoutMillis) {
    PlayerStatisticsProperties properties =
        new PlayerStatisticsProperties(1, 1, 1, timeoutMillis, timeoutMillis, false, true);
    AtomicLong time = new AtomicLong();
    WhoScoredRequestPacer pacer =
        new WhoScoredRequestPacer(1, time::get, time::addAndGet, Clock.systemUTC());
    return new WhoScoredScrapingSectionNavigator(properties, pacer);
  }

  @Nested
  @DisplayName("Espera productiva sobre DOM de Chromium local")
  class FilterRendering {
    @Test
    @DisplayName("Acepta una tabla nueva con HTML idéntico sin exigir valores distintos")
    void acceptsIdenticalRender() {
      // Arrange
      WhoScoredScrapingSectionNavigator navigator = navigator(5000);
      String original = page.locator(TABLE).innerHTML();
      page.evaluate(
          """
          () => {
            const container = document.querySelector('#statistics-table-detailed');
            window.originalTable = container.querySelector('table');
            document.querySelector('#statsAccumulationType').onchange = () => {
              window.changes++;
              container.innerHTML = container.innerHTML;
            };
          }
          """);
      // Act
      navigator.chooseCategory(page, "aerial");
      // Assert
      assertThat(page.locator(TABLE).innerHTML()).isEqualTo(original);
      assertThat(
              page.evaluate(
                  "() => document.querySelector('#statistics-table-detailed table')"
                      + " !== window.originalTable"))
          .isEqualTo(true);
      assertThat(page.locator("#statsAccumulationType").inputValue()).isEqualTo("2");
      assertThat(((Number) page.evaluate("() => window.changes")).intValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("El selector y el spinner no permiten aceptar la tabla vieja sin render")
    void rejectsStaleTable() {
      // Arrange
      WhoScoredScrapingSectionNavigator navigator = navigator(1000);
      String original = page.locator(TABLE).innerHTML();
      page.evaluate(
          """
          () => {
            document.querySelector('#statsAccumulationType').onchange = () => {
              window.changes++;
              const loading = document.querySelector('#statistics-table-detailed-loading');
              loading.style.display = 'block';
              loading.textContent = 'Actualizando';
              loading.style.display = 'none';
            };
          }
          """);
      // Act / Assert
      assertThatThrownBy(() -> navigator.chooseCategory(page, "aerial"))
          .isInstanceOf(TimeoutError.class);
      // Verify
      assertThat(page.locator(TABLE).innerHTML()).isEqualTo(original);
      assertThat(page.locator("#statsAccumulationType").inputValue()).isEqualTo("2");
      assertThat(((Number) page.evaluate("() => window.changes")).intValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("Una tabla renderizada no se lee mientras la carga siga visible")
    void rejectsPendingLoading() {
      // Arrange
      WhoScoredScrapingSectionNavigator navigator = navigator(1000);
      page.evaluate(
          """
          () => {
            document.querySelector('#statsAccumulationType').onchange = () => {
              window.changes++;
              document.querySelector('#statistics-table-detailed-loading').style.display = 'block';
              const container = document.querySelector('#statistics-table-detailed');
              container.innerHTML = container.innerHTML;
              container.querySelector('tbody tr td:last-child').textContent = '12';
            };
          }
          """);
      // Act / Assert
      assertThatThrownBy(() -> navigator.chooseCategory(page, "aerial"))
          .isInstanceOf(TimeoutError.class);
      // Verify
      assertThat(page.locator(TABLE + " tbody tr td:last-child").innerText()).isEqualTo("12");
      assertThat(page.locator("#statistics-table-detailed-loading").isVisible()).isTrue();
      assertThat(((Number) page.evaluate("() => window.changes")).intValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("Espera la finalización diferida de carga y devuelve la tabla actualizada")
    void acceptsCompletedLoading() {
      // Arrange
      WhoScoredScrapingSectionNavigator navigator = navigator(5000);
      page.evaluate(
          """
          () => {
            document.querySelector('#statsAccumulationType').onchange = () => {
              window.changes++;
              const loading = document.querySelector('#statistics-table-detailed-loading');
              const container = document.querySelector('#statistics-table-detailed');
              loading.style.display = 'block';
              container.innerHTML = container.innerHTML;
              requestAnimationFrame(() => requestAnimationFrame(() => {
                container.querySelector('tbody tr td:last-child').textContent = '12';
                window.finalized = true;
                loading.style.display = 'none';
              }));
            };
          }
          """);
      // Act
      navigator.chooseCategory(page, "aerial");
      // Assert
      assertThat(page.evaluate("() => window.finalized")).isEqualTo(true);
      assertThat(page.locator(TABLE + " tbody tr td:last-child").innerText()).isEqualTo("12");
      assertThat(page.locator("#statistics-table-detailed-loading").isVisible()).isFalse();
      assertThat(((Number) page.evaluate("() => window.changes")).intValue()).isEqualTo(1);
    }
  }
}
