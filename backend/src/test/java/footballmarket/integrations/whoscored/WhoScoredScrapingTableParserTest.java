package footballmarket.integrations.whoscored;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsStructureChangedException;
import footballmarket.integrations.whoscored.records.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@org.junit.jupiter.api.Tag("unit")
class WhoScoredScrapingTableParserTest {
  private static final String CONTAINER = "#statistics-table-summary";
  private static final String SECTION = "summary";
  private static final Instant OBSERVED_AT = Instant.parse("2026-11-27T12:00:00Z");

  private final WhoScoredScrapingTableParser parser = new WhoScoredScrapingTableParser();

  private WhoScoredStructuredDataBlock structuredBlock(
      WhoScoredStructuredDataBlock.Entry... entries) {
    return new WhoScoredStructuredDataBlock(List.of(entries));
  }

  private WhoScoredStructuredDataBlock.Entry entry(
      String competitionId, String seasonId, String teamId) {
    return new WhoScoredStructuredDataBlock.Entry(
        competitionId,
        seasonId,
        teamId,
        new BigDecimal("7.50"),
        BigDecimal.ONE,
        BigDecimal.ZERO,
        BigDecimal.valueOf(10),
        BigDecimal.ZERO);
  }

  @Nested
  @DisplayName("Parseo exitoso de tablas")
  class SuccessScenarios {
    @Test
    @DisplayName("Extrae métricas y contexto resolviendo temporada vigente")
    void parsesValidTableWithMultipleRowsAndMetrics() {
      // Arrange
      List<String> headers = List.of("", "Apps", "goal", "assistTotal");
      List<MockRow> rows =
          List.of(
              MockRow.valid(
                  "5",
                  "3",
                  List.of("Liga A", "6", "2", "1"),
                  "/tournaments/5/league",
                  "/teams/3/show"),
              MockRow.valid(
                  "6", "4", List.of("Copa", "2", "0", "0"), "/tournaments/6/cup", "/teams/4/show"));

      Page page = mockPage(CONTAINER, headers, rows);
      WhoScoredStructuredDataBlock structured =
          structuredBlock(entry("5", "11126", "3"), entry("6", "22200", "4"));

      // Act
      List<WhoScoredScrapedContextRow> result =
          parser.parse(page, CONTAINER, SECTION, structured, OBSERVED_AT);

      // Assert
      assertThat(result).hasSize(2);

      WhoScoredScrapedContextRow first = result.get(0);
      assertThat(first.section()).isEqualTo(SECTION);
      assertThat(first.competitionId()).isEqualTo("5");
      assertThat(first.teamId()).isEqualTo("3");
      assertThat(first.seasonKey()).isEqualTo("11126");
      assertThat(first.observedAt()).isEqualTo(OBSERVED_AT);
      assertThat(first.cells())
          .extracting(WhoScoredScrapedMetricCell::providerField, WhoScoredScrapedMetricCell::text)
          .containsExactly(tuple("Apps", "6"), tuple("goal", "2"), tuple("assistTotal", "1"));

      WhoScoredScrapedContextRow second = result.get(1);
      assertThat(second.competitionId()).isEqualTo("6");
      assertThat(second.teamId()).isEqualTo("4");
      assertThat(second.seasonKey()).isEqualTo("22200");
    }

    @Test
    @DisplayName("Omite fila de totales/promedios sin enlace de competición")
    void skipsTotalsRowWithoutCompetitionLink() {
      // Arrange
      List<String> headers = List.of("", "Apps", "goal");
      List<MockRow> rows =
          List.of(
              MockRow.valid(
                  "5", "3", List.of("Liga A", "6", "2"), "/tournaments/5/league", "/teams/3/show"),
              MockRow.totals(List.of("Total / Promedio", "6", "2")));

      Page page = mockPage(CONTAINER, headers, rows);
      WhoScoredStructuredDataBlock structured = structuredBlock(entry("5", "11126", "3"));

      // Act
      List<WhoScoredScrapedContextRow> result =
          parser.parse(page, CONTAINER, SECTION, structured, OBSERVED_AT);

      // Assert
      assertThat(result).hasSize(1);
      assertThat(result.getFirst().competitionId()).isEqualTo("5");
    }

    @Test
    @DisplayName("Omite fila con colspan (mensaje sin datos)")
    void skipsColspanRow() {
      // Arrange
      List<String> headers = List.of("", "Apps", "goal");
      List<MockRow> rows =
          List.of(
              MockRow.colspan("3", "No hay datos disponibles"),
              MockRow.valid(
                  "5", "3", List.of("Liga A", "6", "2"), "/tournaments/5/league", "/teams/3/show"));

      Page page = mockPage(CONTAINER, headers, rows);
      WhoScoredStructuredDataBlock structured = structuredBlock(entry("5", "11126", "3"));

      // Act
      List<WhoScoredScrapedContextRow> result =
          parser.parse(page, CONTAINER, SECTION, structured, OBSERVED_AT);

      // Assert
      assertThat(result).hasSize(1);
      assertThat(result.getFirst().competitionId()).isEqualTo("5");
    }

    @Test
    @DisplayName("Omite fila fuera de la temporada vigente declarada en datos estructurados")
    void skipsRowOutsideCurrentSeason() {
      // Arrange
      List<String> headers = List.of("", "Apps", "goal");
      List<MockRow> rows =
          List.of(
              MockRow.valid(
                  "5", "3", List.of("Liga A", "6", "2"), "/tournaments/5/league", "/teams/3/show"),
              MockRow.valid(
                  "99",
                  "3",
                  List.of("Amistoso", "1", "0"),
                  "/tournaments/99/friendly",
                  "/teams/3/show"));

      Page page = mockPage(CONTAINER, headers, rows);
      // Structured block only contains competition 5, not 99
      WhoScoredStructuredDataBlock structured = structuredBlock(entry("5", "11126", "3"));

      // Act
      List<WhoScoredScrapedContextRow> result =
          parser.parse(page, CONTAINER, SECTION, structured, OBSERVED_AT);

      // Assert
      assertThat(result).hasSize(1);
      assertThat(result.getFirst().competitionId()).isEqualTo("5");
    }

    @Test
    @DisplayName("Resuelve temporada de mayor rango cuando existen múltiples candidatos")
    void resolvesHighestSeasonWhenMultipleCandidatesExist() {
      // Arrange
      List<String> headers = List.of("", "Apps");
      List<MockRow> rows =
          List.of(
              MockRow.valid(
                  "5", "3", List.of("Liga A", "6"), "/tournaments/5/league", "/teams/3/show"));

      Page page = mockPage(CONTAINER, headers, rows);
      // Two entries for same competition and team with different season ranks
      WhoScoredStructuredDataBlock structured =
          structuredBlock(
              entry("5", "10000", "3"), entry("5", "11126", "3"), entry("5", "9000", "3"));

      // Act
      List<WhoScoredScrapedContextRow> result =
          parser.parse(page, CONTAINER, SECTION, structured, OBSERVED_AT);

      // Assert
      assertThat(result).hasSize(1);
      assertThat(result.getFirst().seasonKey()).isEqualTo("11126");
    }

    @Test
    @DisplayName("Tolera seasonKey no numérica asignándole menor rango")
    void handlesNonNumericSeasonKeyGracefully() {
      // Arrange
      List<String> headers = List.of("", "Apps");
      List<MockRow> rows =
          List.of(
              MockRow.valid(
                  "5", "3", List.of("Liga A", "6"), "/tournaments/5/league", "/teams/3/show"));

      Page page = mockPage(CONTAINER, headers, rows);
      WhoScoredStructuredDataBlock structured =
          structuredBlock(entry("5", "NOT_A_NUMBER", "3"), entry("5", "11126", "3"));

      // Act
      List<WhoScoredScrapedContextRow> result =
          parser.parse(page, CONTAINER, SECTION, structured, OBSERVED_AT);

      // Assert
      assertThat(result).hasSize(1);
      assertThat(result.getFirst().seasonKey()).isEqualTo("11126");
    }
  }

  @Nested
  @DisplayName("Validación de cambios de estructura")
  class StructureChangeScenarios {
    @Test
    @DisplayName("Falla si la tabla no existe o hay múltiples")
    void rejectsWhenTableCountNotOne() {
      Page page = mock(Page.class);
      Locator table = mock(Locator.class);
      when(table.count()).thenReturn(0);
      when(page.locator(CONTAINER + " table")).thenReturn(table);

      assertThatThrownBy(
              () -> parser.parse(page, CONTAINER, SECTION, structuredBlock(), OBSERVED_AT))
          .isInstanceOf(WhoScoredStatisticsStructureChangedException.class);
    }

    @Test
    @DisplayName("Falla si los encabezados son menores a 2")
    void rejectsWhenHeadersCountLessThanTwo() {
      Page page = mockPage(CONTAINER, List.of(""), List.of());

      assertThatThrownBy(
              () -> parser.parse(page, CONTAINER, SECTION, structuredBlock(), OBSERVED_AT))
          .isInstanceOf(WhoScoredStatisticsStructureChangedException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Falla si algún encabezado tiene data-stat-name nulo o en blanco")
    void rejectsWhenHeaderStatNameIsBlank(String invalidStatName) {
      List<String> headers = Arrays.asList("", invalidStatName);
      List<MockRow> rows =
          List.of(
              MockRow.valid(
                  "5", "3", List.of("Liga A", "6"), "/tournaments/5/league", "/teams/3/show"));
      Page page = mockPage(CONTAINER, headers, rows);
      WhoScoredStructuredDataBlock structured = structuredBlock(entry("5", "11126", "3"));

      assertThatThrownBy(() -> parser.parse(page, CONTAINER, SECTION, structured, OBSERVED_AT))
          .isInstanceOf(WhoScoredStatisticsStructureChangedException.class);
    }

    @Test
    @DisplayName("Falla si la cantidad de celdas no coincide con la cantidad de encabezados")
    void rejectsWhenCellCountDiffersFromHeaderCount() {
      List<String> headers = List.of("", "Apps", "goal"); // 3 headers
      List<MockRow> rows =
          List.of(
              MockRow.valid(
                  "5",
                  "3",
                  List.of("Liga A", "6"),
                  "/tournaments/5/league",
                  "/teams/3/show")); // 2 cells
      Page page = mockPage(CONTAINER, headers, rows);
      WhoScoredStructuredDataBlock structured = structuredBlock(entry("5", "11126", "3"));

      assertThatThrownBy(() -> parser.parse(page, CONTAINER, SECTION, structured, OBSERVED_AT))
          .isInstanceOf(WhoScoredStatisticsStructureChangedException.class);
    }
  }

  @Nested
  @DisplayName("Validación de inconsistencias del proveedor")
  class InconsistentDataScenarios {
    @Test
    @DisplayName(
        "Falla con PROVIDER_DATA_INCONSISTENT si hay enlaces de competición contradictorios")
    void rejectsContradictoryCompetitionIds() {
      List<String> headers = List.of("", "Apps");
      MockRow row =
          new MockRow(
              List.of("/tournaments/5/league", "/tournaments/6/league"),
              List.of("/teams/3/show"),
              List.of("Liga A", "6"),
              null);

      Page page = mockPage(CONTAINER, headers, List.of(row));
      WhoScoredStructuredDataBlock structured = structuredBlock(entry("5", "11126", "3"));

      assertThatThrownBy(() -> parser.parse(page, CONTAINER, SECTION, structured, OBSERVED_AT))
          .isInstanceOfSatisfying(
              WhoScoredStatisticsException.class,
              e -> assertThat(e.getCode()).isEqualTo("PROVIDER_DATA_INCONSISTENT"));
    }

    @Test
    @DisplayName("Falla con PROVIDER_DATA_INCONSISTENT si hay enlaces de equipo contradictorios")
    void rejectsContradictoryTeamIds() {
      List<String> headers = List.of("", "Apps");
      MockRow row =
          new MockRow(
              List.of("/tournaments/5/league"),
              List.of("/teams/3/show", "/teams/4/show"),
              List.of("Liga A", "6"),
              null);

      Page page = mockPage(CONTAINER, headers, List.of(row));
      WhoScoredStructuredDataBlock structured = structuredBlock(entry("5", "11126", "3"));

      assertThatThrownBy(() -> parser.parse(page, CONTAINER, SECTION, structured, OBSERVED_AT))
          .isInstanceOfSatisfying(
              WhoScoredStatisticsException.class,
              e -> assertThat(e.getCode()).isEqualTo("PROVIDER_DATA_INCONSISTENT"));
    }

    @Test
    @DisplayName(
        "Falla con PROVIDER_DATA_INCONSISTENT si no se encuentra temporada en bloque estructurado")
    void rejectsWhenNoSeasonCandidateFound() {
      List<String> headers = List.of("", "Apps");
      List<MockRow> rows =
          List.of(
              MockRow.valid(
                  "5", "3", List.of("Liga A", "6"), "/tournaments/5/league", "/teams/3/show"));

      Page page = mockPage(CONTAINER, headers, rows);
      // Entry matches competition 5 with season, but for team 99; row has team 3
      WhoScoredStructuredDataBlock structured = structuredBlock(entry("5", "11126", "99"));

      assertThatThrownBy(() -> parser.parse(page, CONTAINER, SECTION, structured, OBSERVED_AT))
          .isInstanceOfSatisfying(
              WhoScoredStatisticsException.class,
              e -> assertThat(e.getCode()).isEqualTo("PROVIDER_DATA_INCONSISTENT"));
    }
  }

  // --- Helpers de simulación DOM ---

  private record MockRow(
      List<String> competitionHrefs,
      List<String> teamHrefs,
      List<String> cellValues,
      String colspan) {

    static MockRow valid(
        String compId, String teamId, List<String> cellValues, String compHref, String teamHref) {
      return new MockRow(
          compHref != null ? List.of(compHref) : List.of(),
          teamHref != null ? List.of(teamHref) : List.of(),
          cellValues,
          null);
    }

    static MockRow totals(List<String> cellValues) {
      return new MockRow(List.of(), List.of(), cellValues, null);
    }

    static MockRow colspan(String colspan, String message) {
      return new MockRow(List.of(), List.of(), List.of(message), colspan);
    }
  }

  private Page mockPage(String container, List<String> headerStatNames, List<MockRow> rows) {
    Page page = mock(Page.class);
    Locator table = mock(Locator.class);
    when(table.count()).thenReturn(1);
    when(page.locator(container + " table")).thenReturn(table);

    // Headers
    Locator headers = mock(Locator.class);
    when(headers.count()).thenReturn(headerStatNames.size());
    for (int i = 0; i < headerStatNames.size(); i++) {
      Locator th = mock(Locator.class);
      when(th.getAttribute("data-stat-name")).thenReturn(headerStatNames.get(i));
      when(headers.nth(i)).thenReturn(th);
    }
    when(table.locator("thead th")).thenReturn(headers);

    // Rows
    Locator tbodyTr = mock(Locator.class);
    List<Locator> rowLocators = new ArrayList<>();

    for (MockRow row : rows) {
      Locator rowLoc = mock(Locator.class);
      Locator cellsLoc = mock(Locator.class);
      when(cellsLoc.count()).thenReturn(row.cellValues().size());

      for (int c = 0; c < row.cellValues().size(); c++) {
        Locator cell = mock(Locator.class);
        String val = row.cellValues().get(c);
        when(cell.innerText()).thenReturn(val);
        if (c == 0 && row.colspan() != null) {
          when(cell.getAttribute("colspan")).thenReturn(row.colspan());
        }
        when(cellsLoc.nth(c)).thenReturn(cell);
        if (c == 0) {
          when(cellsLoc.first()).thenReturn(cell);
        }
      }
      when(rowLoc.locator(":scope > td")).thenReturn(cellsLoc);

      // Links (competition & team)
      List<Locator> links = new ArrayList<>();
      for (String href : row.competitionHrefs()) {
        Locator link = mock(Locator.class);
        when(link.getAttribute("href")).thenReturn(href);
        links.add(link);
      }
      for (String href : row.teamHrefs()) {
        Locator link = mock(Locator.class);
        when(link.getAttribute("href")).thenReturn(href);
        links.add(link);
      }
      Locator linksLoc = mock(Locator.class);
      when(linksLoc.all()).thenReturn(links);
      when(rowLoc.locator("a")).thenReturn(linksLoc);

      rowLocators.add(rowLoc);
    }

    when(tbodyTr.all()).thenReturn(rowLocators);
    when(table.locator("tbody tr")).thenReturn(tbodyTr);

    return page;
  }
}
