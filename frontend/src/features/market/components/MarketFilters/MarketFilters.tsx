import { Button } from "@/shared/components";
import { SlidersHorizontal } from "@/shared/icons";
import MarketFilterFields from "./MarketFilterFields";
import styles from "./MarketFilters.module.scss";

interface Props {
	active?: boolean;
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
	onClear?: () => void;
}

export default function MarketFilters(props: Props) {
	const renderFilterFields = (scope: string) => (
		<MarketFilterFields
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

	return (
		<aside
			className={styles.marketFilters}
			aria-label={
				props.active
					? "Filtros del mercado"
					: "Filtros del mercado, pendientes de implementación"
			}
		>
			<div className={styles.marketFilters__desktop}>
				<div className={styles.marketFilters__heading}>
					<h2>
						<SlidersHorizontal aria-hidden="true" /> Filtros
					</h2>
					<Button
						type="button"
						className={styles.marketFilters__clear}
						disabled={!props.active}
						onClick={props.onClear}
					>
						Limpiar filtros
					</Button>
				</div>
				{!props.active && (
					<p className={styles.marketFilters__pending}>
						Pendiente · controles solo visuales
					</p>
				)}
				{renderFilterFields("market-desktop")}
			</div>

			<details className={styles.marketFilters__mobile}>
				<summary>
					<SlidersHorizontal aria-hidden="true" /> Filtros{" "}
					{!props.active && <span>Pendientes · desplegar</span>}
				</summary>
				<div className={styles.marketFilters__popover}>
					<div className={styles.marketFilters__heading}>
						<h2>Filtros</h2>
						<Button
							type="button"
							className={styles.marketFilters__clear}
							disabled={!props.active}
							onClick={props.onClear}
						>
							Limpiar filtros
						</Button>
					</div>
					{!props.active && (
						<p className={styles.marketFilters__pending}>
							Pendiente · controles solo visuales
						</p>
					)}
					{renderFilterFields("market-mobile")}
				</div>
			</details>
		</aside>
	);
}
