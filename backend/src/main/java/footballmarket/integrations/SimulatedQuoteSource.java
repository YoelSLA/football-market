package footballmarket.integrations;

import footballmarket.models.records.CurrentQuote;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Fuente de cotización simulada para desarrollo y pruebas.
 *
 * <p>Es un componente estrictamente temporal: se elimina cuando exista la integración real de
 * REQ-S2-03. No crea tablas ni datos persistentes y nunca sustituye a una cotización real ausente,
 * porque la selección de la fuente ocurre en el arranque y no durante la compra.
 */
public class SimulatedQuoteSource implements QuoteSource {

  /** Precio por token en centésimas: 100 equivalen a 1 crédito, el valor inicial del enunciado. */
  public static final long DEFAULT_PRICE = 100L;

  /**
   * Identifica la cotización como simulada para que ningún dato de prueba se confunda con uno real.
   */
  private static final String SIMULATED_STRATEGY_VERSION = "simulated";

  private final long price;
  private final Clock clock;

  /** Crea el simulador con el precio por defecto y el reloj del sistema. */
  public SimulatedQuoteSource() {
    this(DEFAULT_PRICE);
  }

  /**
   * Crea el simulador con el precio indicado.
   *
   * @param price precio por token en centésimas de crédito
   */
  public SimulatedQuoteSource(long price) {
    this(price, Clock.systemUTC());
  }

  /**
   * Crea el simulador con el precio y el reloj indicados.
   *
   * @param price precio por token en centésimas de crédito
   * @param clock reloj usado para fechar la cotización
   */
  public SimulatedQuoteSource(long price, Clock clock) {
    this.price = price;
    this.clock = clock;
  }

  /** {@inheritDoc} */
  @Override
  public Optional<CurrentQuote> currentQuote(Long playerId) {
    OffsetDateTime calculatedAt = OffsetDateTime.now(this.clock.withZone(ZoneOffset.UTC));
    return Optional.of(
        new CurrentQuote(playerId, this.price, calculatedAt, SIMULATED_STRATEGY_VERSION));
  }
}
