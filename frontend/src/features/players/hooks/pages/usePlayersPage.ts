import { useEffect, useState } from "react";
import { getErrorMessage } from "@/infrastructure/http";
import { usePlayersQuery } from "../queries";

export function usePlayersPage() {
	const [selectedPage, setSelectedPage] = useState(0);
	const [searchText, setSearchText] = useState("");
	const [selectedLeague, setSelectedLeague] = useState("");
	const [selectedPosition, setSelectedPosition] = useState("");
	const query = usePlayersQuery(selectedPage);
	const isLoading = query.isPending || query.isFetching;
	const errorMessage = query.isError
		? getErrorMessage(
				query.error,
				"No pudimos cargar los jugadores. Vuelve a intentarlo.",
			)
		: null;
	const playersPage = isLoading || errorMessage ? undefined : query.data;

	useEffect(() => {
		const totalPages = query.data?.totalPages;
		if (totalPages && selectedPage >= totalPages)
			setSelectedPage(totalPages - 1);
	}, [query.data?.totalPages, selectedPage]);

	function selectPage(page: number) {
		const totalPages = query.data?.totalPages ?? 0;
		if (page >= 0 && page < totalPages) setSelectedPage(page);
	}

	return {
		playersPage,
		selectedPage,
		selectPage,
		searchText,
		setSearchText,
		selectedLeague,
		setSelectedLeague,
		selectedPosition,
		setSelectedPosition,
		isLoading,
		errorMessage,
		retry: query.refetch,
	};
}
