import { toast } from "react-toastify";
import type { BuyResult, SellResult } from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";

const dateTimeFormatter = new Intl.DateTimeFormat("es-AR", {
	day: "2-digit",
	month: "2-digit",
	year: "numeric",
	hour: "2-digit",
	minute: "2-digit",
});

/**
 * Hook local de Market para emitir notificaciones visuales mediante React-Toastify.
 * Combina el nombre del jugador (de presentación) con el resultado efectivo de la compra.
 */
export function useMarketToast() {
	const notifySuccess = (playerName: string, result: BuyResult) => {
		const unitPriceFormatted = formatCredits(result.unitPrice);
		const totalAmountFormatted = formatCredits(result.totalAmount);
		let executedAtFormatted = result.executedAt;
		try {
			const date = new Date(result.executedAt);
			if (!Number.isNaN(date.getTime())) {
				executedAtFormatted = dateTimeFormatter.format(date);
			}
		} catch {
			// Conserva string original si falla el formateo
		}

		toast.success(
			`¡Compraste ${result.quantity} ${result.quantity === 1 ? "token" : "tokens"} de ${playerName}! Transacción #${result.transactionId}. Precio unitario: ${unitPriceFormatted}. Total: ${totalAmountFormatted}. Ejecutado: ${executedAtFormatted}.`,
			{ autoClose: 5000 },
		);
	};

	const notifyError = (message: string) => {
		toast.error(message, { autoClose: 5000 });
	};
	const notifySellSuccess = (playerName: string, result: SellResult) => {
		const expiresAt = new Date(result.expiresAt);
		const expiry = Number.isNaN(expiresAt.getTime())
			? result.expiresAt
			: dateTimeFormatter.format(expiresAt);
		toast.success(
			`Publicaste ${result.quantity} tokens de ${playerName}. Publicación #${result.listingId}. Precio unitario: ${formatCredits(result.unitPrice)}. Total: ${formatCredits(result.totalAmount)}. Vence: ${expiry}.`,
			{ autoClose: 5000 },
		);
	};

	return {
		notifySuccess,
		notifyError,
		notifySellSuccess,
	};
}
