import type { ReactNode } from "react";
import type { usePlayerRankingsPage } from "@/features/players/hooks";
import RankingsEmpty from "./RankingsEmpty";
import RankingsError from "./RankingsError";
import RankingsFilterEmpty from "./RankingsFilterEmpty";
import RankingsLoading from "./RankingsLoading";

type RankingsPageData = ReturnType<typeof usePlayerRankingsPage>;

interface Props {
	children: ReactNode;
	status: RankingsPageData["status"];
	mode: RankingsPageData["mode"];
	isGlobalEmpty: boolean;
	isFilterEmpty: boolean;
	hasActiveFilters: boolean;
	onClearFilters: () => void;
}

export default function RankingsStatus({
	children,
	status,
	mode,
	isGlobalEmpty,
	isFilterEmpty,
	hasActiveFilters,
	onClearFilters,
}: Props) {
	if (status.isError) {
		return <RankingsError error={status.error} onRetry={status.retry} />;
	}

	if (status.isLoading) {
		return <RankingsLoading />;
	}

	if (isGlobalEmpty) {
		return <RankingsEmpty mode={mode} />;
	}

	return (
		<>
			{children}

			{isFilterEmpty && (
				<RankingsFilterEmpty
					hasActiveFilters={hasActiveFilters}
					onClearFilters={onClearFilters}
				/>
			)}
		</>
	);
}
