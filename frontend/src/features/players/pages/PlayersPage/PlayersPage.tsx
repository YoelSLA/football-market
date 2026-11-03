import { PaginatedPageLayout } from "@/app/layouts";
import { usePlayersPage } from "@/features/players/hooks";
import { Search } from "@/shared/icons";
import PlayerCard from "./PlayerCard";
import PlayerCatalogControls from "./PlayerCatalogControls";
import styles from "./PlayersPage.module.scss";

export default function PlayersPage() {
	const page = usePlayersPage();
	const players = page.playersPage?.players ?? [];
	const header = (
		<div className={styles.playersPage__toolbar}>
			<div className={styles.playersPage__heading}>
				<h1>Catálogo de jugadores</h1>
				{page.playersPage && (
					<span>{page.playersPage.totalElements} jugadores activos</span>
				)}
				<label className={styles.playersPage__search}>
					<Search size={18} aria-hidden="true" />
					<input
						type="search"
						aria-label="Buscar jugador por nombre"
						placeholder="Buscar jugador…"
						value={page.searchText}
						onChange={(event) => page.setSearchText(event.target.value)}
						maxLength={255}
					/>
				</label>
			</div>
			<PlayerCatalogControls
				criteria={page.criteria}
				options={page.options}
				disabled={page.optionsLoading || !page.options}
				onChange={page.changeFilter}
				onClear={page.clearFilters}
			/>
			{page.optionsError && (
				<div role="alert">
					{page.optionsError}{" "}
					<button type="button" onClick={() => page.retryOptions()}>
						Reintentar filtros
					</button>
				</div>
			)}
		</div>
	);
	return (
		<div className={styles.playersPage} aria-busy={page.isLoading}>
			<PaginatedPageLayout
				header={header}
				page={
					page.playersPage && players.length ? page.selectedPage : undefined
				}
				totalPages={
					page.playersPage && players.length
						? page.playersPage.totalPages
						: undefined
				}
				onPageChange={page.selectPage}
			>
				{page.errorMessage ? (
					<section className={styles.playersPage__message} role="alert">
						<p>{page.errorMessage}</p>
						<button type="button" onClick={() => page.retry()}>
							Reintentar
						</button>
					</section>
				) : page.isLoading ? (
					<p className={styles.playersPage__message} role="status">
						Cargando jugadores…
					</p>
				) : players.length === 0 ? (
					<p className={styles.playersPage__message} role="status">
						{page.criteria.name ||
						page.criteria.leagueId ||
						page.criteria.teamId ||
						page.criteria.position ||
						page.criteria.nationality
							? "No hay jugadores que coincidan con los filtros."
							: "No hay jugadores disponibles."}
					</p>
				) : (
					<section className={styles.playersPage__grid} aria-label="Jugadores">
						{players.map((player) => (
							<PlayerCard key={player.id} player={player} />
						))}
					</section>
				)}
			</PaginatedPageLayout>
		</div>
	);
}
