import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { useTransactionHistoryQuery } from "@/features/portfolio/hooks/queries";
import {
	type TransactionHistoryQueryParams,
	TransactionHistorySort,
	TransactionType,
} from "@/features/portfolio/types";
import { getErrorMessage } from "@/infrastructure/http";

const PAGE_SIZE = 20;

function parseNonNegative(value: string | null): number {
	const parsed = Number(value);
	return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
}

function parseType(value: string | null): TransactionType | undefined {
	return value === TransactionType.BUY || value === TransactionType.SELL
		? value
		: undefined;
}

function parseSort(value: string | null): TransactionHistorySort {
	return Object.values(TransactionHistorySort).includes(
		value as TransactionHistorySort,
	)
		? (value as TransactionHistorySort)
		: TransactionHistorySort.NEWEST;
}

function parseDate(value: string | null): string {
	if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return "";
	const date = new Date(`${value}T00:00:00`);
	return Number.isNaN(date.getTime()) ? "" : value;
}

function startOfLocalDay(value: string): string {
	return new Date(`${value}T00:00:00`).toISOString();
}

function startOfNextLocalDay(value: string): string {
	const date = new Date(`${value}T00:00:00`);
	date.setDate(date.getDate() + 1);
	return date.toISOString();
}

export function useTransactionHistoryPage() {
	const [searchParams, setSearchParams] = useSearchParams();
	const page = parseNonNegative(searchParams.get("page"));
	const search = searchParams.get("search")?.trim() ?? "";
	const type = parseType(searchParams.get("type"));
	const from = parseDate(searchParams.get("from"));
	const to = parseDate(searchParams.get("to"));
	const sort = parseSort(searchParams.get("sort"));

	const params: TransactionHistoryQueryParams = {
		page,
		size: PAGE_SIZE,
		sort,
		...(search ? { search } : {}),
		...(type ? { type } : {}),
		...(from ? { executedFrom: startOfLocalDay(from) } : {}),
		...(to ? { executedBefore: startOfNextLocalDay(to) } : {}),
	};
	const query = useTransactionHistoryQuery(params);
	const errorMessage = query.isError
		? getErrorMessage(query.error, "No pudimos cargar tu historial.")
		: null;
	const hasFilters = Boolean(search || type || from || to);

	useEffect(() => {
		if (!query.data || query.isFetching) return;
		const lastPage = Math.max(query.data.pagination.totalPages - 1, 0);
		if (page > lastPage) {
			setSearchParams((current) => {
				const next = new URLSearchParams(current);
				if (lastPage > 0) next.set("page", String(lastPage));
				else next.delete("page");
				return next;
			});
		}
	}, [page, query.data, query.isFetching, setSearchParams]);

	function updateCriterion(key: string, value: string) {
		setSearchParams((current) => {
			const next = new URLSearchParams(current);
			if (value) next.set(key, value);
			else next.delete(key);
			next.delete("page");
			return next;
		});
	}

	function selectPage(nextPage: number) {
		setSearchParams((current) => {
			const next = new URLSearchParams(current);
			if (nextPage > 0) next.set("page", String(nextPage));
			else next.delete("page");
			return next;
		});
	}

	function clearFilters() {
		setSearchParams((current) => {
			const next = new URLSearchParams();
			const currentSort = current.get("sort");
			if (currentSort && currentSort !== TransactionHistorySort.NEWEST) {
				next.set("sort", currentSort);
			}
			return next;
		});
	}

	return {
		data: query.data,
		isLoading: query.isPending,
		isRefreshing: query.isFetching && !query.isPending,
		errorMessage,
		retry: query.refetch,
		criteria: { page, search, type, from, to, sort },
		hasFilters,
		updateCriterion,
		selectPage,
		clearFilters,
	};
}
