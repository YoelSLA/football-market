import { useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import { useCancelListingMutation } from "@/features/market/hooks/mutations";
import { useMyListingsQuery } from "@/features/market/hooks/queries";
import type { UserOrderListing } from "@/features/market/types";
import { useAllPlayersQuery } from "@/features/players";
import { getErrorCode } from "@/infrastructure/http";

export interface EnrichedUserOrder extends UserOrderListing {
	playerName: string;
	teamName: string | null;
	leagueName: string | null;
	imageUrl: string | null;
	position: string | null;
}

export function useMyOrdersPage() {
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

	const { data: playersPage, isPending: isLoadingPlayers } =
		useAllPlayersQuery();

	const allPlayers = playersPage?.players ?? [];

	const playerMap = useMemo(() => {
		const map = new Map<number, (typeof allPlayers)[number]>();
		for (const p of allPlayers) {
			map.set(p.id, p);
		}
		return map;
	}, [allPlayers]);

	const enrichedOrders: EnrichedUserOrder[] = useMemo(() => {
		if (!ordersPage?.listings) return [];
		return ordersPage.listings.map((item) => {
			const player = playerMap.get(item.playerId);
			return {
				...item,
				playerName: player?.name ?? `Jugador #${item.playerId}`,
				teamName: player?.teamName ?? null,
				leagueName: player?.leagueName ?? null,
				imageUrl: player?.imageUrl ?? null,
				position: player?.position ?? null,
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
			toast.success(
				`Publicación #${selectedOrderToCancel.listingId} cancelada. Los ${selectedOrderToCancel.quantity} tokens de ${selectedOrderToCancel.playerName} volvieron a estar disponibles en tu cartera.`,
			);
			closeCancelModal();
		} catch (error: unknown) {
			const code = getErrorCode(error);
			if (code === "LISTING_NOT_AVAILABLE") {
				toast.error(
					"La orden ya no se encuentra disponible para ser cancelada (puede haber sido vendida o expirado).",
				);
			} else {
				toast.error("Ocurrió un error al intentar cancelar la publicación.");
			}
			closeCancelModal();
		}
	};

	return {
		page,
		totalPages: ordersPage?.totalPages ?? 0,
		totalElements: ordersPage?.totalElements ?? 0,
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
