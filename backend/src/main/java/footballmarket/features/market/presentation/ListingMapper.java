package footballmarket.features.market.presentation;

import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.domain.records.SellOrderRequest;
import footballmarket.features.market.domain.records.TokenListingView;
import footballmarket.features.market.domain.records.UserOrdersPage;
import footballmarket.features.market.domain.records.UserOrdersSummary;
import footballmarket.features.market.presentation.dtos.request.OrderSellRequestDTO;
import footballmarket.features.market.presentation.dtos.response.MarketPaginationResponseDTO;
import footballmarket.features.market.presentation.dtos.response.OrderSellResponseDTO;
import footballmarket.features.market.presentation.dtos.response.TokenListingResponseDTO;
import footballmarket.features.market.presentation.dtos.response.TokenListingSummaryResponseDTO;
import footballmarket.features.market.presentation.dtos.response.TokenListingsResponseDTO;
import footballmarket.features.market.presentation.dtos.response.UserOrdersSummaryResponseDTO;
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
                        listing.expiresAt(),
                        listing.playerName(),
                        listing.teamId(),
                        listing.teamName(),
                        listing.leagueId(),
                        listing.leagueName(),
                        listing.position(),
                        listing.imageUrl(),
                        listing.fallbackImageUrl()))
            .toList());
  }

  public static TokenListingsResponseDTO toPageResponse(UserOrdersPage page) {
    UserOrdersSummary summary = page.summary();
    UserOrdersSummaryResponseDTO summaryDTO =
        summary != null
            ? new UserOrdersSummaryResponseDTO(
                summary.activeOrders(),
                summary.activeTokens(),
                summary.activeValue(),
                summary.soldOrders(),
                summary.soldTokens(),
                summary.soldValue(),
                summary.cancelledOrders(),
                summary.expiredOrders(),
                summary.totalOrders())
            : null;
    MarketPaginationResponseDTO paginationDTO =
        new MarketPaginationResponseDTO(
            page.page(), page.size(), page.totalElements(), page.totalPages());
    return new TokenListingsResponseDTO(
        summaryDTO, toResponses(page.content()).listings(), paginationDTO);
  }

  public static SellOrderRequest toSellRequest(OrderSellRequestDTO request) {
    return new SellOrderRequest(request.playerId(), request.quantity(), request.unitPrice());
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
