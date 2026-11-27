import type { CommunityMarketFilters as CommunityFilterModel } from "@/features/market/types";
import type { PlayerCatalogFilters } from "@/features/players";
import { X } from "@/shared/icons";
import {
	CommunityMarketFilterFields,
	OfficialMarketFilterFields,
	OrdersMarketFilterFields,
	SellMarketFilterFields,
} from "./MarketFilterFields";
import styles from "./MarketFilters.module.scss";

interface BaseProps {
	onClear: () => void;
	isUpdating?: boolean;
	summary?: string;
}

interface OfficialProps extends BaseProps {
	variant: "official";
	officialOptions?: PlayerCatalogFilters;
	search: string;
	league: string;
	team: string;
	position: string;
	nationality: string;
	sort: string;
	priceRange: { min: number | null; max: number | null };
	selectedPriceRange: readonly [number, number] | null;
	onSearchChange: (value: string) => void;
	onLeagueChange: (value: string) => void;
	onTeamChange: (value: string) => void;
	onPositionChange: (value: string) => void;
	onNationalityChange: (value: string) => void;
	onSortChange: (value: string) => void;
	onPriceRangeCommit: (value: readonly [number, number]) => void;
}

interface CommunityProps extends BaseProps {
	variant: "community";
	filters: CommunityFilterModel;
	leagues: { id: number; name: string }[];
	teams: { id: number; name: string; leagueId: number; leagueName: string }[];
	positions: string[];
	priceRange: { min: number | null; max: number | null };
	onChange: (next: Partial<CommunityFilterModel>) => void;
	onMaxPriceCommit: (value: number | null) => void;
}

interface SellProps extends BaseProps {
	variant?: "sell";
	active: boolean;
	options?: PlayerCatalogFilters;
	leagues?: string[];
	positions?: string[];
	search?: string;
	league?: string;
	team?: string;
	position?: string;
	availability?: string;
	sort?: string;
	onSearchChange?: (value: string) => void;
	onLeagueChange?: (value: string) => void;
	onTeamChange?: (value: string) => void;
	onPositionChange?: (value: string) => void;
	onAvailabilityChange?: (value: string) => void;
	onSortChange?: (value: string) => void;
	onApply?: () => void;
}

interface OrdersProps extends BaseProps {
	variant: "orders";
	search: string;
	status: string;
	sort: string;
	onSearchChange: (value: string) => void;
	onStatusChange: (value: string) => void;
	onSortChange: (value: string) => void;
}

type Props = OfficialProps | CommunityProps | SellProps | OrdersProps;

export default function MarketFilters(props: Props) {
	const isCatalog =
		props.variant === "official" ||
		props.variant === "community" ||
		props.variant === "orders" ||
		props.variant === "sell";

	const hasActiveFilters = (() => {
		if (props.variant === "official") {
			const isPriceActive = Boolean(
				props.selectedPriceRange &&
					props.priceRange.min !== null &&
					props.priceRange.max !== null &&
					(props.selectedPriceRange[0] > props.priceRange.min ||
						props.selectedPriceRange[1] < props.priceRange.max),
			);
			return (
				props.search.trim() !== "" ||
				props.league !== "all" ||
				props.team !== "all" ||
				props.position !== "all" ||
				props.nationality !== "all" ||
				props.sort !== "id,asc" ||
				isPriceActive
			);
		}
		if (props.variant === "community") {
			return (
				props.filters.search.trim() !== "" ||
				props.filters.leagueId !== null ||
				props.filters.teamId !== null ||
				props.filters.position !== null ||
				props.filters.maxPrice !== null ||
				props.filters.sort !== "price"
			);
		}
		if (props.variant === "sell") {
			return (
				Boolean((props.search ?? "").trim()) ||
				(props.league && props.league !== "all") ||
				(props.team && props.team !== "all") ||
				(props.position && props.position !== "all") ||
				(props.availability && props.availability !== "all") ||
				Boolean(props.sort && props.sort !== "name,asc")
			);
		}
		if (props.variant === "orders") {
			return (
				props.search.trim() !== "" ||
				(props.status && props.status !== "all" && props.status !== "ALL") ||
				(props.sort && props.sort !== "NEWEST")
			);
		}
		return false;
	})();

	const ariaLabel =
		props.variant === "official"
			? "Filtros del Mercado Oficial"
			: props.variant === "community"
				? "Filtros del Mercado P2P"
				: props.variant === "orders"
					? "Filtros de Mis Órdenes"
					: "Filtros de Publicar venta";

	const renderFields = () => {
		if (props.variant === "official") {
			return (
				<OfficialMarketFilterFields
					scope="market-desktop"
					options={props.officialOptions}
					search={props.search}
					league={props.league}
					team={props.team}
					position={props.position}
					nationality={props.nationality}
					sort={props.sort}
					priceRange={props.priceRange}
					selectedPriceRange={props.selectedPriceRange}
					onSearchChange={props.onSearchChange}
					onLeagueChange={props.onLeagueChange}
					onTeamChange={props.onTeamChange}
					onPositionChange={props.onPositionChange}
					onNationalityChange={props.onNationalityChange}
					onSortChange={props.onSortChange}
					onPriceRangeCommit={props.onPriceRangeCommit}
				/>
			);
		}

		if (props.variant === "community") {
			return (
				<CommunityMarketFilterFields
					scope="community-market"
					filters={props.filters}
					leagues={props.leagues}
					teams={props.teams}
					positions={props.positions}
					priceRange={props.priceRange}
					onChange={props.onChange}
					onMaxPriceCommit={props.onMaxPriceCommit}
				/>
			);
		}

		if (props.variant === "orders") {
			return (
				<OrdersMarketFilterFields
					scope="orders-market"
					search={props.search}
					status={props.status}
					sort={props.sort}
					onSearchChange={props.onSearchChange}
					onStatusChange={props.onStatusChange}
					onSortChange={props.onSortChange}
				/>
			);
		}

		return (
			<SellMarketFilterFields
				scope="sell-market"
				active={props.active}
				options={props.options}
				leagues={props.leagues}
				positions={props.positions}
				search={props.search}
				league={props.league}
				team={props.team}
				position={props.position}
				availability={props.availability}
				sort={props.sort}
				onSearchChange={props.onSearchChange}
				onLeagueChange={props.onLeagueChange}
				onTeamChange={props.onTeamChange}
				onPositionChange={props.onPositionChange}
				onAvailabilityChange={props.onAvailabilityChange}
				onSortChange={props.onSortChange}
				onApply={props.onApply}
			/>
		);
	};

	return (
		<search
			className={styles.toolbar}
			aria-label={ariaLabel}
			aria-busy={props.isUpdating ? "true" : undefined}
		>
			<div className={styles.toolbar__left}>{renderFields()}</div>
			<div className={styles.toolbar__right}>
				{props.summary && (
					<span className={styles.toolbar__summary}>{props.summary}</span>
				)}
				{hasActiveFilters && (
					<button
						type="button"
						className={styles.toolbar__clear}
						disabled={isCatalog ? false : !props.active}
						onClick={props.onClear}
						title="Restablecer filtros"
					>
						<X aria-hidden="true" />
						<span>Limpiar</span>
					</button>
				)}
			</div>
		</search>
	);
}
