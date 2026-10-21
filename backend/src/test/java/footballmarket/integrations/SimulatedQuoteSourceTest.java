package footballmarket.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.integrations.exceptions.QuoteBatchUnavailableException;
import footballmarket.models.records.CurrentQuote;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comportamiento del simulador de cotizaciones.
 *
 * <p>Prueba pura de una Integration autocontenida: no usa Spring, ni base de datos, ni el perfil
 * {@code test}, conforme a {@code docs/backend/testing.md} §2 y §12.
 */
@org.junit.jupiter.api.Tag("unit")
class SimulatedQuoteSourceTest {

  private static final Long PLAYER_ID = 7821L;

  @Nested
  @DisplayName("Cotizaciones masivas compatibles")
  class BatchQuotes {
    @Test
    @DisplayName("Construye el lote con un instante común sin invocar la lectura individual")
    void construyeLoteSinDelegar() {
      // Arrange
      Instant instant = Instant.parse("2026-10-20T12:00:00Z");
      Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
      QuoteSource source =
          new SimulatedQuoteSource(125L, clock) {
            @Override
            public Optional<CurrentQuote> currentQuote(Long playerId) {
              throw new AssertionError("El lote no debe invocar currentQuote");
            }
          };

      // Act
      Map<Long, CurrentQuote> result = source.currentQuotes(Set.of(1L, 2L));

      // Assert
      assertThat(result).containsOnlyKeys(1L, 2L);
      assertThat(result.get(1L).playerId()).isEqualTo(1L);
      assertThat(result.get(2L).playerId()).isEqualTo(2L);
      assertThat(result.values())
          .allSatisfy(
              quote -> {
                assertThat(quote.price()).isEqualTo(125L);
                assertThat(quote.calculatedAt()).isEqualTo(instant.atOffset(ZoneOffset.UTC));
              });
    }

    @Test
    @DisplayName("Un conjunto vacío no produce cotizaciones")
    void aceptaConjuntoVacio() {
      // Arrange
      QuoteSource source = new SimulatedQuoteSource();
      // Act / Assert
      assertThat(source.currentQuotes(Set.of())).isEmpty();
    }

    @Test
    @DisplayName("Una implementación antigua conserva la lectura individual y rechaza el lote")
    void conservaCompatibilidadIndividual() {
      // Arrange
      CurrentQuote quote = new CurrentQuote(1L, 100L, OffsetDateTime.now(ZoneOffset.UTC), null);
      QuoteSource legacy = playerId -> Optional.of(quote);
      // Act / Assert
      assertThat(legacy.currentQuote(1L)).contains(quote);
      assertThatThrownBy(() -> legacy.currentQuotes(Set.of(1L)))
          .isInstanceOf(QuoteBatchUnavailableException.class);
    }
  }

  @Nested
  @DisplayName("Cotización simulada")
  class SimulatedQuote {
    @Test
    @DisplayName("Devuelve siempre una cotización para el jugador solicitado")
    void devuelveCotizacionParaElJugadorSolicitado() {
      // Arrange
      QuoteSource quotes = new SimulatedQuoteSource();

      // Act
      CurrentQuote quote = quotes.currentQuote(PLAYER_ID).orElseThrow();

      // Assert
      assertThat(quote.playerId()).isEqualTo(PLAYER_ID);
    }

    @Test
    @DisplayName("Usa el precio configurado en centésimas de crédito")
    void usaElPrecioConfigurado() {
      // Arrange
      QuoteSource quotes = new SimulatedQuoteSource(250L);

      // Act
      CurrentQuote quote = quotes.currentQuote(PLAYER_ID).orElseThrow();

      // Assert
      assertThat(quote.price()).isEqualTo(250L);
    }

    @Test
    @DisplayName("El precio por defecto equivale a un crédito en centésimas")
    void aplicaElPrecioPorDefecto() {
      // Arrange / Act
      CurrentQuote quote = new SimulatedQuoteSource().currentQuote(PLAYER_ID).orElseThrow();

      // Assert
      assertThat(quote.price()).isEqualTo(SimulatedQuoteSource.DEFAULT_PRICE);
      assertThat(quote.price()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Identifica la cotización como simulada")
    void identificaLaCotizacionComoSimulada() {
      // Arrange / Act
      CurrentQuote quote = new SimulatedQuoteSource().currentQuote(PLAYER_ID).orElseThrow();

      // Assert
      assertThat(quote.strategyVersion()).isNotBlank();
      assertThat(quote.strategyVersion()).containsIgnoringCase("simul");
    }

    @Test
    @DisplayName("La instantánea se fecha en UTC con el reloj entregado")
    void fechaLaInstantaneaEnUtc() {
      // Arrange
      Instant fixed = Instant.parse("2026-10-14T10:30:00Z");
      QuoteSource quotes = new SimulatedQuoteSource(100L, Clock.fixed(fixed, ZoneOffset.UTC));

      // Act
      CurrentQuote quote = quotes.currentQuote(PLAYER_ID).orElseThrow();

      // Assert
      assertThat(quote.calculatedAt()).isEqualTo(OffsetDateTime.ofInstant(fixed, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("La cotización corresponde al jugador pedido, nunca a otro")
    void correspondeAlJugadorSolicitado() {
      // Arrange
      QuoteSource quotes = new SimulatedQuoteSource();

      // Act
      CurrentQuote first = quotes.currentQuote(PLAYER_ID).orElseThrow();
      CurrentQuote second = quotes.currentQuote(99L).orElseThrow();

      // Assert
      assertThat(first.playerId()).isEqualTo(PLAYER_ID);
      assertThat(second.playerId()).isEqualTo(99L);
      assertThat(first.playerId()).isNotEqualTo(second.playerId());
    }
  }
}
