import MarketCatalogState from "@/features/market/components/MarketCatalogState";
import type { MarketPlayerQuoteState } from "@/features/market/hooks";
import type { PlayerHolding } from "../../MarketSellPage";
import MarketSellCard from "../MarketSellCard";
import styles from "./MarketSellCatalog.module.scss";

interface Props {
	isLoading: boolean;
	isError: boolean;
	errorMessage?: string | null;
	onRetry?: () => void;
	players: PlayerHolding[];
	getQuoteState: (playerId: number) => MarketPlayerQuoteState;
	onSell: (player: PlayerHolding, target: HTMLElement) => void;
}

export default function MarketSellCatalog({
	isLoading,
	isError,
	errorMessage,
	onRetry,
	players,
	getQuoteState,
	onSell,
}: Props) {
	if (isLoading) {
		return (
			<main className={styles.sellCatalog}>
				<MarketCatalogState
					variant="loading"
					message="Cargando jugadores y tenencias…"
				/>
			</main>
		);
	}

	if (isError) {
		return (
			<main className={styles.sellCatalog}>
				<MarketCatalogState
					variant="error"
					message={errorMessage ?? "No se pudo cargar el mercado."}
					onRetry={onRetry}
				/>
			</main>
		);
	}

	if (players.length === 0) {
		return (
			<main className={styles.sellCatalog}>
				<MarketCatalogState
					variant="empty"
					message="No hay jugadores con tenencias para publicar."
				/>
			</main>
		);
	}

	return (
		<main className={styles.sellCatalog}>
			<div className={styles.sellCatalog__grid}>
				{players.map((player) => {
					const quote = getQuoteState(player.id);
					return (
						<MarketSellCard
							key={player.id}
							player={player}
							quoteState={quote}
							onSell={onSell}
						/>
					);
				})}
			</div>
		</main>
	);
}
