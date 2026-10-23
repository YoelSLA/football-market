import { PaginatedPageLayout } from "@/app/layouts";
import { usePlayersPage } from "@/features/players/hooks";
import { Button } from "@/shared/components";
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
			</div>

			<PlayerCatalogControls
				searchText={page.searchText}
				selectedLeague={page.selectedLeague}
				selectedPosition={page.selectedPosition}
				onSearchTextChange={page.setSearchText}
				onLeagueChange={page.setSelectedLeague}
				onPositionChange={page.setSelectedPosition}
			/>
		</div>
	);

	return (
		<div className={styles.playersPage} aria-busy={page.isLoading}>
			<PaginatedPageLayout
				header={header}
				page={page.playersPage ? page.selectedPage : undefined}
				totalPages={
					page.playersPage && players.length > 0
						? page.playersPage.totalPages
						: undefined
				}
				onPageChange={page.selectPage}
			>
				{page.errorMessage ? (
					<section className={styles.playersPage__message} role="alert">
						<p>{page.errorMessage}</p>

						<Button type="button" onClick={() => page.retry()}>
							Reintentar
						</Button>
					</section>
				) : page.isLoading ? (
					<p className={styles.playersPage__message} role="status">
						Cargando jugadores...
					</p>
				) : players.length === 0 ? (
					<p className={styles.playersPage__message} role="status">
						No hay jugadores disponibles.
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
