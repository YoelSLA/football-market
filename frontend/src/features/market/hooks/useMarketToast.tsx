import { toast } from "react-toastify";
import MarketToastContent from "@/features/market/components/MarketToastContent";
import type {
	BuyResult,
	SellOfficialResult,
	SellResult,
} from "@/features/market/types";

const dateTimeFormatter = new Intl.DateTimeFormat("es-AR", {
	day: "2-digit",
	month: "2-digit",
	year: "numeric",
	hour: "2-digit",
	minute: "2-digit",
});

/**
 * Hook local de Market para emitir notificaciones visuales mediante React-Toastify.
 * Combina el nombre del jugador con el resultado de la operación en un componente estilizado.
 */
export function useMarketToast() {
	const notifySuccess = (playerName: string, result: BuyResult) => {
		let executedAtFormatted: string | undefined;
		try {
			const date = new Date(result.executedAt);
			if (!Number.isNaN(date.getTime())) {
				executedAtFormatted = dateTimeFormatter.format(date);
			}
		} catch {
			// Conserva fallback sin timestamp si falla el formateo
		}

		toast.success(
			<MarketToastContent
				variant="buy"
				title="Compra confirmada"
				badge={`#${result.transactionId}`}
				playerName={playerName}
				quantity={result.quantity}
				totalAmount={result.totalAmount}
				unitPrice={result.unitPrice}
				timestampText={executedAtFormatted}
			/>,
			{ autoClose: 4500 },
		);
	};

	const notifyError = (message: string) => {
		toast.error(
			<MarketToastContent variant="error" title="Error" message={message} />,
			{ autoClose: 5000 },
		);
	};

	const notifySellSuccess = (playerName: string, result: SellResult) => {
		const expiresAt = new Date(result.expiresAt);
		const expiry = Number.isNaN(expiresAt.getTime())
			? undefined
			: `Vence: ${dateTimeFormatter.format(expiresAt)}`;

		toast.success(
			<MarketToastContent
				variant="sell"
				title="Publicación confirmada"
				badge={`#${result.listingId}`}
				playerName={playerName}
				quantity={result.quantity}
				totalAmount={result.totalAmount}
				unitPrice={result.unitPrice}
				timestampText={expiry}
			/>,
			{ autoClose: 4500 },
		);
	};

	const notifySellOfficialSuccess = (
		playerName: string,
		result: SellOfficialResult,
	) => {
		toast.success(
			<MarketToastContent
				variant="sell"
				title="Venta inmediata completada"
				badge={`#${result.transactionId}`}
				playerName={playerName}
				quantity={result.quantity}
				totalAmount={result.totalAmount}
				unitPrice={result.unitPrice}
			/>,
			{ autoClose: 4500 },
		);
	};

	const notifyCancelSuccess = (
		listingId: number,
		playerName: string,
		quantity: number,
	) => {
		toast.success(
			<MarketToastContent
				variant="cancel"
				title="Publicación cancelada"
				badge={`#${listingId}`}
				playerName={playerName}
				quantity={quantity}
			/>,
			{ autoClose: 4500 },
		);
	};

	return {
		notifySuccess,
		notifyError,
		notifySellSuccess,
		notifySellOfficialSuccess,
		notifyCancelSuccess,
	};
}
