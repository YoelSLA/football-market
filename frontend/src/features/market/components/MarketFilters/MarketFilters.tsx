import type { PlayerCatalogFilters } from "@/features/players";
import { Button } from "@/shared/components";
import { SlidersHorizontal } from "@/shared/icons";
import {
	OfficialMarketFilterFields,
	SellMarketFilterFields,
} from "./MarketFilterFields";
import styles from "./MarketFilters.module.scss";

interface BaseProps {
	onClear: () => void;
	isUpdating?: boolean;
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
	minPrice: string;
	maxPrice: string;
	rangeError: string | null;
	onSearchChange: (value: string) => void;
	onLeagueChange: (value: string) => void;
	onTeamChange: (value: string) => void;
	onPositionChange: (value: string) => void;
	onNationalityChange: (value: string) => void;
	onSortChange: (value: string) => void;
	onMinPriceChange: (value: string) => void;
	onMaxPriceChange: (value: string) => void;
}

interface SellProps extends BaseProps {
	variant?: "sell";
	active: boolean;
	leagues?: string[];
	positions?: string[];
	search?: string;
	league?: string;
	position?: string;
	availability?: string;
	sort?: string;
	onSearchChange?: (value: string) => void;
	onLeagueChange?: (value: string) => void;
	onPositionChange?: (value: string) => void;
	onAvailabilityChange?: (value: string) => void;
	onSortChange?: (value: string) => void;
	onApply?: () => void;
}

type Props = OfficialProps | SellProps;

export default function MarketFilters(props: Props) {
	const isOfficial = props.variant === "official";
	const renderFilterFields = (scope: string) => {
		if (props.variant === "official")
			return (
				<OfficialMarketFilterFields
					scope={scope}
					options={props.officialOptions}
					search={props.search}
					league={props.league}
					team={props.team}
					position={props.position}
					nationality={props.nationality}
					sort={props.sort}
					minPrice={props.minPrice}
					maxPrice={props.maxPrice}
					rangeError={props.rangeError}
					onSearchChange={props.onSearchChange}
					onLeagueChange={props.onLeagueChange}
					onTeamChange={props.onTeamChange}
					onPositionChange={props.onPositionChange}
					onNationalityChange={props.onNationalityChange}
					onSortChange={props.onSortChange}
					onMinPriceChange={props.onMinPriceChange}
					onMaxPriceChange={props.onMaxPriceChange}
				/>
			);
		return (
			<SellMarketFilterFields
				scope={scope}
				active={props.active}
				leagues={props.leagues}
				positions={props.positions}
				search={props.search}
				league={props.league}
				position={props.position}
				availability={props.availability}
				sort={props.sort}
				onSearchChange={props.onSearchChange}
				onLeagueChange={props.onLeagueChange}
				onPositionChange={props.onPositionChange}
				onAvailabilityChange={props.onAvailabilityChange}
				onSortChange={props.onSortChange}
				onApply={props.onApply}
			/>
		);
	};

	return (
		<aside
			className={`${styles.marketFilters} ${isOfficial ? styles.marketFilters_official : styles.marketFilters_sell}`}
			aria-label={
				isOfficial ? "Filtros del Mercado Oficial" : "Filtros del mercado"
			}
			aria-busy={props.isUpdating ? "true" : undefined}
		>
			<div className={styles.marketFilters__desktop}>
				<div className={styles.marketFilters__heading}>
					<h2>
						<SlidersHorizontal aria-hidden="true" /> Filtros
					</h2>
					<Button
						type="button"
						className={styles.marketFilters__clear}
						disabled={isOfficial ? false : !props.active}
						onClick={props.onClear}
					>
						Limpiar filtros
					</Button>
				</div>
				{renderFilterFields("market-desktop")}
			</div>

			<details className={styles.marketFilters__mobile}>
				<summary>
					<SlidersHorizontal aria-hidden="true" /> Filtros
				</summary>
				<div className={styles.marketFilters__popover}>
					<div className={styles.marketFilters__heading}>
						<h2>Filtros</h2>
						<Button
							type="button"
							className={styles.marketFilters__clear}
							disabled={isOfficial ? false : !props.active}
							onClick={props.onClear}
						>
							Limpiar filtros
						</Button>
					</div>
					{renderFilterFields("market-mobile")}
				</div>
			</details>
		</aside>
	);
}
