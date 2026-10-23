import styles from "./PlayerCatalogControls.module.scss";

interface Props {
	searchText: string;
	selectedLeague: string;
	selectedPosition: string;
	onSearchTextChange: (value: string) => void;
	onLeagueChange: (value: string) => void;
	onPositionChange: (value: string) => void;
}

export default function PlayerCatalogControls(props: Props) {
	return (
		<section
			className={styles.playerCatalogControls}
			aria-label="Controles del catálogo"
		>
			<label>
				<span>Buscar jugador</span>
				<input
					value={props.searchText}
					onChange={(event) => props.onSearchTextChange(event.target.value)}
					placeholder="Nombre del jugador"
				/>
			</label>
			<label>
				<span>Liga</span>
				<select
					value={props.selectedLeague}
					onChange={(event) => props.onLeagueChange(event.target.value)}
				>
					<option value="">Todas las ligas</option>
					<option>Premier League</option>
					<option>Bundesliga</option>
					<option>Primera División</option>
					<option>Serie A</option>
					<option>Ligue 1</option>
				</select>
			</label>
			<label>
				<span>Posición</span>
				<select
					value={props.selectedPosition}
					onChange={(event) => props.onPositionChange(event.target.value)}
				>
					<option value="">Todas las posiciones</option>
					<option>Goalkeeper</option>
					<option>Defence</option>
					<option>Midfield</option>
					<option>Offence</option>
				</select>
			</label>
		</section>
	);
}
