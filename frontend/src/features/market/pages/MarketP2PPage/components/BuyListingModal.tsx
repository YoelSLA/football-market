import type { RefObject } from "react";
import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import type {
	BuyResult,
	CommunityMarketPlayer,
	MarketListing,
} from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { Button, Modal } from "@/shared/components";
import styles from "./BuyListingModal.module.scss";

interface Props {
	player: CommunityMarketPlayer | null;
	listing: MarketListing | null;
	isBusy: boolean;
	result: BuyResult | null;
	errorMessage: string | null;
	availableCredits?: number;
	canConfirm: boolean;
	onClose: () => void;
	onConfirm: () => void;
	triggerRef: RefObject<HTMLElement | null>;
}

export default function BuyListingModal({
	player,
	listing,
	isBusy,
	result,
	errorMessage,
	availableCredits,
	canConfirm,
	onClose,
	onConfirm,
	triggerRef,
}: Props) {
	if (!player || !listing) return null;
	const isExpired = Date.parse(listing.expiresAt) <= Date.now();
	const hasInsufficientCredits =
		availableCredits !== undefined && availableCredits < listing.totalAmount;
	return (
		<Modal
			className={styles.dialog}
			isOpen
			onClose={onClose}
			title={`Comprar publicación #${listing.listingId}`}
			isBusy={isBusy}
			closeOnEsc={!isBusy}
			closeOnOverlayClick={!isBusy}
			triggerRef={triggerRef}
		>
			<section
				className={styles.modal}
				aria-label="Confirmación de compra de publicación"
			>
				<header className={styles.modal__header}>
					<div>
						<span>Oferta de usuario</span>
						<h3>Publicación #{listing.listingId}</h3>
					</div>
					<Button type="button" onClick={onClose} disabled={isBusy}>
						Cerrar
					</Button>
				</header>
				<div className={styles.modal__player}>
					<MarketPlayerSummary player={player} />
				</div>
				{result ? (
					<div className={styles.modal__success} role="status">
						<h3>Compra confirmada</h3>
						<p>Transacción #{result.transactionId}</p>
						<dl>
							<div>
								<dt>Cantidad</dt>
								<dd>{result.quantity} tokens</dd>
							</div>
							<div>
								<dt>Precio unitario</dt>
								<dd>{formatCredits(result.unitPrice)}</dd>
							</div>
							<div>
								<dt>Total</dt>
								<dd>{formatCredits(result.totalAmount)}</dd>
							</div>
							<div>
								<dt>Ejecutada</dt>
								<dd>{new Date(result.executedAt).toLocaleString("es-AR")}</dd>
							</div>
						</dl>
						<Button onClick={onClose}>Volver a ofertas</Button>
					</div>
				) : (
					<>
						<h3>Lote completo</h3>
						<dl className={styles.modal__details}>
							<div className={styles.modal__total}>
								<dt>Cantidad</dt>
								<dd>{listing.quantity} tokens</dd>
							</div>
							<div>
								<dt>Precio por token</dt>
								<dd>{formatCredits(listing.unitPrice)}</dd>
							</div>
							<div>
								<dt>Total</dt>
								<dd>{formatCredits(listing.totalAmount)}</dd>
							</div>
							<div>
								<dt>Vencimiento</dt>
								<dd>{new Date(listing.expiresAt).toLocaleString("es-AR")}</dd>
							</div>
							{availableCredits != null && (
								<div>
									<dt>Saldo disponible</dt>
									<dd>{availableCredits.toLocaleString("es-AR")} créditos</dd>
								</div>
							)}
						</dl>
						<p className={styles.modal__notice}>
							La disponibilidad se confirma al realizar la compra.
						</p>
						{errorMessage && (
							<p className={styles.modal__error} role="alert">
								{errorMessage}
							</p>
						)}
						{!errorMessage && !canConfirm && (
							<p className={styles.modal__error} role="status">
								{isExpired
									? "La publicación venció. Volvé al detalle para actualizar las ofertas."
									: hasInsufficientCredits
										? "El saldo disponible no alcanza para comprar este lote."
										: "La compra no está disponible."}
							</p>
						)}
						<footer className={styles.modal__actions}>
							<Button type="button" onClick={onClose} disabled={isBusy}>
								Cancelar
							</Button>
							<Button
								type="button"
								onClick={onConfirm}
								disabled={isBusy || !canConfirm}
								aria-busy={isBusy ? "true" : undefined}
							>
								{isBusy
									? "Procesando…"
									: `Comprar por ${formatCredits(listing.totalAmount)}`}
							</Button>
						</footer>
					</>
				)}
			</section>
		</Modal>
	);
}
