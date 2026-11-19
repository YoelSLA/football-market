import type {
	CommunityMarketFilters,
	CommunityMarketSort,
} from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { sanitizePositions } from "@/features/players";
import {
	CompetitionLogo,
	FilterSearch,
	FilterSelect,
	Slider,
	TeamBadge,
} from "@/shared/components";
import styles from "./MarketFilterFields.module.scss";

interface Props {
	scope: string;
	filters: CommunityMarketFilters;
	leagues: { id: number; name: string }[];
	teams: { id: number; name: string; leagueId: number; leagueName: string }[];
	positions: string[];
	priceRange: { min: number | null; max: number | null };
	onChange: (next: Partial<CommunityMarketFilters>) => void;
	onMaxPriceCommit: (value: number | null) => void;
}

export default function CommunityMarketFilterFields({
	scope,
	filters,
	leagues,
	teams,
	positions,
	priceRange,
	onChange,
	onMaxPriceCommit,
}: Props) {
	const availableTeams = teams.filter(
		(team) => filters.leagueId === null || team.leagueId === filters.leagueId,
	);

	return (
		<fieldset
			className={`${styles.marketFilterFields} ${styles.marketFilterFields_community}`}
		>
			<FilterSearch
				id={`${scope}-search`}
				className={styles.marketFilterFields__search}
				value={filters.search}
				onChange={(value) => onChange({ search: value })}
				label="Buscar jugador o club"
				placeholder="Nombre o club..."
			/>
			<FilterSelect
				id={`${scope}-league`}
				className={styles.marketFilterFields__league}
				label="Liga"
				value={filters.leagueId === null ? "all" : String(filters.leagueId)}
				options={[
					{ value: "all", label: "Todas" },
					...leagues.map((league) => ({
						value: String(league.id),
						label: league.name,
						visual: (
							<CompetitionLogo
								competitionId={league.id}
								competitionName={league.name}
								decorative
							/>
						),
					})),
				]}
				searchable
				searchPlaceholder="Buscar liga…"
				onChange={(value) =>
					onChange({ leagueId: value === "all" ? null : Number(value) })
				}
			/>
			<FilterSelect
				id={`${scope}-team`}
				className={styles.marketFilterFields__team}
				label="Equipo"
				value={filters.teamId === null ? "all" : String(filters.teamId)}
				options={[
					{ value: "all", label: "Todos" },
					...availableTeams.map((team) => ({
						value: String(team.id),
						label: team.name,
						visual: (
							<TeamBadge
								teamId={team.id}
								teamName={team.name}
								leagueId={team.leagueId}
								leagueName={team.leagueName}
							/>
						),
					})),
				]}
				searchable
				searchPlaceholder="Buscar equipo…"
				onChange={(value) =>
					onChange({ teamId: value === "all" ? null : Number(value) })
				}
			/>
			<FilterSelect
				id={`${scope}-position`}
				className={styles.marketFilterFields__position}
				label="Posición"
				value={filters.position ?? "all"}
				options={[
					{ value: "all", label: "Todas" },
					...sanitizePositions(positions).map((position) => ({
						value: position,
						label: position,
					})),
				]}
				onChange={(value) =>
					onChange({ position: value === "all" ? null : value })
				}
			/>
			{priceRange.min !== null && priceRange.max !== null ? (
				<Slider
					id={`${scope}-max-price`}
					className={styles.marketFilterFields__maxPrice}
					variant="single"
					label="Precio máximo por token"
					min={priceRange.min}
					max={priceRange.max}
					step={1}
					value={filters.maxPrice}
					formatValue={formatCredits}
					allowNoLimit
					onCommit={onMaxPriceCommit}
				/>
			) : (
				<div className={styles.marketFilterFields__maxPrice} role="status">
					Precio máximo: sin publicaciones disponibles
				</div>
			)}
			<FilterSelect
				id={`${scope}-sort`}
				className={styles.marketFilterFields__sort}
				label="Orden"
				value={filters.sort}
				options={[
					{ value: "price", label: "Mejor precio" },
					{ value: "quantity", label: "Mayor cantidad ofrecida" },
					{ value: "name", label: "Nombre" },
				]}
				onChange={(value) => onChange({ sort: value as CommunityMarketSort })}
			/>
		</fieldset>
	);
}
