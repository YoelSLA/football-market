import type {
	CommunityMarketFilters,
	CommunityMarketPlayer,
	MarketListing,
	MarketPlayer,
} from "@/features/market/types";

const normalize = (value: string) =>
	value
		.normalize("NFD")
		.replace(/\p{Diacritic}/gu, "")
		.toLocaleLowerCase("es-AR")
		.trim();

export function buildCommunityPlayers(
	listings: MarketListing[],
	players: MarketPlayer[],
): CommunityMarketPlayer[] {
	const playersById = new Map(players.map((player) => [player.id, player]));
	const listingsByPlayer = new Map<number, MarketListing[]>();
	for (const listing of listings) {
		const current = listingsByPlayer.get(listing.playerId) ?? [];
		current.push(listing);
		listingsByPlayer.set(listing.playerId, current);
	}
	return [...listingsByPlayer.entries()].flatMap(
		([playerId, playerListings]) => {
			const player = playersById.get(playerId);
			if (!player) return [];
			const availableQuantity = playerListings.reduce((total, listing) => {
				const next = total + listing.quantity;
				if (!Number.isSafeInteger(next))
					throw new Error("INVALID_LISTING_QUANTITY_TOTAL");
				return next;
			}, 0);
			return [
				{
					...player,
					listings: sortListings(playerListings),
					listingCount: playerListings.length,
					availableQuantity,
					minimumUnitPrice: Math.min(
						...playerListings.map((listing) => listing.unitPrice),
					),
				},
			];
		},
	);
}

export function sortListings(listings: MarketListing[]) {
	return [...listings].sort(
		(a, b) =>
			a.unitPrice - b.unitPrice ||
			Date.parse(a.expiresAt) - Date.parse(b.expiresAt) ||
			a.listingId - b.listingId,
	);
}

export function filterCommunityPlayers(
	players: CommunityMarketPlayer[],
	filters: CommunityMarketFilters,
) {
	const search = normalize(filters.search);
	return players
		.filter(
			(player) =>
				!search ||
				normalize(`${player.name} ${player.teamName}`).includes(search),
		)
		.filter(
			(player) =>
				filters.league === "all" || player.leagueName === filters.league,
		)
		.filter(
			(player) =>
				filters.position === "all" || player.position === filters.position,
		)
		.sort((a, b) => {
			if (filters.sort === "quantity")
				return (
					b.availableQuantity - a.availableQuantity ||
					a.name.localeCompare(b.name, "es")
				);
			if (filters.sort === "name")
				return a.name.localeCompare(b.name, "es") || a.id - b.id;
			return (
				a.minimumUnitPrice - b.minimumUnitPrice ||
				a.name.localeCompare(b.name, "es")
			);
		});
}
