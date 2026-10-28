package footballmarket.features.market.presentation;

import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.domain.records.SellOrderRequest;
import footballmarket.features.market.domain.records.TokenListingView;
import footballmarket.features.market.presentation.dtos.request.OrderSellRequestDTO;
import footballmarket.features.market.presentation.dtos.response.OrderSellResponseDTO;
import footballmarket.features.market.presentation.dtos.response.TokenListingResponseDTO;
import footballmarket.features.market.presentation.dtos.response.TokenListingSummaryResponseDTO;
import footballmarket.features.market.presentation.dtos.response.TokenListingsResponseDTO;
import java.util.List;

/** Conversiones estructurales entre DTO HTTP y publicaciones del dominio, sin reglas de negocio. */
public final class ListingMapper {
  private ListingMapper() {}

  public static TokenListingResponseDTO toResponse(TokenListingView listing) {
    return new TokenListingResponseDTO(
        listing.listingId(),
        listing.sellerId(),
        listing.playerId(),
        listing.quantity(),
        listing.unitPrice(),
        listing.totalAmount(),
        listing.status().name(),
        listing.createdAt(),
        listing.expiresAt(),
        listing.closedAt());
  }

  public static TokenListingsResponseDTO toResponses(List<TokenListingView> listings) {
    return new TokenListingsResponseDTO(
        listings.stream()
            .map(
                listing ->
                    new TokenListingSummaryResponseDTO(
                        listing.listingId(),
                        listing.sellerId(),
                        listing.playerId(),
                        listing.quantity(),
                        listing.unitPrice(),
                        listing.totalAmount(),
                        listing.status().name(),
                        listing.createdAt(),
                        listing.expiresAt()))
            .toList());
  }

  public static SellOrderRequest toSellRequest(OrderSellRequestDTO request) {
    return new SellOrderRequest(request.playerId(), request.quantity());
  }

  public static OrderSellResponseDTO toSellResponse(TokenListing listing) {
    return new OrderSellResponseDTO(
        listing.getId(),
        listing.getPlayer().getId(),
        listing.getQuantity(),
        listing.getUnitPrice(),
        listing.getTotalAmount(),
        listing.getStatus().name(),
        listing.getCreatedAt(),
        listing.getExpiresAt());
  }
}
