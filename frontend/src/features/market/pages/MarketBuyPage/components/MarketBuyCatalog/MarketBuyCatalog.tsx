import type { MarketPlayerQuoteState } from "@/features/market/hooks";
import type { MarketPlayer } from "@/features/market/types";
import { Button } from "@/shared/components";
import MarketPlayerCard from "../MarketPlayerCard";
import styles from "./MarketBuyCatalog.module.scss";

interface Props {
	isLoading: boolean;
	isError: boolean;
	errorMessage?: string | null;
	onRetry?: () => void;
	players: MarketPlayer[];
	getQuoteState: (playerId: number) => MarketPlayerQuoteState;
	onBuyClick: (player: MarketPlayer, triggerEl: HTMLElement | null) => void;
}

export default function MarketBuyCatalog({
	isLoading,
	isError,
	errorMessage,
	onRetry,
	players,
	getQuoteState,
	onBuyClick,
}: Props) {
	return (
		<main className={styles.buyCatalog}>
			{isLoading && (
				<div role="status" className={styles.buyCatalog__status}>
					<p>Cargando catálogo del mercado...</p>
				</div>
			)}

			{!isLoading && isError && (
				<div role="alert" className={styles.buyCatalog__errorState}>
					<p>{errorMessage || "No se pudo cargar el listado del mercado."}</p>
					{onRetry && (
						<Button
							type="button"
							onClick={onRetry}
							className={styles.buyCatalog__retryBtn}
						>
							Reintentar
						</Button>
					)}
				</div>
			)}

			{!isLoading && !isError && players.length === 0 && (
				<div className={styles.buyCatalog__status}>
					<p>No hay jugadores disponibles en el mercado actualmente.</p>
				</div>
			)}

			{!isLoading && !isError && players.length > 0 && (
				<div className={styles.buyCatalog__grid}>
					{players.map((player) => {
						const quoteState = getQuoteState(player.id);
						return (
							<MarketPlayerCard
								key={player.id}
								player={player}
								quote={quoteState.quote}
								quoteStatus={quoteState.status}
								quoteHttpStatus={quoteState.httpStatus}
								canRetryQuote={quoteState.canRetry}
								onRetryQuote={quoteState.onRetry}
								onBuyClick={onBuyClick}
							/>
						);
					})}
				</div>
			)}
		</main>
	);
}
