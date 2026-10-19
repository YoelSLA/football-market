package footballmarket.integrations;

import static org.assertj.core.api.Assertions.*;

import footballmarket.integrations.records.WhoScoredStructuredDataBlock;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

@org.junit.jupiter.api.Tag("unit")
class WhoScoredStructuredDataParserTest {
  private final WhoScoredStructuredDataParser parser = new WhoScoredStructuredDataParser();

  @Nested
  @DisplayName("Bloque JSON opcional del HTML original")
  class Structured {
    @Test
    @DisplayName(
        "Conserva el token original de rating con dieciséis decimales sin ejecutar JavaScript")
    void preservesOriginalDecimal() {
      // Arrange
      String html =
          """
          <script>require.config.params['args'] = {
          tournaments: [{"TournamentId":5,"SeasonId":11126,"TeamId":2889,
          "Rating":6.9033333333333333,"ShotsOnTarget":0,"SecondYellow":0,"GameStarted":6,"SubOn":0,
          "Unrelated":"embedded ] [ delimiters"}], playerId: 123 }; </script>
          """;
      // Act
      WhoScoredStructuredDataBlock result = parser.parse(html);
      // Assert
      assertThat(result.entries())
          .singleElement()
          .satisfies(
              entry -> {
                assertThat(entry.rating()).isEqualTo(new BigDecimal("6.9033333333333333"));
                assertThat(entry.rating().scale()).isEqualTo(16);
                assertThat(entry.shotsOnTarget()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(entry.gameStarted()).isEqualByComparingTo("6");
                assertThat(entry.competitionId()).isEqualTo("5");
                assertThat(entry.seasonId()).isEqualTo("11126");
              });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"<html></html>", "tournaments: [", "tournaments: [broken]"})
    @DisplayName("Un bloque ausente o incompleto no inventa indicadores opcionales")
    void acceptsMissingBlock(String html) {
      // Act / Assert
      assertThat(parser.parse(html).entries()).isEmpty();
    }

    @Test
    @DisplayName("Conserva ceros finales publicados y permite campos opcionales ausentes")
    void preservesTrailingZeroPrecision() {
      // Act
      WhoScoredStructuredDataBlock result = parser.parse("tournaments: [{\"Rating\":6.9000}]");
      // Assert
      assertThat(result.entries())
          .singleElement()
          .satisfies(
              entry -> {
                assertThat(entry.rating()).isEqualTo(new BigDecimal("6.9000"));
                assertThat(entry.shotsOnTarget()).isNull();
              });
    }
  }
}
