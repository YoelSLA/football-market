package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/**
 * Indica que el importe de la compra o el saldo resultante de una acreditación no puede
 * representarse con exactitud.
 *
 * <p>La operación se revierte completa: no transfiere tokens, no modifica saldos y no registra
 * compra. La respuesta pública es un 500 genérico.
 */
public class ArithmeticOverflowException extends ApplicationException {
  public ArithmeticOverflowException() {
    super("El importe de la compra no puede representarse correctamente.");
  }
}
