package footballmarket.features.market.application;

import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.domain.records.SellOrderRequest;
import footballmarket.features.market.domain.records.TokenListingView;
import java.util.List;

/** Publicación transaccional de tokens con precio congelado y reservas derivadas. */
public interface TokenListingService {
  /** Publica sin mover créditos ni tokens y sin generar una transacción financiera. */
  TokenListing publish(String sellerEmail, SellOrderRequest order);

  /** Mercado vigente de otros usuarios sobre jugadores visibles. */
  List<TokenListingView> market(String viewerEmail);

  /** Todas las propias; filtro exacto de estado efectivo, independiente de la visibilidad. */
  List<TokenListingView> mine(String viewerEmail, String status);

  /** Detalle en cualquier estado para el dueño; solo vigente y visible para terceros. */
  TokenListingView detail(String viewerEmail, Long listingId);

  /** Cancela bajo bloqueo, solo por su propietario y mientras sea vigente; no toca recursos. */
  TokenListingView cancel(String ownerEmail, Long listingId);
}
