package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.requests.OrderSellRequestDTO;
import footballmarket.controllers.dtos.responses.OrderSellResponseDTO;
import footballmarket.controllers.dtos.responses.TokenListingResponseDTO;
import footballmarket.controllers.dtos.responses.TokenListingSummaryResponseDTO;
import footballmarket.controllers.dtos.responses.TokenListingsResponseDTO;
import footballmarket.models.TokenListing;
import footballmarket.models.records.SellOrderRequest;
import footballmarket.models.records.TokenListingView;
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
