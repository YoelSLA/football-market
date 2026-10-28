package footballmarket.features.market.application;

import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.records.BuyListingRequest;
import footballmarket.features.market.domain.records.BuyOrderRequest;

/**
 * Caso de uso de compra de tokens de un jugador.
 *
 * <p>Es la única frontera de aplicación del dominio de mercado: no existen servicios financieros
 * auxiliares ni orquestadores, de modo que la transferencia de créditos y tokens y su registro
 * financiero forman una sola operación transaccional.
 */
public interface BuyOrderService {

  /**
   * Compra tokens de un jugador para el usuario autenticado.
   *
   * <p>La compra es independiente de cualquier solicitud anterior: repetirla no es motivo de
   * rechazo y produce una compra nueva con su propio registro financiero.
   *
   * @param buyerEmail correo del comprador, tomado del sujeto de la sesión autenticada
   * @param order intención de compra con el jugador y la cantidad solicitados
   * @return registro financiero de la compra confirmada
   */
  MarketTransaction buy(String buyerEmail, BuyOrderRequest order);

  /**
   * Liquida una publicación completa una única vez, con el precio congelado y auditoría atómica.
   */
  MarketTransaction buyListing(String buyerEmail, BuyListingRequest order);
}
