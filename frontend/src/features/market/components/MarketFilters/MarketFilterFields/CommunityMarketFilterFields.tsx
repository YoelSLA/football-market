import type {
	CommunityMarketFilters,
	CommunityMarketSort,
} from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { sanitizePositions } from "@/features/players";
import {
	CompetitionLogo,
	FilterPopover,
	FilterSearch,
	FilterSelect,
	Slider,
	TeamBadge,
} from "@/shared/components";
import { TOKEN_SYMBOL } from "@/shared/constants";
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

const COMMUNITY_SORT_OPTIONS = [
	{ value: "price", label: "Mejor precio" },
	{ value: "quantity", label: "Mayor cantidad ofrecida" },
	{ value: "name", label: "Nombre" },
];

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

	const popoverActiveCount = [
		filters.teamId !== null,
		filters.maxPrice !== null,
		filters.sort !== "price",
	].filter(Boolean).length;

	const handleClearPopover = () => {
		onChange({ teamId: null, sort: "price" });
		onMaxPriceCommit(null);
	};

	return (
		<div className={styles.fields}>
			<FilterSearch
				id={`${scope}-search`}
				className={styles.search}
				value={filters.search}
				onChange={(value) => onChange({ search: value })}
				label="Buscar jugador o club"
				placeholder="Buscar jugador o club…"
			/>
			<FilterSelect
				id={`${scope}-league`}
				className={styles.select}
				label="Liga"
				value={filters.leagueId === null ? "all" : String(filters.leagueId)}
				options={[
					{ value: "all", label: "Todas las ligas" },
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
				id={`${scope}-position`}
				className={styles.select}
				label="Posición"
				value={filters.position ?? "all"}
				options={[
					{ value: "all", label: "Todas las posiciones" },
					...sanitizePositions(positions).map((position) => ({
						value: position,
						label: position,
					})),
				]}
				onChange={(value) =>
					onChange({ position: value === "all" ? null : value })
				}
			/>
			<FilterPopover
				triggerLabel="Más filtros"
				title="Filtros avanzados"
				activeCount={popoverActiveCount}
				onClear={handleClearPopover}
				panelWidth="md"
			>
				<FilterSelect
					id={`${scope}-team`}
					className={styles.popoverField}
					label="Equipo"
					value={filters.teamId === null ? "all" : String(filters.teamId)}
					options={[
						{ value: "all", label: "Todos los equipos" },
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
				{priceRange.min !== null && priceRange.max !== null ? (
					<div className={styles.popoverSlider}>
						<Slider
							id={`${scope}-max-price`}
							variant="single"
							label={`Precio máximo por ${TOKEN_SYMBOL}`}
							min={priceRange.min}
							max={priceRange.max}
							step={1}
							value={filters.maxPrice}
							formatValue={formatCredits}
							allowNoLimit
							onCommit={onMaxPriceCommit}
						/>
					</div>
				) : (
					<div className={styles.popoverStatus} role="status">
						Precio máximo: sin publicaciones disponibles
					</div>
				)}
				<FilterSelect
					id={`${scope}-sort`}
					className={styles.popoverField}
					label="Orden"
					value={filters.sort}
					options={COMMUNITY_SORT_OPTIONS}
					onChange={(value) => onChange({ sort: value as CommunityMarketSort })}
				/>
			</FilterPopover>
		</div>
	);
}
