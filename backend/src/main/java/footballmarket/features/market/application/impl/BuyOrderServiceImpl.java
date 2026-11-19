package footballmarket.features.market.application.impl;

import footballmarket.features.market.application.BuyOrderService;
import footballmarket.features.market.application.internal.OfficialBuyOrderExecutor;
import footballmarket.features.market.application.internal.P2PBuyOrderExecutor;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.records.BuyListingRequest;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación transaccional de la compra de tokens en el mercado.
 *
 * <p>Constituye la fachada de aplicación transaccional del servicio {@link BuyOrderService}.
 * Ejecuta cada compra dentro de una única transacción de Spring y delega la ejecución especializada
 * en los ejecutores internos {@link OfficialBuyOrderExecutor} (para la emisión primaria oficial) y
 * {@link P2PBuyOrderExecutor} (para la liquidación secundaria comunitaria).
 *
 * <p>Cualquier excepción de negocio o del sistema produce la reversión atómica de toda la
 * operación, impidiendo movimientos parciales de fondos o tenencias no auditadas.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BuyOrderServiceImpl implements BuyOrderService {

  private final OfficialBuyOrderExecutor officialBuyOrderExecutor;
  private final P2PBuyOrderExecutor p2pBuyOrderExecutor;

  /** {@inheritDoc} */
  @Override
  public MarketTransaction buy(String buyerEmail, BuyOrderRequest order) {
    return this.officialBuyOrderExecutor.execute(buyerEmail, order);
  }

  /** {@inheritDoc} */
  @Override
  public MarketTransaction buyListing(String buyerEmail, BuyListingRequest order) {
    return this.p2pBuyOrderExecutor.execute(buyerEmail, order);
  }
}
