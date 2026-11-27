import { useQueries } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useCurrentUserQuery } from "@/features/auth";
import { useCancelListingMutation } from "@/features/market/hooks/mutations";
import { useMyListingsQuery } from "@/features/market/hooks/queries";
import { useMarketToast } from "@/features/market/hooks/useMarketToast";
import type { UserOrderListing } from "@/features/market/types";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";
import type { PlayerDetail } from "@/features/players/types";
import { getErrorCode } from "@/infrastructure/http";
import { useAuthStore } from "@/infrastructure/storage";

export interface EnrichedUserOrder extends UserOrderListing {
	playerName: string;
	teamId: number | null;
	teamName: string | null;
	leagueId: number | null;
	leagueName: string | null;
	imageUrl: string | null;
	fallbackImageUrl: string | null;
	position: string | null;
}

export function useMyOrdersPage() {
	const revision = useAuthStore((state) => state.revision);
	const currentUser = useCurrentUserQuery(true, revision);
	const availableCredits = currentUser.data?.availableCredits;

	const [searchParams, setSearchParams] = useSearchParams();

	const page = Number(searchParams.get("page") ?? "0") || 0;
	const statusParam = searchParams.get("status") ?? "";
	const validStatuses = ["ACTIVE", "SOLD", "CANCELLED", "EXPIRED"];
	const status = validStatuses.includes(statusParam) ? statusParam : "";

	const sortParam = searchParams.get("sort") ?? "";
	const validSorts = ["NEWEST", "OLDEST", "PRICE_DESC", "PRICE_ASC"];
	const sort = validSorts.includes(sortParam) ? sortParam : "NEWEST";

	const [searchInput, setSearchInput] = useState(
		searchParams.get("search") ?? "",
	);

	const updateParams = (updates: Record<string, string | null>) => {
		const next = new URLSearchParams(searchParams);
		for (const [key, value] of Object.entries(updates)) {
			if (value === null || value === "") {
				next.delete(key);
			} else {
				next.set(key, value);
			}
		}
		setSearchParams(next);
	};

	const setPage = (newPage: number) => {
		updateParams({ page: newPage > 0 ? String(newPage) : null });
	};

	const setStatus = (newStatus: string) => {
		updateParams({
			status: newStatus || null,
			page: null,
		});
	};

	const setSort = (newSort: string) => {
		updateParams({
			sort: newSort && newSort !== "NEWEST" ? newSort : null,
			page: null,
		});
	};

	const handleSearchChange = (value: string) => {
		setSearchInput(value);
		updateParams({
			search: value.trim() ? value.trim() : null,
			page: null,
		});
	};

	const clearFilters = () => {
		setSearchInput("");
		setSearchParams(new URLSearchParams());
	};

	const {
		data: ordersPage,
		isLoading: isLoadingOrders,
		isError: isOrdersError,
		refetch: refetchOrders,
	} = useMyListingsQuery({
		page,
		size: 10,
		status: status || undefined,
		sort,
	});

	const playerIds = useMemo(() => {
		if (!ordersPage?.listings) return [];
		return Array.from(
			new Set(ordersPage.listings.map((item) => item.playerId)),
		);
	}, [ordersPage?.listings]);

	const playerQueries = useQueries({
		queries: playerIds.map((id) => ({
			queryKey: playersQueryKeys.detail(id),
			queryFn: ({ signal }: { signal: AbortSignal }) =>
				playersService.getById(id, signal),
			staleTime: 5 * 60 * 1000,
		})),
	});

	const playerMap = useMemo(() => {
		const map = new Map<number, PlayerDetail>();
		for (const q of playerQueries) {
			if (q.data) {
				map.set(q.data.id, q.data);
			}
		}
		return map;
	}, [playerQueries]);

	const isLoadingPlayers =
		playerIds.length > 0 && playerQueries.some((q) => q.isPending);

	const enrichedOrders: EnrichedUserOrder[] = useMemo(() => {
		if (!ordersPage?.listings) return [];
		return ordersPage.listings.map((item) => {
			const player = playerMap.get(item.playerId);
			return {
				...item,
				playerName:
					item.playerName ?? player?.name ?? `Jugador #${item.playerId}`,
				teamId: item.teamId ?? player?.teamId ?? null,
				teamName: item.teamName ?? player?.teamName ?? null,
				leagueId: item.leagueId ?? player?.leagueId ?? null,
				leagueName: item.leagueName ?? player?.leagueName ?? null,
				imageUrl: item.imageUrl ?? player?.imageUrl ?? null,
				fallbackImageUrl:
					item.fallbackImageUrl ?? player?.fallbackImageUrl ?? null,
				position: item.position ?? player?.position ?? null,
			};
		});
	}, [ordersPage?.listings, playerMap]);

	const filteredOrders = useMemo(() => {
		const query = searchInput.trim().toLowerCase();
		if (!query) return enrichedOrders;
		return enrichedOrders.filter(
			(order) =>
				order.playerName.toLowerCase().includes(query) ||
				(order.teamName?.toLowerCase().includes(query) ?? false),
		);
	}, [enrichedOrders, searchInput]);

	// Modal de cancelación
	const [selectedOrderToCancel, setSelectedOrderToCancel] =
		useState<EnrichedUserOrder | null>(null);
	const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);

	const cancelMutation = useCancelListingMutation();
	const { notifyCancelSuccess, notifyError } = useMarketToast();

	const openCancelModal = (order: EnrichedUserOrder) => {
		setSelectedOrderToCancel(order);
		setIsCancelModalOpen(true);
	};

	const closeCancelModal = () => {
		setIsCancelModalOpen(false);
		setSelectedOrderToCancel(null);
	};

	const confirmCancel = async () => {
		if (!selectedOrderToCancel) return;
		try {
			await cancelMutation.mutateAsync(selectedOrderToCancel.listingId);
			notifyCancelSuccess(
				selectedOrderToCancel.listingId,
				selectedOrderToCancel.playerName,
				selectedOrderToCancel.quantity,
			);
			closeCancelModal();
		} catch (error: unknown) {
			const code = getErrorCode(error);
			if (code === "LISTING_NOT_AVAILABLE") {
				notifyError(
					"La orden ya no se encuentra disponible para ser cancelada (puede haber sido vendida o expirado).",
				);
			} else {
				notifyError("Ocurrió un error al intentar cancelar la publicación.");
			}
			closeCancelModal();
		}
	};

	return {
		page,
		totalPages: ordersPage?.totalPages ?? 0,
		totalElements: ordersPage?.totalElements ?? 0,
		availableCredits,
		summary: ordersPage?.summary ?? {
			activeOrders: 0,
			activeTokens: 0,
			activeValue: 0,
			soldOrders: 0,
			soldTokens: 0,
			soldValue: 0,
			cancelledOrders: 0,
			expiredOrders: 0,
			totalOrders: 0,
		},
		orders: filteredOrders,
		status,
		sort,
		searchInput,
		isLoading: isLoadingOrders || isLoadingPlayers,
		isError: isOrdersError,
		refetch: refetchOrders,
		setPage,
		setStatus,
		setSort,
		setSearchInput: handleSearchChange,
		clearFilters,
		// Cancel modal
		selectedOrderToCancel,
		isCancelModalOpen,
		isCancelling: cancelMutation.isPending,
		openCancelModal,
		closeCancelModal,
		confirmCancel,
	};
}
