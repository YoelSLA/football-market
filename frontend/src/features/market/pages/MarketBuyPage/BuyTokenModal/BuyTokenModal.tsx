import type { RefObject } from "react";
import type { UseFormRegister } from "react-hook-form";
import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import MarketQuoteFeedback from "@/features/market/components/MarketQuoteFeedback";
import type {
	BuyEstimation,
	BuyQuantityForm,
	BuyResult,
	MarketPlayer,
	MarketQuote,
	MarketQuoteStatus,
} from "@/features/market/types";
import {
	formatCentimes,
	formatCredits,
	formatQuoteCalculatedAt,
} from "@/features/market/utils";
import { Button, Field, Modal } from "@/shared/components";
import {
	Coins,
	Layers3,
	LoaderCircle,
	Minus,
	Plus,
	ShoppingCart,
	X,
} from "@/shared/icons";
import styles from "./BuyTokenModal.module.scss";

interface Props {
	isOpen: boolean;
	player: MarketPlayer | null;
	quote: MarketQuote | null;
	quoteStatus: MarketQuoteStatus;
	quoteHttpStatus?: number;
	canRetryQuote: boolean;
	onRetryQuote: () => void;
	confirmationBlockReason: string | null;
	estimation: BuyEstimation | null;
	formRegister: UseFormRegister<BuyQuantityForm>;
	formError?: string;
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
	day: "2-digit",
	month: "2-digit",
	year: "numeric",
	hour: "2-digit",
	minute: "2-digit",
});

/**
 * Modal de confirmación de compra de tokens (SPEC-019).
 * Component local sin Hooks propios: recibe todo su estado y handlers por props.
 */
export default function BuyTokenModal({
	isOpen,
	player,
	quote,
	quoteStatus,
	quoteHttpStatus,
	canRetryQuote,
	onRetryQuote,
	confirmationBlockReason,
	estimation,
	formRegister,
	formError,
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
	if (!player) {
		return null;
	}

	const { ref: registerRef, ...quantityProps } = formRegister("quantity");

	const setInputRef = (el: HTMLInputElement | null) => {
		registerRef(el);
		if (initialFocusRef) {
			(initialFocusRef as { current: HTMLInputElement | null }).current = el;
		}
	};

	let formattedExecutedAt = purchaseResult?.executedAt ?? "";
	if (purchaseResult?.executedAt) {
		try {
			const date = new Date(purchaseResult.executedAt);
			if (!Number.isNaN(date.getTime())) {
				formattedExecutedAt = dateTimeFormatter.format(date);
			}
		} catch {
			// Conserva string
		}
	}

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
					<ShoppingCart aria-hidden="true" className={styles.modal__symbol} />
					<div className={styles.modal__heading}>
						<h3 className={styles.modal__title}>Comprar tokens</h3>
						<p>Adquirí tokens del jugador seleccionado.</p>
					</div>
					<button
						type="button"
						aria-label="Cerrar ventana de compra"
						onClick={onClose}
						disabled={isBusy}
						className={styles.modal__closeButton}
					>
						<X aria-hidden="true" />
					</button>
				</header>

				<div className={styles.modal__content}>
					<section
						className={styles.modal__player}
						aria-label="Jugador seleccionado"
					>
						<MarketPlayerSummary player={player} />
					</section>
					<div className={styles.modal__prices}>
						{/* Cotización actual */}
						<div className={styles.modal__quoteCard}>
							<Coins aria-hidden="true" className={styles.modal__quoteIcon} />
							<span className={styles.modal__quoteLabel}>Precio actual</span>
							<MarketQuoteFeedback
								status={quoteStatus}
								httpStatus={quoteHttpStatus}
								canRetry={canRetryQuote}
								onRetry={onRetryQuote}
							/>
							{quoteStatus === "available" && quote && (
								<>
									<div className={styles.modal__quotePriceRow}>
										<span className={styles.modal__quoteCredits}>
											{formatCredits(quote.price)}
										</span>
										<span className={styles.modal__quoteCentimes}>
											({formatCentimes(quote.price)})
										</span>
									</div>
									<p className={styles.modal__quoteCalculated}>
										{formatQuoteCalculatedAt(quote.calculatedAt)}
									</p>
								</>
							)}
						</div>
						<div className={styles.modal__variation}>
							<p>Variación (24h)</p>
							<strong>Sin datos</strong>
						</div>
					</div>

					{/* Alerta de error */}
					{errorMessage && !isSuccess && (
						<div role="alert" className={styles.modal__errorAlert}>
							{errorMessage}
						</div>
					)}

					{/* Estado de éxito */}
					{isSuccess && purchaseResult ? (
						<div role="status" className={styles.modal__successCard}>
							<h4 className={styles.modal__successTitle}>
								¡Compra confirmada!
							</h4>
							<p className={styles.modal__successSub}>
								Transacción #{purchaseResult.transactionId}
							</p>
							<div className={styles.modal__successDetails}>
								<div className={styles.modal__detailRow}>
									<span>Tokens adquiridos:</span>
									<strong>{purchaseResult.quantity}</strong>
								</div>
								<div className={styles.modal__detailRow}>
									<span>Precio unitario:</span>
									<strong>{formatCredits(purchaseResult.unitPrice)}</strong>
								</div>
								<div className={styles.modal__detailRow}>
									<span>Total abonado:</span>
									<strong>{formatCredits(purchaseResult.totalAmount)}</strong>
								</div>
								<div className={styles.modal__detailRow}>
									<span>Ejecutado el:</span>
									<span>{formattedExecutedAt}</span>
								</div>
							</div>
						</div>
					) : (
						/* Formulario de cantidad y estimación */
						<form
							onSubmit={(e) => {
								e.preventDefault();
								onConfirm();
							}}
							className={styles.modal__form}
						>
							<div className={styles.modal__quantity}>
								<span className={styles.modal__quantityLabel}>
									<Layers3 aria-hidden="true" /> Cantidad de tokens
								</span>
								<button
									type="button"
									disabled
									aria-label="Disminuir cantidad: pendiente"
								>
									<Minus aria-hidden="true" />
								</button>
								<Field
									id="buy-token-quantity"
									label="Cantidad de tokens (1 a 100)"
									type="text"
									inputMode="numeric"
									autoComplete="off"
									disabled={isBusy}
									error={formError}
									ref={setInputRef}
									className={styles.modal__quantityInput}
									{...quantityProps}
								/>
								<button
									type="button"
									disabled
									aria-label="Aumentar cantidad: pendiente"
								>
									<Plus aria-hidden="true" />
								</button>
							</div>
							<fieldset className={styles.modal__quickAmounts}>
								<legend className={styles.modal__quickAmountsLegend}>
									Cantidades rápidas pendientes
								</legend>
								{[1, 5, 10, 50, 100].map((amount) => (
									<button
										type="button"
										disabled
										key={amount}
										aria-label={`${amount} tokens: pendiente`}
									>
										{amount}
									</button>
								))}
							</fieldset>
							<p className={styles.modal__pending}>
								−/+ y cantidades rápidas: pendientes. Podés editar la cantidad.
							</p>

							{/* Resumen de estimación */}
							{quote && quote.price > 0 && estimation && (
								<div className={styles.modal__estimation}>
									<div className={styles.modal__detailRow}>
										<span>Precio por token</span>
										<strong>{formatCredits(quote.price)}</strong>
									</div>
									<div className={styles.modal__detailRow}>
										<span>Cantidad de tokens</span>
										<strong>{estimation.quantity}</strong>
									</div>
									<div className={styles.modal__estimationRow}>
										<span className={styles.modal__estimationLabel}>
											Importe estimado:
										</span>
										<div className={styles.modal__estimationValues}>
											<span className={styles.modal__estimationCredits}>
												{formatCredits(estimation.totalCentimes)}
											</span>
											<span className={styles.modal__estimationCentimes}>
												({formatCentimes(estimation.totalCentimes)})
											</span>
										</div>
									</div>
									<p className={styles.modal__estimationDisclaimer}>
										Estimación orientativa. El precio definitivo y la
										disponibilidad de stock son confirmados por el servidor al
										procesar la compra.
									</p>
								</div>
							)}
						</form>
					)}
				</div>

				{confirmationBlockReason && !isSuccess && (
					<p
						id="market-buy-block-reason"
						role="status"
						className={styles.modal__blockReason}
					>
						{confirmationBlockReason}
					</p>
				)}
				<footer className={styles.modal__footer}>
					{isSuccess ? (
						<Button
							type="button"
							onClick={onClose}
							className={styles.modal__btnPrimary}
						>
							Cerrar
						</Button>
					) : (
						<>
							<Button
								type="button"
								onClick={onClose}
								disabled={isBusy}
								className={styles.modal__btnSecondary}
							>
								Cancelar
							</Button>
							<Button
								type="button"
								onClick={onConfirm}
								disabled={!canConfirm || isBusy}
								className={styles.modal__btnPrimary}
								aria-busy={isBusy ? "true" : undefined}
								aria-describedby={
									confirmationBlockReason
										? "market-buy-block-reason"
										: undefined
								}
							>
								{isBusy ? (
									<LoaderCircle
										aria-hidden="true"
										className={styles.modal__spinner}
									/>
								) : (
									<ShoppingCart aria-hidden="true" />
								)}
								{isBusy ? "Procesando…" : "Confirmar compra"}
							</Button>
						</>
					)}
				</footer>
			</div>
		</Modal>
	);
}
