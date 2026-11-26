package footballmarket.features.market.application.impl;

import footballmarket.common.config.QueryCacheConfiguration;
import footballmarket.common.config.QueryCacheInvalidationEvent;
import footballmarket.features.market.application.OfficialBuybackService;
import footballmarket.features.market.application.internal.OfficialBuybackExecutor;
import footballmarket.features.market.domain.records.OfficialBuybackReceipt;
import footballmarket.features.market.domain.records.OfficialBuybackRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación transaccional de la venta oficial inmediata.
 *
 * <p>Delega la liquidación en {@link OfficialBuybackExecutor} dentro de una única transacción: un
 * fallo de negocio o del sistema revierte créditos, tenencias y registro financiero.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OfficialBuybackServiceImpl implements OfficialBuybackService {

  private final OfficialBuybackExecutor officialBuybackExecutor;
  private final ApplicationEventPublisher events;

  /** {@inheritDoc} */
  @Override
  public OfficialBuybackReceipt sellToSuperuser(String sellerEmail, OfficialBuybackRequest order) {
    OfficialBuybackReceipt receipt = this.officialBuybackExecutor.execute(sellerEmail, order);
    this.events.publishEvent(
        new QueryCacheInvalidationEvent(java.util.Set.of(QueryCacheConfiguration.OFFICIAL_MARKET)));
    return receipt;
  }
}
