import type { MarketListing } from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { TOKEN_SYMBOL } from "@/shared/constants";
import styles from "./BuyListingBreakdown.module.scss";

interface Props {
	listing: MarketListing;
	availableCreditsCentimes?: number;
	estimatedRemaining: number | null;
	errorMessage: string | null;
	isExpired: boolean;
	hasInsufficientCredits: boolean;
	canConfirm: boolean;
}

export function BuyListingBreakdown({
	listing,
	availableCreditsCentimes,
	estimatedRemaining,
	errorMessage,
	isExpired,
	hasInsufficientCredits,
	canConfirm,
}: Props) {
	return (
		<>
			<dl className={styles.details}>
				<div>
					<dt>Cantidad</dt>
					<dd>
						{listing.quantity} {TOKEN_SYMBOL}
					</dd>
				</div>
				<div>
					<dt>Precio por {TOKEN_SYMBOL}</dt>
					<dd>{formatCredits(listing.unitPrice)}</dd>
				</div>
				<div>
					<dt>Vencimiento</dt>
					<dd>
						<time dateTime={listing.expiresAt}>
							{new Date(listing.expiresAt).toLocaleString("es-AR")}
						</time>
					</dd>
				</div>
			</dl>

			<div className={styles.total}>
				<span>Total de la compra</span>
				<strong>{formatCredits(listing.totalAmount)}</strong>
			</div>

			{availableCreditsCentimes !== undefined && (
				<dl className={styles.balance}>
					<div>
						<dt>Saldo disponible</dt>
						<dd>{formatCredits(availableCreditsCentimes)}</dd>
					</div>
					<div>
						<dt>Saldo restante estimado</dt>
						<dd>{formatCredits(estimatedRemaining)}</dd>
					</div>
				</dl>
			)}

			<p className={styles.notice}>
				La disponibilidad se confirma al realizar la compra.
			</p>

			{errorMessage && (
				<p className={styles.error} role="alert">
					{errorMessage}
				</p>
			)}

			{!errorMessage && !canConfirm && (
				<p className={styles.error} role="status">
					{isExpired
						? "La publicación venció. Volvé al detalle para actualizar las ofertas."
						: hasInsufficientCredits
							? "El saldo disponible no alcanza para comprar este lote."
							: "La compra no está disponible."}
				</p>
			)}
		</>
	);
}
