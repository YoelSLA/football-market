import type { RefObject } from "react";
import type { UseFormRegister } from "react-hook-form";
import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import TokenQuantityStepper from "@/features/market/components/TokenQuantityStepper";
import type {
	BuyEstimation,
	BuyQuantityForm,
	BuyResult,
	OfficialMarketPlayer,
	OfficialMarketQuote,
} from "@/features/market/types";
import {
	formatCredits,
	formatQuoteEffectiveDate,
	formatVariationPercentage,
} from "@/features/market/utils";
import { Button, Modal } from "@/shared/components";
import { LoaderCircle, ShoppingCart, X } from "@/shared/icons";
import styles from "./BuyTokenModal.module.scss";

interface Props {
	isOpen: boolean;
	player: OfficialMarketPlayer | null;
	quote: OfficialMarketQuote | null;
	confirmationBlockReason: string | null;
	estimation: BuyEstimation | null;
	formRegister: UseFormRegister<BuyQuantityForm>;
	formError?: string;
	quantity: string;
	onQuantityChange: (quantity: number) => void;
	isBusy: boolean;
	canConfirm: boolean;
	isSuccess: boolean;
	purchaseResult: BuyResult | null;
	errorMessage: string | null;
	onClose: () => void;
	onConfirm: () => void;
	triggerRef?: RefObject<HTMLElement | null>;
	initialFocusRef?: RefObject<HTMLInputElement | null>;
}

const dateTimeFormatter = new Intl.DateTimeFormat("es-AR", {
	dateStyle: "short",
	timeStyle: "short",
});

export default function BuyTokenModal({
	isOpen,
	player,
	quote,
	confirmationBlockReason,
	estimation,
	formRegister,
	formError,
	quantity,
	onQuantityChange,
	isBusy,
	canConfirm,
	isSuccess,
	purchaseResult,
	errorMessage,
	onClose,
	onConfirm,
	triggerRef,
	initialFocusRef,
}: Props) {
	if (!player || !quote) return null;
	const { ref: registerRef, ...quantityProps } = formRegister("quantity");
	const setInputRef = (element: HTMLInputElement | null) => {
		registerRef(element);
		if (initialFocusRef)
			(initialFocusRef as { current: HTMLInputElement | null }).current =
				element;
	};
	const numericQuantity = Number(quantity);
	const safeQuantity = Number.isInteger(numericQuantity) ? numericQuantity : 1;
	return (
		<Modal
			isOpen={isOpen}
			onClose={onClose}
			title={`Comprar tokens de ${player.name}`}
			closeOnEsc={!isBusy}
			closeOnOverlayClick={!isBusy}
			isBusy={isBusy}
			triggerRef={triggerRef}
			initialFocusRef={initialFocusRef as RefObject<HTMLElement | null>}
		>
			<div className={styles.modal}>
				<header className={styles.modal__header}>
					<h3 className={styles.modal__title}>Comprar tokens</h3>
					<Button
						unstyled
						type="button"
						aria-label="Cerrar compra"
						onClick={onClose}
						disabled={isBusy}
					>
						<X aria-hidden="true" />
					</Button>
				</header>
				<div className={styles.modal__content}>
					<section aria-label="Jugador seleccionado">
						<MarketPlayerSummary player={player} />
					</section>
					<dl className={styles.modal__prices}>
						<div>
							<dt>Precio actual</dt>
							<dd>{formatCredits(quote.price)}</dd>
						</div>
						<div>
							<dt>Variación</dt>
							<dd>
								{formatVariationPercentage(
									quote.variationPercentage,
									"Sin referencia previa",
								)}
							</dd>
						</div>
						<div>
							<dt>Precio anterior</dt>
							<dd>{formatCredits(quote.previousPrice)}</dd>
						</div>
						<div>
							<dt>Vigencia</dt>
							<dd>{formatQuoteEffectiveDate(quote.effectiveDate)}</dd>
						</div>
					</dl>
					{errorMessage && !isSuccess && (
						<p role="alert" className={styles.modal__errorAlert}>
							{errorMessage}
						</p>
					)}
					{isSuccess && purchaseResult ? (
						<div role="status" className={styles.modal__successCard}>
							<h4>Compra confirmada</h4>
							<p>Transacción #{purchaseResult.transactionId}</p>
							<p>
								{purchaseResult.quantity} tokens ·{" "}
								{formatCredits(purchaseResult.unitPrice)} por token
							</p>
							<p>Total: {formatCredits(purchaseResult.totalAmount)}</p>
							<p>
								{dateTimeFormatter.format(new Date(purchaseResult.executedAt))}
							</p>
						</div>
					) : (
						<form
							onSubmit={(event) => {
								event.preventDefault();
								onConfirm();
							}}
							className={styles.modal__form}
						>
							<TokenQuantityStepper
								id="buy-token-quantity"
								label="Cantidad de tokens (1 a 100)"
								quantity={safeQuantity}
								min={1}
								max={100}
								disabled={isBusy}
								error={formError}
								inputRef={setInputRef}
								inputProps={quantityProps}
								presets={[1, 5, 10, 50, 100]}
								onQuantityChange={onQuantityChange}
							/>
							{estimation && (
								<div className={styles.modal__estimation}>
									<p>
										Importe estimado:{" "}
										<strong>{formatCredits(estimation.totalCentimes)}</strong>
									</p>
									<p>
										El servidor confirma precio y disponibilidad al comprar.
									</p>
								</div>
							)}
						</form>
					)}
				</div>
				{confirmationBlockReason && !isSuccess && (
					<p id="market-buy-block-reason" role="status">
						{confirmationBlockReason}
					</p>
				)}
				<footer className={styles.modal__footer}>
					<Button type="button" onClick={onClose} disabled={isBusy}>
						{isSuccess ? "Cerrar" : "Cancelar"}
					</Button>
					{!isSuccess && (
						<Button
							type="button"
							onClick={onConfirm}
							disabled={!canConfirm || isBusy}
							aria-busy={isBusy}
						>
							{isBusy ? (
								<LoaderCircle aria-hidden="true" />
							) : (
								<ShoppingCart aria-hidden="true" />
							)}
							{isBusy ? "Procesando…" : "Confirmar compra"}
						</Button>
					)}
				</footer>
			</div>
		</Modal>
	);
}
