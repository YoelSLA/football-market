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
	onClose,
	onConfirm,
	triggerRef,
}: Props) {
	if (!player || !listing) return null;
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
						<p>
							{result.quantity} tokens por {formatCredits(result.totalAmount)}
						</p>
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
						<footer className={styles.modal__actions}>
							<Button type="button" onClick={onClose} disabled={isBusy}>
								Cancelar
							</Button>
							<Button type="button" onClick={onConfirm} disabled={isBusy}>
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
