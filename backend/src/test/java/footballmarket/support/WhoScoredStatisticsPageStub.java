package footballmarket.support;

import static org.mockito.Mockito.*;

import com.microsoft.playwright.*;
import java.util.*;

/** Dobles del protocolo Playwright que no crean procesos ni requieren sesión gráfica. */
public final class WhoScoredStatisticsPageStub {
  private final Browser browser = mock(Browser.class);
  private final List<BrowserContext> contexts = new ArrayList<>();
  private final List<Page> pages = new ArrayList<>();
  private final Queue<Section> sections = new ArrayDeque<>();

  public WhoScoredStatisticsPageStub() {
    when(browser.isConnected()).thenReturn(true);
    when(browser.newContext()).thenAnswer(invocation -> openSection());
    when(browser.newContext(any(Browser.NewContextOptions.class)))
        .thenAnswer(invocation -> openSection());
  }

  public Page enqueue(String originalHtml, int status) {
    BrowserContext context = mock(BrowserContext.class);
    Page page = mock(Page.class);
    Response response = mock(Response.class);
    when(context.newPage()).thenReturn(page);
    when(page.content()).thenReturn(originalHtml);
    when(page.navigate(anyString())).thenReturn(response);
    when(page.navigate(anyString(), any(Page.NavigateOptions.class))).thenReturn(response);
    when(response.status()).thenReturn(status);
    when(response.text()).thenReturn(originalHtml);
    sections.add(new Section(context, page));
    return page;
  }

  public Browser browser() {
    return browser;
  }

  public List<BrowserContext> contexts() {
    return List.copyOf(contexts);
  }

  public List<Page> pages() {
    return List.copyOf(pages);
  }

  private BrowserContext openSection() {
    Section section = sections.remove();
    contexts.add(section.context());
    pages.add(section.page());
    return section.context();
  }

  private record Section(BrowserContext context, Page page) {}
}
