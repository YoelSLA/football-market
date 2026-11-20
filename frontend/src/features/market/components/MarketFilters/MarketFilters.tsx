import type { CommunityMarketFilters as CommunityFilterModel } from "@/features/market/types";
import type { PlayerCatalogFilters } from "@/features/players";
import { Button } from "@/shared/components";
import { SlidersHorizontal } from "@/shared/icons";
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

	const renderFilterFields = () => {
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
					scope="community-market-desktop"
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
					scope="orders-desktop"
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
				scope="market-desktop"
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
		<aside
			className={`${styles.marketFilters} ${
				props.variant === "official"
					? styles.marketFilters_official
					: props.variant === "community"
						? styles.marketFilters_community
						: props.variant === "orders"
							? styles.marketFilters_orders
							: styles.marketFilters_sell
			}`}
			aria-label={
				props.variant === "official"
					? "Filtros del Mercado Oficial"
					: props.variant === "community"
						? "Filtros del Mercado P2P"
						: props.variant === "orders"
							? "Filtros de Mis Órdenes"
							: "Filtros de Publicar venta"
			}
			aria-busy={props.isUpdating ? "true" : undefined}
		>
			<div className={styles.marketFilters__desktop}>
				<div className={styles.marketFilters__heading}>
					<h2>
						<SlidersHorizontal aria-hidden="true" />
						Filtros
					</h2>
					{props.summary && (
						<span className={styles.marketFilters__summary}>
							{props.summary}
						</span>
					)}

					<Button
						type="button"
						className={styles.marketFilters__clear}
						disabled={isCatalog ? false : !props.active}
						onClick={props.onClear}
					>
						Limpiar filtros
					</Button>
				</div>

				{renderFilterFields()}
			</div>
		</aside>
	);
}
