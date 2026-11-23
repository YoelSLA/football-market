package footballmarket.features.market.application;

import footballmarket.features.market.domain.records.OfficialBuybackReceipt;
import footballmarket.features.market.domain.records.OfficialBuybackRequest;

/**
 * Caso de uso de la venta oficial inmediata de tokens al superusuario.
 *
 * <p>Es la frontera transaccional que liquida créditos y tokens y registra la operación financiera
 * como una sola unidad atómica.
 */
public interface OfficialBuybackService {

  /**
   * Vende tokens libres del usuario autenticado al superusuario.
   *
   * @param sellerEmail correo del vendedor, tomado del sujeto de la sesión autenticada
   * @param order jugador y cantidad solicitados
   * @return comprobante de la venta confirmada
   */
  OfficialBuybackReceipt sellToSuperuser(String sellerEmail, OfficialBuybackRequest order);
}
