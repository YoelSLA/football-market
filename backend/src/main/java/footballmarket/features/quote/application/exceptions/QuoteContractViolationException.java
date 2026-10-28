package footballmarket.features.quote.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/**
 * Indica que la fuente de cotización devolvió una instantánea de otro jugador.
 *
 * <p>Es un incumplimiento interno del contrato de integración, no un error de negocio del
 * solicitante: la falla está en un componente interno y por eso la respuesta pública es un 500
 * genérico que no expone detalles técnicos.
 */
public class QuoteContractViolationException extends ApplicationException {
  public QuoteContractViolationException() {
    super("La fuente de cotización devolvió una instantánea de un jugador distinto al solicitado.");
  }
}
