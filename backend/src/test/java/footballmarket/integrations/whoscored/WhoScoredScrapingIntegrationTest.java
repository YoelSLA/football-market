package footballmarket.integrations.whoscored;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.microsoft.playwright.*;
import footballmarket.features.statistics.infraestructure.config.PlayerStatisticsProperties;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsStructureChangedException;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsUnavailableException;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedContextRow;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedPlayer;
import footballmarket.integrations.whoscored.records.WhoScoredStructuredDataBlock;
import footballmarket.support.WhoScoredStatisticsPageStub;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Recorrido headed del scraping sobre una carga del resumen y otra de la tabla de detalle.
 *
 * <p>Verifica la coordinación del navegador, la conmutación de pestañas y categorías, y el
 * comportamiento ante rechazos o timeouts sin acoplarse a detalles de implementación del DOM
 * interno.
 */
@org.junit.jupiter.api.Tag("lightweight")
class WhoScoredScrapingIntegrationTest {
  private static final String HTML =
      """
      <script>tournaments: [{"TournamentId":5,"SeasonId":11126,"TeamId":3,
      "Rating":6.9033333333333333,"ShotsOnTarget":0,"SecondYellow":0,"GameStarted":6,"SubOn":0}]</script>
      """;

  @Test
  @DisplayName("Inicializa pestañas antes del único clic y conserva los timeouts de operación")
  void waitsForInitializationBeforeClick() {
    Page page = mock(Page.class);
    configureDetailPage(page);
    WhoScoredScrapingSectionNavigator navigator =
        new WhoScoredScrapingSectionNavigator(
            new PlayerStatisticsProperties(1, 1, 1500, 45000, 20000, false, true),
            pacer(new AtomicLong()));

    navigator.openTab(page, "detailed");

    Locator link =
        page.locator("#player-tournament-stats-options a[href='#player-tournament-stats-detailed']")
            .first();

    verify(link).click(any(Locator.ClickOptions.class));

    ArgumentCaptor<Page.WaitForFunctionOptions> optionsCaptor =
        ArgumentCaptor.forClass(Page.WaitForFunctionOptions.class);
    verify(page, times(2)).waitForFunction(anyString(), any(), optionsCaptor.capture());

    List<Page.WaitForFunctionOptions> captured = optionsCaptor.getAllValues();
    assertThat(captured.get(0).timeout).isEqualTo(45000);
    assertThat(captured.get(1).timeout).isEqualTo(8000);
  }

  @Test
  @DisplayName("Falla con error de estructura si la pestaña no existe")
  void rejectsMissingTab() {
    Page page = mock(Page.class);
    Locator link = mock(Locator.class);
    when(link.count()).thenReturn(0);
    when(page.locator(startsWith("#player-tournament-stats-options a"))).thenReturn(link);
    when(link.first()).thenReturn(link);

    WhoScoredScrapingSectionNavigator navigator =
        new WhoScoredScrapingSectionNavigator(
            new PlayerStatisticsProperties(1, 1, 1500, 45000, 20000, false, true),
            pacer(new AtomicLong()));

    assertThatThrownBy(() -> navigator.openTab(page, "detailed"))
        .isInstanceOf(WhoScoredStatisticsStructureChangedException.class);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @DisplayName("Cierra el contexto tras fallo inicial sin parsear ni reintentar")
  void closesContextOnInitializationFailure(boolean tabFailure) {
    WhoScoredStatisticsPageStub stub = new WhoScoredStatisticsPageStub();
    Page page = stub.enqueue(HTML, 200);
    configureDetailPage(page);
    if (tabFailure) {
      doThrow(new TimeoutError("synthetic"))
          .when(page)
          .waitForFunction(anyString(), any(), any(Page.WaitForFunctionOptions.class));
    } else {
      doThrow(new TimeoutError("synthetic"))
          .when(page)
          .waitForSelector(anyString(), any(Page.WaitForSelectorOptions.class));
    }
    WhoScoredRequestPacer pacer = pacer(new AtomicLong());
    WhoScoredScrapingSectionNavigator navigator =
        new WhoScoredScrapingSectionNavigator(
            new PlayerStatisticsProperties(4, 4, 2500, 45000, 45000, false, true), pacer);
    Runnable parsed = mock(Runnable.class);

    assertThatThrownBy(
            () ->
                navigator.read(
                    stub.browser(),
                    "https://www.whoscored.com/players/1/show",
                    "detailed",
                    null,
                    7L,
                    pacer.statisticsWorker(1500),
                    () -> {},
                    (p, html) -> {
                      parsed.run();
                      return null;
                    }))
        .isInstanceOf(WhoScoredStatisticsUnavailableException.class);

    var order = inOrder(page, stub.contexts().getFirst());
    order.verify(page).navigate(anyString(), any(Page.NavigateOptions.class));
    order.verify(stub.contexts().getFirst()).close();
    verify(page, times(1)).navigate(anyString(), any(Page.NavigateOptions.class));
    verifyNoInteractions(parsed);
  }

  private void configureSummaryPage(Page page) {
    Locator options = mock(Locator.class);
    when(options.count()).thenReturn(1);
    when(page.locator("#player-tournament-stats-options")).thenReturn(options);
    Locator tab = mock(Locator.class);
    when(tab.count()).thenReturn(1);
    when(tab.first()).thenReturn(tab);
    when(page.locator(startsWith("#player-tournament-stats-options a"))).thenReturn(tab);
  }

  private void configureDetailPage(Page page) {
    configureSummaryPage(page);

    Locator accumulation = mock(Locator.class);
    when(accumulation.count()).thenReturn(1);
    when(accumulation.inputValue()).thenReturn("0");
    when(page.locator("#statistics-filter-detailed #statsAccumulationType"))
        .thenReturn(accumulation);

    Locator category = mock(Locator.class);
    when(category.count()).thenReturn(1);
    AtomicReference<String> currentCategory = new AtomicReference<>("initial");
    when(category.inputValue()).thenAnswer(inv -> currentCategory.get());
    doAnswer(
            inv -> {
              currentCategory.set(inv.getArgument(0));
              return null;
            })
        .when(category)
        .selectOption(anyString());
    when(page.locator("#statistics-filter-detailed #category")).thenReturn(category);

    Locator container = mock(Locator.class);
    when(container.count()).thenReturn(1);
    when(page.locator("#statistics-table-detailed")).thenReturn(container);

    JSHandle render = mock(JSHandle.class);
    when(page.evaluateHandle(anyString())).thenReturn(render);
  }

  private WhoScoredStatisticsScrapingIntegration integration(
      WhoScoredRequestPacer pacer, boolean filter, WhoScoredScrapingTableParser tables) {
    WhoScoredProperties provider = mock(WhoScoredProperties.class);
    when(provider.baseUrl()).thenReturn("https://provider.example");
    PlayerStatisticsProperties properties =
        new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, filter, true);
    WhoScoredScrapingSectionNavigator navigator =
        new WhoScoredScrapingSectionNavigator(properties, pacer);
    return new WhoScoredStatisticsScrapingIntegration(
        provider, properties, navigator, tables, new WhoScoredStructuredDataParser(), pacer);
  }

  private WhoScoredRequestPacer pacer(AtomicLong time) {
    return new WhoScoredRequestPacer(2500, time::get, time::addAndGet, Clock.systemUTC());
  }

  @Nested
  @DisplayName("Recorrido headed en dos cargas")
  class Scraping {
    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "4", "unknown"})
    @DisplayName("No interpreta como por partido un modo distinto del nativo")
    void rejectsUnexpectedMode(String mode) {
      Page page = mock(Page.class);
      Locator accumulation = mock(Locator.class);
      when(accumulation.count()).thenReturn(1);
      when(accumulation.inputValue()).thenReturn(mode);
      when(page.locator("#statistics-filter-detailed #statsAccumulationType"))
          .thenReturn(accumulation);

      PlayerStatisticsProperties properties =
          new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, false, true);
      WhoScoredScrapingSectionNavigator navigator =
          new WhoScoredScrapingSectionNavigator(properties, pacer(new AtomicLong()));

      assertThatThrownBy(() -> navigator.chooseCategory(page, "aerial"))
          .isInstanceOf(WhoScoredStatisticsStructureChangedException.class);

      verify(accumulation, never()).selectOption(anyString());
      verify(page, never()).locator("#statistics-filter-detailed #category");
    }

    @Test
    @DisplayName("Una categoría que agota su espera no se recarga ni cambia a otra categoría")
    void doesNotRetryCategory() {
      Page page = mock(Page.class);
      configureDetailPage(page);
      JSHandle render = mock(JSHandle.class);
      when(page.evaluateHandle(anyString())).thenReturn(render);
      when(page.waitForFunction(anyString(), any(), any(Page.WaitForFunctionOptions.class)))
          .thenReturn(null)
          .thenThrow(new TimeoutError("recarga controlada"));

      PlayerStatisticsProperties properties =
          new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, false, true);
      WhoScoredScrapingSectionNavigator navigator =
          new WhoScoredScrapingSectionNavigator(properties, pacer(new AtomicLong()));

      assertThatThrownBy(() -> navigator.chooseCategory(page, "aerial"))
          .isInstanceOf(TimeoutError.class);

      verify(page.locator("#statistics-filter-detailed #category")).selectOption("aerial");
      verify(page, never()).reload();
      verify(render).dispose();
    }

    @Test
    @DisplayName("Espera el render de categoría sin tocar acumulados y conserva el timeout")
    void waitsForCategoryRender() {
      Page page = mock(Page.class);
      configureDetailPage(page);
      JSHandle render = mock(JSHandle.class);
      when(page.evaluateHandle(anyString())).thenReturn(render);

      PlayerStatisticsProperties properties =
          new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, false, true);
      WhoScoredScrapingSectionNavigator navigator =
          new WhoScoredScrapingSectionNavigator(properties, pacer(new AtomicLong()));

      navigator.chooseCategory(page, "aerial");

      Locator category = page.locator("#statistics-filter-detailed #category");
      Locator accumulation = page.locator("#statistics-filter-detailed #statsAccumulationType");

      verify(category).selectOption("aerial");
      ArgumentCaptor<Page.WaitForFunctionOptions> options =
          ArgumentCaptor.forClass(Page.WaitForFunctionOptions.class);
      verify(page, atLeastOnce()).waitForFunction(anyString(), any(), options.capture());
      assertThat(options.getValue().timeout).isEqualTo(20000);
      verify(render).dispose();
      verify(accumulation, never()).selectOption(anyString());
      verify(page, never()).reload();
    }

    @Test
    @DisplayName("No vuelve a seleccionar filtros que ya tienen el valor solicitado")
    void doesNotSelectUnchangedFilters() {
      Page page = mock(Page.class);
      Locator accumulation = mock(Locator.class);
      when(accumulation.count()).thenReturn(1);
      when(accumulation.inputValue()).thenReturn("0");
      when(page.locator("#statistics-filter-detailed #statsAccumulationType"))
          .thenReturn(accumulation);

      Locator category = mock(Locator.class);
      when(category.count()).thenReturn(1);
      when(category.inputValue()).thenReturn("aerial");
      when(page.locator("#statistics-filter-detailed #category")).thenReturn(category);

      PlayerStatisticsProperties properties =
          new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, false, true);
      WhoScoredScrapingSectionNavigator navigator =
          new WhoScoredScrapingSectionNavigator(properties, pacer(new AtomicLong()));

      navigator.chooseCategory(page, "aerial");

      verify(category, never()).selectOption(anyString());
      verify(accumulation, never()).selectOption(anyString());
      verify(page, never()).evaluateHandle(anyString());
    }

    @Test
    @DisplayName("Libera el observer ante fallo de selección sin ocultar el error original")
    void preservesSelectionFailure() {
      Page page = mock(Page.class);
      configureDetailPage(page);
      JSHandle render = mock(JSHandle.class);
      when(page.evaluateHandle(anyString())).thenReturn(render);

      Locator category = page.locator("#statistics-filter-detailed #category");
      TimeoutError failure = new TimeoutError("selección controlada");
      doThrow(failure).when(category).selectOption("aerial");
      when(render.evaluate(anyString())).thenThrow(new PlaywrightException("cleanup controlado"));

      PlayerStatisticsProperties properties =
          new PlayerStatisticsProperties(4, 4, 2500, 20000, 20000, false, true);
      WhoScoredScrapingSectionNavigator navigator =
          new WhoScoredScrapingSectionNavigator(properties, pacer(new AtomicLong()));

      assertThatThrownBy(() -> navigator.chooseCategory(page, "aerial")).isSameAs(failure);
      verify(render).dispose();
      verify(category).selectOption("aerial");
      verify(page, never()).reload();
    }

    @Test
    @DisplayName("Un timeout falla sin abrir otro contexto ni reintentar la navegación")
    void doesNotRetryTimeout() {
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

      assertThat(time.get()).isZero();
      verify(stub.browser()).newContext();
      for (BrowserContext context : stub.contexts()) {
        verify(context).close();
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Lee el resumen y las dieciséis categorías con dos cargas del documento")
    void walksSummaryAndCategories(boolean filter) {
      // Arrange
      WhoScoredStatisticsPageStub stub = new WhoScoredStatisticsPageStub();
      Page summary = stub.enqueue(HTML, 200);
      configureSummaryPage(summary);

      Page detail = stub.enqueue(HTML, 200);
      configureDetailPage(detail);

      AtomicLong time = new AtomicLong();
      WhoScoredRequestPacer pacer = pacer(time);

      WhoScoredScrapingTableParser tables = mock(WhoScoredScrapingTableParser.class);
      when(tables.parse(any(), any(), anyString(), any(), any()))
          .thenAnswer(
              invocation -> {
                String section = invocation.getArgument(2);
                WhoScoredStructuredDataBlock block = invocation.getArgument(3);
                String seasonKey =
                    block.entries().isEmpty() ? "unknown" : block.entries().getFirst().seasonId();
                return List.of(
                    new WhoScoredScrapedContextRow(
                        section, "5", "3", seasonKey, null, null, List.of(), Instant.now()));
              });

      WhoScoredStatisticsScrapingIntegration integration = integration(pacer, filter, tables);

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
        verify(summary)
            .navigate(
                eq("https://provider.example/players/123/show"), any(Page.NavigateOptions.class));
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
