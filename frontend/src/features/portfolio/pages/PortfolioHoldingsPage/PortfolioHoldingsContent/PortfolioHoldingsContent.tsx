import { portfolioLabels } from "@/features/portfolio/constants";
import type { PortfolioPosition } from "@/features/portfolio/types";
import styles from "./PortfolioHoldingsContent.module.scss";
import PortfolioPositionCard from "./PortfolioPositionCard";

interface Props {
	isLoading: boolean;
	errorMessage: string | null;
	positions: PortfolioPosition[];
	filter: "ALL" | "OPEN" | "CLOSED";
	onRetry: () => void;
	onOpenDetail: (position: PortfolioPosition) => void;
}

export default function PortfolioHoldingsContent({
	isLoading,
	errorMessage,
	positions,
	filter,
	onRetry,
	onOpenDetail,
}: Props) {
	if (errorMessage) {
		return (
			<div role="alert" className={styles.portfolioHoldingsContent__error}>
				<p>{errorMessage}</p>
				<button
					type="button"
					onClick={onRetry}
					className={styles.portfolioHoldingsContent__retryButton}
				>
					Reintentar
				</button>
			</div>
		);
	}

	if (isLoading) {
		return (
			<p role="status" className={styles.portfolioHoldingsContent__loading}>
				Cargando tus posiciones…
			</p>
		);
	}

	if (positions.length === 0) {
		return (
			<p role="status" className={styles.portfolioHoldingsContent__empty}>
				{filter === "ALL"
					? portfolioLabels.empty
					: `No hay posiciones ${filter === "OPEN" ? "abiertas" : "cerradas"}.`}
			</p>
		);
	}

	return (
		<>
			<p className={styles.portfolioHoldingsContent__summary}>
				Mostrando {positions.length} posiciones
			</p>
			<ul
				className={styles.portfolioHoldingsContent__grid}
				aria-label="Posiciones del portfolio"
			>
				{positions.map((position) => (
					<li key={position.playerId}>
						<PortfolioPositionCard
							position={position}
							onDetails={() => onOpenDetail(position)}
						/>
					</li>
				))}
			</ul>
		</>
	);
}
