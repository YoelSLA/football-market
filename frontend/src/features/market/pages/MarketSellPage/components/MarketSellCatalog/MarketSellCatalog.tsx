import type { MarketPlayerQuoteState } from "@/features/market/hooks";
import { Button } from "@/shared/components";
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
	return (
		<main className={styles.sellCatalog}>
			{isLoading && (
				<p role="status" className={styles.sellCatalog__status}>
					Cargando jugadores y tenencias…
				</p>
			)}

			{!isLoading && isError && (
				<div role="alert" className={styles.sellCatalog__status}>
					<p>{errorMessage ?? "No se pudo cargar el mercado."}</p>
					{onRetry && <Button onClick={onRetry}>Reintentar</Button>}
				</div>
			)}

			{!isLoading && !isError && players.length === 0 && (
				<p className={styles.sellCatalog__status}>
					No hay jugadores con tenencias para publicar.
				</p>
			)}

			{!isLoading && !isError && players.length > 0 && (
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
			)}
		</main>
	);
}
