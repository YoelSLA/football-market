import type { MarketListing } from "@/features/market/types";

/**
 * Ordena publicaciones de venta de un jugador por menor precio unitario y fecha de expiración.
 */
export function sortListings(listings: MarketListing[]): MarketListing[] {
	return [...listings].sort(
		(a, b) =>
			a.unitPrice - b.unitPrice ||
			Date.parse(a.expiresAt) - Date.parse(b.expiresAt) ||
			a.listingId - b.listingId,
	);
}
