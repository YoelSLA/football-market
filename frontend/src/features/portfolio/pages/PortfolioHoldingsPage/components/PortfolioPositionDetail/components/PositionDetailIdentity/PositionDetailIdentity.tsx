import { portfolioLabels } from "@/features/portfolio/constants";
import type { PortfolioPosition } from "@/features/portfolio/types";
import { PlayerPortrait } from "@/shared/components";
import styles from "./PositionDetailIdentity.module.scss";

interface Props {
	position: PortfolioPosition;
}

export default function PositionDetailIdentity({ position: p }: Props) {
	return (
		<section className={styles.identity} aria-label="Información del jugador">
			<div className={styles.player}>
				<div className={styles.portrait}>
					<PlayerPortrait src={p.imageUrl} name={p.playerName} />
				</div>
				<div>
					<h3>{p.playerName ?? portfolioLabels.missing}</h3>
					<p>{p.teamName ?? portfolioLabels.missing}</p>
					<div className={styles.statusLine}>
						<span>{p.position ?? portfolioLabels.missing}</span>
						<span
							className={`${styles.badge} ${
								p.status === "CLOSED" ? styles.badge_closed : ""
							}`}
						>
							<span aria-hidden="true">●</span>
							{p.status === "OPEN" ? "ABIERTA" : "CERRADA"}
						</span>
					</div>
				</div>
			</div>
			<dl className={styles.identifiers}>
				<div>
					<dt>ID del jugador</dt>
					<dd>{p.playerId}</dd>
				</div>
				<div>
					<dt>Jugador activo</dt>
					<dd>{p.active ? "Sí" : "No"}</dd>
				</div>
				<div>
					<dt>Estado de valoración</dt>
					<dd>{p.valuationStatus}</dd>
				</div>
			</dl>
		</section>
	);
}
