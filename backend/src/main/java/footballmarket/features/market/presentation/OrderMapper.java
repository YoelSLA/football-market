package footballmarket.features.market.presentation;

import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.records.BuyListingRequest;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.market.presentation.dtos.request.OrderBuyRequestDTO;
import footballmarket.features.market.presentation.dtos.response.OrderBuyResponseDTO;

/**
 * Transforma entre el contrato HTTP de compra y el modelo de mercado.
 *
 * <p>No contiene negocio, validaciones de negocio ni cálculos: los importes que expone la respuesta
 * son los mismos que la transferencia y la auditoría usaron.
 */
public final class OrderMapper {

  private OrderMapper() {}

  /**
   * Convierte una solicitud de compra validada en la intención de compra del modelo.
   *
   * @param request solicitud de compra validada
   * @return intención de compra del dominio
   */
  public static BuyOrderRequest toBuyRequest(OrderBuyRequestDTO request) {
    return new BuyOrderRequest(request.playerId(), request.quantity());
  }

  public static BuyListingRequest toBuyListingRequest(OrderBuyRequestDTO request) {
    return new BuyListingRequest(request.listingId());
  }

  /**
   * Expone el resultado de una compra confirmada.
   *
   * @param transaction registro financiero de la compra confirmada
   * @return respuesta contractual de la operación
   */
  public static OrderBuyResponseDTO toBuyResponse(MarketTransaction transaction) {
    return new OrderBuyResponseDTO(
        transaction.getId(),
        transaction.getPlayer().getId(),
        transaction.getQuantity(),
        transaction.getUnitPrice(),
        transaction.getTotalAmount(),
        transaction.getExecutedAt(),
        transaction.getListing() == null ? null : transaction.getListing().getId());
  }
}
