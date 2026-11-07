import styles from "./PortfolioTokenDistribution.module.scss";

interface Props {
	total: number;
	available: number;
	reserved: number;
}

function percentage(value: number, total: number): number {
	if (total <= 0) return 0;
	return Math.round((value / total) * 100);
}

/**
 * Distribucion de tokens de la posicion.
 *
 * Las proporciones se derivan de los contadores que entrega el backend
 * (tokens totales, disponibles y reservados): es una representacion
 * visual de un reparto, no un calculo financiero.
 */
export default function PortfolioTokenDistribution({
	total,
	available,
	reserved,
}: Props) {
	const availablePercentage = percentage(available, total);
	const reservedPercentage = percentage(reserved, total);

	return (
		<div className={styles.portfolioDistribution}>
			<h3>Distribución de tokens</h3>
			<div
				className={styles.portfolioDistribution__track}
				role="img"
				aria-label={`${available} tokens disponibles y ${reserved} reservados de ${total}`}
			>
				<span
					className={styles.portfolioDistribution__fill}
					style={{ width: `${availablePercentage}%` }}
				/>
				<span
					className={styles.portfolioDistribution__reserved}
					style={{ width: `${reservedPercentage}%` }}
				/>
			</div>
			<ul className={styles.portfolioDistribution__legend}>
				<li className={styles.portfolioDistribution__percentage_available}>
					<span
						className={`${styles.portfolioDistribution__dot} ${styles.portfolioDistribution__dot_available}`}
						aria-hidden="true"
					/>
					{availablePercentage} %
				</li>
				<li className={styles.portfolioDistribution__percentage_reserved}>
					<span
						className={`${styles.portfolioDistribution__dot} ${styles.portfolioDistribution__dot_reserved}`}
						aria-hidden="true"
					/>
					{reservedPercentage} %
				</li>
				<li>
					<span
						className={`${styles.portfolioDistribution__dot} ${styles.portfolioDistribution__dot_available}`}
						aria-hidden="true"
					/>
					Disponibles ({available})
				</li>
				<li>
					<span
						className={`${styles.portfolioDistribution__dot} ${styles.portfolioDistribution__dot_reserved}`}
						aria-hidden="true"
					/>
					Reservados ({reserved})
				</li>
			</ul>
		</div>
	);
}
