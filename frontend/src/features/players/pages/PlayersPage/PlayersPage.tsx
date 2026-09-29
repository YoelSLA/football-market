import {
	PlayerCard,
	PlayerCatalogControls,
	PlayersPagination,
} from "../../components";
import { usePlayersPage } from "../../hooks";
import styles from "./PlayersPage.module.scss";

export function PlayersPage() {
	const page = usePlayersPage();
	const players = page.playersPage?.players ?? [];

	return (
		<main className={styles["players-page"]} aria-busy={page.isLoading}>
			<header className={styles["players-page__header"]}>
				<div>
					<p>FOOTBALL MARKET</p>
					<h1>Catálogo de jugadores</h1>
				</div>
				{page.playersPage && (
					<span>{page.playersPage.totalElements} jugadores activos</span>
				)}
			</header>

			<PlayerCatalogControls
				searchText={page.searchText}
				selectedLeague={page.selectedLeague}
				selectedPosition={page.selectedPosition}
				onSearchTextChange={page.setSearchText}
				onLeagueChange={page.setSelectedLeague}
				onPositionChange={page.setSelectedPosition}
			/>

			{page.errorMessage ? (
				<section className={styles["players-page__message"]} role="alert">
					<p>{page.errorMessage}</p>
					<button type="button" onClick={() => page.retry()}>
						Reintentar
					</button>
				</section>
			) : page.isLoading ? (
				<p className={styles["players-page__message"]} role="status">
					Cargando jugadores...
				</p>
			) : players.length === 0 ? (
				<p className={styles["players-page__message"]} role="status">
					No hay jugadores disponibles.
				</p>
			) : (
				<section
					className={styles["players-page__grid"]}
					aria-label="Jugadores"
				>
					{players.map((player) => (
						<PlayerCard key={player.id} player={player} />
					))}
				</section>
			)}

			{page.playersPage && players.length > 0 && (
				<PlayersPagination
					currentPage={page.selectedPage}
					totalPages={page.playersPage.totalPages}
					onPageChange={page.selectPage}
				/>
			)}
		</main>
	);
}
