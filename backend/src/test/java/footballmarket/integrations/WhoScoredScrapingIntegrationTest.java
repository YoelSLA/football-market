package footballmarket.integrations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.microsoft.playwright.*;
import footballmarket.config.*;
import footballmarket.integrations.exceptions.*;
import footballmarket.integrations.records.*;
import footballmarket.support.WhoScoredStatisticsPageStub;
import java.time.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Recorrido headed del scraping sobre una sola carga del resumen y otra de la tabla de detalle.
 *
 * <p>La tabla de detalle se puebla por categoría, así que el doble responde a la categoría
 * seleccionada: un recorrido que leyera siempre la misma tabla no probaría que el desplegable se
 * usa como lo usa una persona.
 */
@org.junit.jupiter.api.Tag("lightweight")
class WhoScoredScrapingIntegrationTest {
  private static final String HTML =
      """
      <script>tournaments: [{"TournamentId":5,"SeasonId":11126,"TeamId":3,
      "Rating":6.9033333333333333,"ShotsOnTarget":0,"SecondYellow":0,"GameStarted":6,"SubOn":0}]</script>
      """;
  private static final Map<String, String> INITIAL_FIELDS =
      Map.of(
          "initial",
          "minsPlayed apps goal assistTotal shotsPerGame passSuccess aerialWonPerGame yellowCard"
              + " redCard");
  private static final Map<String, String> CATEGORY_FIELDS =
      Map.ofEntries(
          Map.entry("aerial", "duelAerialLost duelAerialTotal"),
          Map.entry(
              "assists",
              "assistCross assistCorner assistThroughball assistFreekick assistThrowin"
                  + " assistOther"),
          Map.entry("blocks", "passCrossBlockedDefensive outfielderBlockedPass"),
          Map.entry("cards", "yellowCard redCard"),
          Map.entry("dribbles", "dribbleLost dribbleTotal"),
          Map.entry("fouls", "foulCommitted"),
          Map.entry("goals", "goalSixYardBox goalPenaltyArea goalObox"),
          Map.entry("key-passes", "keyPassLong keyPassShort"),
          Map.entry("passes", "passLongBallInaccurate shortPassAccurate shortPassInaccurate"),
          Map.entry("saves", "saveTotal saveSixYardBox savePenaltyArea saveObox"),
          Map.entry("shots", "shotSixYardBox shotPenaltyArea shotOboxTotal"),
          Map.entry("tackles", "tackleTotalAttempted"));

  /** Categoría que el recorrido considera seleccionada en cada momento. */
  private final AtomicReference<String> selected = new AtomicReference<>("shots");

  private void configureSummary(Page page) {
    Locator tabs = mock(Locator.class);
    when(tabs.count()).thenReturn(1);
    when(page.locator("#player-tournament-stats-options")).thenReturn(tabs);
    Locator tab = mock(Locator.class);
    when(page.locator(startsWith("#player-tournament-stats-options a"))).thenReturn(tab);
    when(tab.count()).thenReturn(1);
    when(tab.first()).thenReturn(tab);
    // El recorrido localiza la tabla dentro del contenedor de la sección.
    table(page, "#statistics-table-summary table", INITIAL_FIELDS.get("initial"));
  }

  /** Prepara el panel de detalle: el doble de tabla cambia con la categoría seleccionada. */
  private JSHandle configureDetail(Page page) {
    Locator tabs = mock(Locator.class);
    when(tabs.count()).thenReturn(1);
    when(page.locator("#player-tournament-stats-options")).thenReturn(tabs);
    Locator tab = mock(Locator.class);
    when(page.locator(startsWith("#player-tournament-stats-options a"))).thenReturn(tab);
    when(tab.count()).thenReturn(1);
    when(tab.first()).thenReturn(tab);
    Locator category = mock(Locator.class);
    when(category.count()).thenReturn(1);
    when(category.inputValue()).thenAnswer(invocation -> selected.get());
    doAnswer(
            invocation -> {
              selected.set(invocation.getArgument(0));
              return null;
            })
        .when(category)
        .selectOption(anyString());
    Locator accumulation = mock(Locator.class);
    when(accumulation.count()).thenReturn(1);
    when(accumulation.inputValue()).thenReturn("2");
    when(page.locator("#statistics-filter-detailed #category")).thenReturn(category);
    when(page.locator("#statistics-filter-detailed #statsAccumulationType"))
        .thenReturn(accumulation);
    Locator container = mock(Locator.class);
    when(container.count()).thenReturn(1);
    when(container.innerHTML()).thenReturn("tabla-" + selected.get());
    when(page.locator("#statistics-table-detailed")).thenReturn(container);
    JSHandle render = mock(JSHandle.class);
    when(page.evaluateHandle(anyString())).thenReturn(render);
    table(page, "#statistics-table-detailed table", null);
    return render;
  }

  /**
   * Tabla cuyas columnas dependen del valor indicado, o de la categoría seleccionada si es nulo.
   */
  private void table(Page page, String selector, String fixed) {
    Locator table = mock(Locator.class);
    when(table.count()).thenReturn(1);
    when(page.locator(selector)).thenReturn(table);
    when(table.locator("thead th")).thenAnswer(invocation -> headers(columns(fixed)));
    when(table.locator("tbody tr")).thenAnswer(invocation -> body(page, columns(fixed)));
  }

  /** Columnas de la tabla que el recorrido tiene delante en este momento. */
  private String columns(String fixed) {
    return fixed != null ? fixed : CATEGORY_FIELDS.getOrDefault(selected.get(), "");
  }

  private List<String> fields(String columns) {
    List<String> fields = new ArrayList<>();
    fields.add("");
    fields.add("Apps");
    for (String field : columns.split(" ")) {
      if (!field.isBlank() && !field.equalsIgnoreCase("apps")) {
        fields.add(field);
      }
    }
    return fields;
  }

  private Locator headers(String columns) {
    List<String> fields = fields(columns);
    Locator headers = mock(Locator.class);
    when(headers.count()).thenReturn(fields.size());
    for (int index = 0; index < fields.size(); index++) {
      Locator header = mock(Locator.class);
      when(header.getAttribute("data-stat-name")).thenReturn(fields.get(index));
      when(headers.nth(index)).thenReturn(header);
    }
    return headers;
  }

  private Locator body(Page page, String columns) {
    List<String> fields = fields(columns);
    Locator row = mock(Locator.class);
    Locator cells = mock(Locator.class);
    Locator body = mock(Locator.class);
    when(body.all()).thenReturn(List.of(row));
    when(row.locator(":scope > td")).thenReturn(cells);
    // Los enlaces se crean antes del when: anidar stubs dentro de uno abierto rompe Mockito.
    Locator links = linksOf(competition(), team());
    when(row.locator("a")).thenReturn(links);
    when(cells.count()).thenReturn(fields.size());
    for (int index = 0; index < fields.size(); index++) {
      Locator cell = mock(Locator.class);
      when(cell.innerText())
          .thenReturn(fields.get(index).equals("Apps") ? "6" : index == 0 ? "Liga A" : "3");
      when(cells.nth(index)).thenReturn(cell);
    }
    return body;
  }

  private Locator competition() {
    Locator link = mock(Locator.class);
    when(link.getAttribute("href")).thenReturn("/regions/1/tournaments/5/league");
    return link;
  }

  private Locator team() {
    Locator link = mock(Locator.class);
    when(link.getAttribute("href")).thenReturn("/teams/3/show/team");
    return link;
  }

  /** Enlaces de la fila: los usa el lector para extraer competición y equipo. */
  private Locator linksOf(Locator... items) {
    Locator links = mock(Locator.class);
    when(links.all()).thenReturn(List.of(items));
    return links;
  }

  private WhoScoredStatisticsScrapingIntegration integration(
      WhoScoredRequestPacer pacer, boolean filter) {
    WhoScoredProperties provider = mock(WhoScoredProperties.class);
    when(provider.baseUrl()).thenReturn("https://provider.example");
    PlayerStatisticsProperties properties =
        new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, filter, true);
    WhoScoredScrapingSectionNavigator navigator =
        new WhoScoredScrapingSectionNavigator(properties, pacer);
    return new WhoScoredStatisticsScrapingIntegration(
        provider,
        properties,
        navigator,
        new WhoScoredScrapingTableParser(),
        new WhoScoredStructuredDataParser(),
        pacer);
  }

  private WhoScoredRequestPacer pacer(AtomicLong time) {
    return new WhoScoredRequestPacer(2500, time::get, time::addAndGet, Clock.systemUTC());
  }

  @Nested
  @DisplayName("Recorrido headed en dos cargas")
  class Scraping {
    @Test
    @DisplayName("Una categoría que agota su espera no se recarga ni cambia a otra categoría")
    void doesNotRetryCategory() {
      // Arrange
      Page page = mock(Page.class);
      JSHandle render = configureDetail(page);
      AtomicLong time = new AtomicLong();
      WhoScoredRequestPacer pacer = pacer(time);
      PlayerStatisticsProperties properties =
          new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, false, true);
      WhoScoredScrapingSectionNavigator navigator =
          new WhoScoredScrapingSectionNavigator(properties, pacer);
      when(page.waitForFunction(anyString(), any(), any(Page.WaitForFunctionOptions.class)))
          .thenReturn(null)
          .thenThrow(new TimeoutError("recarga controlada"));
      // Act / Assert
      assertThatThrownBy(() -> navigator.chooseCategory(page, "aerial"))
          .isInstanceOf(TimeoutError.class);
      // Verify
      verify(page.locator("#statistics-filter-detailed #category")).selectOption("aerial");
      verify(page, never()).reload();
      verify(render).evaluate("state => state.observer.disconnect()");
      verify(render).dispose();
    }

    @Test
    @DisplayName("Arma el observer antes del acumulado y conserva el timeout sin comparar HTML")
    void waitsForAccumulationRender() {
      // Arrange
      Page page = mock(Page.class);
      configureDetail(page);
      selected.set("aerial");
      Locator accumulation = page.locator("#statistics-filter-detailed #statsAccumulationType");
      when(accumulation.inputValue()).thenReturn("0");
      JSHandle render = mock(JSHandle.class);
      when(page.evaluateHandle(anyString())).thenReturn(render);
      AtomicLong time = new AtomicLong();
      PlayerStatisticsProperties properties =
          new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, false, true);
      WhoScoredScrapingSectionNavigator navigator =
          new WhoScoredScrapingSectionNavigator(properties, pacer(time));
      // Act
      navigator.chooseCategory(page, "aerial");
      // Assert / Verify
      org.mockito.InOrder order = inOrder(page, accumulation, render);
      order.verify(page).evaluateHandle(contains("MutationObserver"));
      order.verify(accumulation).selectOption("2");
      ArgumentCaptor<Page.WaitForFunctionOptions> options =
          ArgumentCaptor.forClass(Page.WaitForFunctionOptions.class);
      order
          .verify(page)
          .waitForFunction(
              contains("state.rendered"),
              eq(List.of(render, "#statsAccumulationType", "2")),
              options.capture());
      assertThat(options.getValue().timeout).isEqualTo(20000);
      order.verify(render).evaluate("state => state.observer.disconnect()");
      order.verify(render).dispose();
      verify(accumulation).selectOption("2");
      verify(page, never()).reload();
    }

    @Test
    @DisplayName("No vuelve a seleccionar filtros que ya tienen el valor solicitado")
    void doesNotSelectUnchangedFilters() {
      // Arrange
      Page page = mock(Page.class);
      configureDetail(page);
      selected.set("aerial");
      AtomicLong time = new AtomicLong();
      PlayerStatisticsProperties properties =
          new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, false, true);
      WhoScoredScrapingSectionNavigator navigator =
          new WhoScoredScrapingSectionNavigator(properties, pacer(time));
      // Act
      navigator.chooseCategory(page, "aerial");
      // Verify
      verify(page.locator("#statistics-filter-detailed #category"), never())
          .selectOption(anyString());
      verify(page.locator("#statistics-filter-detailed #statsAccumulationType"), never())
          .selectOption(anyString());
      verify(page, never()).evaluateHandle(anyString());
    }

    @Test
    @DisplayName("Libera el observer ante fallo de selección sin ocultar el error original")
    void preservesSelectionFailure() {
      // Arrange
      Page page = mock(Page.class);
      JSHandle render = configureDetail(page);
      Locator category = page.locator("#statistics-filter-detailed #category");
      TimeoutError failure = new TimeoutError("selección controlada");
      doThrow(failure).when(category).selectOption("aerial");
      when(render.evaluate(anyString())).thenThrow(new PlaywrightException("cleanup controlado"));
      AtomicLong time = new AtomicLong();
      PlayerStatisticsProperties properties =
          new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, false, true);
      WhoScoredScrapingSectionNavigator navigator =
          new WhoScoredScrapingSectionNavigator(properties, pacer(time));
      // Act / Assert
      assertThatThrownBy(() -> navigator.chooseCategory(page, "aerial")).isSameAs(failure);
      // Verify
      verify(render).dispose();
      verify(category).selectOption("aerial");
      verify(page, never()).reload();
    }

    @Test
    @DisplayName("Un timeout falla sin abrir otro contexto ni reintentar la navegación")
    void doesNotRetryTimeout() {
      // Arrange
      WhoScoredStatisticsPageStub stub = new WhoScoredStatisticsPageStub();
      Page failed = stub.enqueue(HTML, 200);
      when(failed.navigate(anyString(), any(Page.NavigateOptions.class)))
          .thenThrow(new TimeoutError("timeout controlado"));
      AtomicLong time = new AtomicLong();
      WhoScoredRequestPacer pacer = pacer(time);
      PlayerStatisticsProperties properties =
          new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, false, true);
      WhoScoredScrapingSectionNavigator navigator =
          new WhoScoredScrapingSectionNavigator(properties, pacer);
      // Act
      assertThatThrownBy(
              () ->
                  navigator.read(
                      stub.browser(),
                      "https://provider.example/players/123/show",
                      "initial",
                      pacer.statisticsWorker(2500),
                      () -> {},
                      (page, original) -> original))
          .isInstanceOfSatisfying(
              WhoScoredStatisticsException.class,
              failure -> assertThat(failure.getCode()).isEqualTo("PROVIDER_TIMEOUT"));
      // Assert
      assertThat(time.get()).isZero();
      // Verify
      verify(stub.browser()).newContext();
      for (BrowserContext context : stub.contexts()) {
        verify(context).close();
      }
    }

    @Test
    @DisplayName(
        "Workers distintos no comparten intervalo y un deadline vencido no envía solicitudes")
    void isolatesWorkerPacingAndDeadline() {
      // Arrange
      AtomicLong time = new AtomicLong();
      Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
      WhoScoredRequestPacer pacer =
          new WhoScoredRequestPacer(2500, time::get, time::addAndGet, clock);
      WhoScoredRequestPacer.StatisticsWorker first = pacer.statisticsWorker(2500);
      WhoScoredRequestPacer.StatisticsWorker second = pacer.statisticsWorker(2500);
      // Act
      long firstStart = pacer.executeStatistics(first, null, time::get);
      long secondStart = pacer.executeStatistics(second, null, time::get);
      long nextFirst = pacer.executeStatistics(first, null, time::get);
      // Assert
      assertThat(firstStart).isZero();
      assertThat(secondStart).isZero();
      assertThat(nextFirst).isEqualTo(2500);
      assertThatThrownBy(
              () -> pacer.executeStatistics(second, Instant.EPOCH, () -> "no debe enviarse"))
          .isInstanceOfSatisfying(
              WhoScoredStatisticsException.class,
              failure -> assertThat(failure.getCode()).isEqualTo("EXTRACTION_INTERRUPTED"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Lee el resumen y las dieciséis categorías con dos cargas del documento")
    void walksSummaryAndCategories(boolean filter) {
      // Arrange
      WhoScoredStatisticsPageStub stub = new WhoScoredStatisticsPageStub();
      Page summary = stub.enqueue(HTML, 200);
      configureSummary(summary);
      Page detail = stub.enqueue(HTML, 200);
      configureDetail(detail);
      AtomicLong time = new AtomicLong();
      WhoScoredRequestPacer pacer = pacer(time);
      WhoScoredStatisticsScrapingIntegration integration = integration(pacer, filter);
      Browser probe = mock(Browser.class);
      Playwright playwright = mock(Playwright.class);
      BrowserType chromium = mock(BrowserType.class);
      when(playwright.chromium()).thenReturn(chromium);
      when(chromium.launch(any(BrowserType.LaunchOptions.class))).thenReturn(probe, stub.browser());
      try (MockedStatic<Playwright> factory = mockStatic(Playwright.class)) {
        factory
            .when(() -> Playwright.create(any(Playwright.CreateOptions.class)))
            .thenReturn(playwright);
        // Act
        WhoScoredScrapedPlayer result;
        try (WhoScoredStatisticsScrapingIntegration.Session session = integration.openSession()) {
          result = integration.scrape(session, "123");
        }
        // Assert
        assertThat(result.rows()).hasSize(17);
        assertThat(result.rows().getFirst().section()).isEqualTo("initial");
        assertThat(result.rows().stream().skip(1).map(WhoScoredScrapedContextRow::section))
            .containsExactlyElementsOf(WhoScoredScrapingSectionNavigator.CATEGORIES);
        assertThat(result.providerCalls()).isEqualTo(2);
        assertThat(result.rows()).allSatisfy(row -> assertThat(row.seasonKey()).isEqualTo("11126"));
        assertThat(result.structured().entries().getFirst().rating().scale()).isEqualTo(16);
        // Verify
        ArgumentCaptor<BrowserType.LaunchOptions> launch =
            ArgumentCaptor.forClass(BrowserType.LaunchOptions.class);
        verify(chromium, times(2)).launch(launch.capture());
        assertThat(launch.getAllValues())
            .allSatisfy(options -> assertThat(options.headless).isFalse());
        verify(probe, never()).newContext();
        verify(probe).close();
        verify(stub.browser()).close();
        verify(playwright, times(2)).close();
        verify(stub.browser(), times(2)).newContext();
        for (BrowserContext context : stub.contexts()) {
          verify(context).close();
        }
        if (filter) {
          verify(summary).route(anyString(), any());
          verify(detail).route(anyString(), any());
        } else {
          verify(summary, never()).route(anyString(), any());
          verify(detail, never()).route(anyString(), any());
        }
      }
    }
  }

  @Nested
  @DisplayName("Rechazos del proveedor")
  class Rejections {
    @ParameterizedTest
    // El 429 respeta Retry-After y abandona el jugador como fallo de red, no como timeout.
    @CsvSource({"403,PROVIDER_BLOCKED", "404,PLAYER_NOT_FOUND", "429,PROVIDER_TRANSPORT_ERROR"})
    @DisplayName("No reintenta rechazos HTTP y libera el contexto fallido")
    void doesNotRetryBlockedPlayer(int status, String code) {
      // Arrange
      WhoScoredStatisticsPageStub stub = new WhoScoredStatisticsPageStub();
      stub.enqueue("blocked", status);
      AtomicLong time = new AtomicLong();
      WhoScoredRequestPacer pacer = pacer(time);
      PlayerStatisticsProperties properties =
          new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, false, true);
      WhoScoredScrapingSectionNavigator navigator =
          new WhoScoredScrapingSectionNavigator(properties, pacer);
      // Act / Assert
      assertThatThrownBy(
              () ->
                  navigator.read(
                      stub.browser(),
                      "https://provider.example/players/123/show",
                      "initial",
                      pacer.statisticsWorker(2500),
                      () -> {},
                      (page, html) -> html))
          .isInstanceOfSatisfying(
              WhoScoredStatisticsException.class,
              failure -> assertThat(failure.getCode()).isEqualTo(code));
      // Verify
      verify(stub.browser()).newContext();
      verify(stub.contexts().getFirst()).close();
    }
  }
}
