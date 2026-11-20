import type { RefObject } from "react";
import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import type {
	BuyResult,
	CommunityMarketPlayer,
	MarketListing,
} from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { Button, Modal } from "@/shared/components";
import { X } from "@/shared/icons";
import styles from "./BuyListingModal.module.scss";

interface Props {
	player: CommunityMarketPlayer | null;
	listing: MarketListing | null;
	isBusy: boolean;
	result: BuyResult | null;
	returnsToCatalog: boolean;
	errorMessage: string | null;
	availableCreditsCentimes?: number;
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
	returnsToCatalog,
	errorMessage,
	availableCreditsCentimes,
	canConfirm,
	onClose,
	onConfirm,
	triggerRef,
}: Props) {
	if (!player || !listing) return null;
	const isExpired = Date.parse(listing.expiresAt) <= Date.now();
	const hasInsufficientCredits =
		availableCreditsCentimes !== undefined &&
		availableCreditsCentimes < listing.totalAmount;
	const estimatedRemaining =
		availableCreditsCentimes === undefined
			? null
			: availableCreditsCentimes - listing.totalAmount;
	return (
		<Modal
			className={styles.dialog}
			isOpen
			onClose={onClose}
			title={`Confirmar compra P2P, publicación #${listing.listingId}`}
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
					<div className={styles.modal__heading}>
						<h3>Confirmar compra P2P</h3>
						<span>Publicación #{listing.listingId}</span>
					</div>
					<Button
						type="button"
						className={styles.modal__close}
						unstyled
						aria-label="Cerrar confirmación de compra"
						onClick={onClose}
						disabled={isBusy}
					>
						<X aria-hidden="true" />
					</Button>
				</header>
				<div className={styles.modal__player}>
					<MarketPlayerSummary player={player} variant="communityCard" />
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
						<Button onClick={onClose}>
							{returnsToCatalog ? "Volver al catálogo" : "Volver a ofertas"}
						</Button>
					</div>
				) : (
					<>
						<dl className={styles.modal__details}>
							<div>
								<dt>Cantidad</dt>
								<dd>{listing.quantity} tokens</dd>
							</div>
							<div>
								<dt>Precio por token</dt>
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
						<div className={styles.modal__total}>
							<span>Total de la compra</span>
							<strong>{formatCredits(listing.totalAmount)}</strong>
						</div>
						{availableCreditsCentimes !== undefined && (
							<dl className={styles.modal__balance}>
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
							<Button
								type="button"
								variant="secondary"
								onClick={onClose}
								disabled={isBusy}
							>
								Cancelar
							</Button>
							<Button
								type="button"
								onClick={onConfirm}
								disabled={isBusy || !canConfirm}
								aria-busy={isBusy ? "true" : undefined}
							>
								{isBusy ? "Procesando…" : "Confirmar compra"}
							</Button>
						</footer>
					</>
				)}
			</section>
		</Modal>
	);
}
