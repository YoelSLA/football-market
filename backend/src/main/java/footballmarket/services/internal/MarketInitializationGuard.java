package footballmarket.services.internal;

import footballmarket.models.enums.QuoteOrigin;
import footballmarket.repositories.PlayerQuoteRepository;
import footballmarket.services.exceptions.MarketNotInitializedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Comprobación del estado de inicialización del mercado para las operaciones de mercado.
 *
 * <p>El mercado está inicializado cuando existe al menos una cotización de origen {@code INITIAL},
 * que solo la inicialización puede crear. La comprobación se resuelve por el repositorio canónico
 * de cotizaciones y distingue un mercado sin inicializar de un jugador sin cotización, que son
 * hechos distintos: el primero invalida toda operación de mercado y el segundo solo afecta a un
 * jugador.
 *
 * <p>Es un componente interno del rol Service, no un Service independiente: no expone interfaz de
 * aplicación, no es consumido por otros Services y comparte el límite transaccional de los
 * servicios que lo componen.
 */
@Component
@RequiredArgsConstructor
public class MarketInitializationGuard {

  private final PlayerQuoteRepository quotes;

  /**
   * Rechaza la operación cuando el mercado todavía no fue inicializado.
   *
   * @throws MarketNotInitializedException si no existe ninguna cotización de origen {@code INITIAL}
   */
  public void requireInitialized() {
    if (!this.quotes.existsByOrigin(QuoteOrigin.INITIAL)) {
      throw new MarketNotInitializedException();
    }
  }
}
