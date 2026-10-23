package footballmarket.integrations;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.models.Player;
import footballmarket.models.User;
import footballmarket.models.records.CurrentQuote;
import footballmarket.support.MarketTestData;
import footballmarket.support.QuoteTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Categoría Integration de la fuente de cotización real.
 *
 * <p>Afirma el comportamiento observable del adaptador contra el repositorio canónico de
 * cotizaciones: la fila vigente del jugador, la ausencia de cotización, el instante de cálculo
 * conservado y la versión de estrategia nula de la fila inicial. No reimprime la lógica interna del
 * repositorio, que se afirma en su propia categoría.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, QuoteTestData.class})
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("integration")
class RepositoryQuoteSourceTest {

  private static final LocalDate MARKET_DATE = LocalDate.of(2026, 10, 17);
  private static final LocalDate QUOTE_DATE = LocalDate.of(2026, 10, 20);
  private static final Instant INITIAL_INSTANT = Instant.parse("2026-10-17T09:00:00Z");
  private static final Instant RECOMPUTED_INSTANT = Instant.parse("2026-10-20T09:00:00Z");

  @Autowired private RepositoryQuoteSource source;
  @Autowired private MarketTestData market;
  @Autowired private QuoteTestData quoteData;

  @Nested
  @DisplayName("Cotización vigente del jugador")
  class CurrentQuoteOfPlayer {

    @Test
    @DisplayName("Devuelve la última fila persistida del jugador")
    void devuelveLaUltimaFilaDelJugador() {
      // Arrange
      Player player = preparePlayer();
      quoteData.persistInitial(player, MARKET_DATE, INITIAL_INSTANT);
      quoteData.persistRecomputed(player, QUOTE_DATE, 130L, RECOMPUTED_INSTANT);

      // Act
      Optional<CurrentQuote> quote = source.currentQuote(player.getId());

      // Assert
      assertThat(quote).isPresent();
      assertThat(quote.orElseThrow().price()).isEqualTo(130L);
      assertThat(quote.orElseThrow().playerId()).isEqualTo(player.getId());
    }

    @Test
    @DisplayName("Devuelve vacío cuando el jugador no tiene ninguna cotización")
    void devuelveVacioSinCotizacion() {
      // Arrange
      Player player = preparePlayer();

      // Act
      Optional<CurrentQuote> quote = source.currentQuote(player.getId());

      // Assert
      assertThat(quote).isEmpty();
    }

    @Test
    @DisplayName("Conserva el instante de cálculo de la cotización y no el de la lectura")
    void conservaElInstanteDeCalculo() {
      // Arrange
      Player player = preparePlayer();
      quoteData.persistInitial(player, MARKET_DATE, INITIAL_INSTANT);

      // Act
      CurrentQuote quote = source.currentQuote(player.getId()).orElseThrow();

      // Assert
      assertThat(quote.calculatedAt())
          .isEqualTo(INITIAL_INSTANT.atOffset(java.time.ZoneOffset.UTC));
    }

    @Test
    @DisplayName("La fila inicial no tiene estrategia y su versión se conserva nula")
    void conservaLaVersionNulaDeLaFilaInicial() {
      // Arrange
      Player player = preparePlayer();
      quoteData.persistInitial(player, MARKET_DATE, INITIAL_INSTANT);

      // Act
      CurrentQuote quote = source.currentQuote(player.getId()).orElseThrow();

      // Assert
      assertThat(quote.strategyVersion()).isNull();
    }
  }

  private Player preparePlayer() {
    User superuser = market.registerSuperuser();
    Player player = market.createVisiblePlayer();
    market.emitInitialTokens(player, superuser);
    return player;
  }
}
