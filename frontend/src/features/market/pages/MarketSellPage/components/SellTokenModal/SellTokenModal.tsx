import type { RefObject } from "react";
import type { UseFormRegister } from "react-hook-form";
import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import MarketQuoteFeedback from "@/features/market/components/MarketQuoteFeedback";
import TokenQuantityStepper from "@/features/market/components/TokenQuantityStepper";
import type {
	MarketPlayer,
	MarketQuote,
	MarketQuoteStatus,
	SellEstimation,
	SellQuantityForm,
	SellResult,
} from "@/features/market/types";
import {
	formatCentimes,
	formatCredits,
	formatQuoteCalculatedAt,
} from "@/features/market/utils";
import { Button, Modal } from "@/shared/components";
import { CheckCircle2, LoaderCircle, Tag, X } from "@/shared/icons";
import styles from "./SellTokenModal.module.scss";

interface Props {
	isOpen: boolean;
	player: MarketPlayer | null;
	availableQuantity: number;
	reservedQuantity: number;
	quote: MarketQuote | null;
	quoteStatus: MarketQuoteStatus;
	quoteHttpStatus?: number;
	canRetryQuote: boolean;
	onRetryQuote: () => void;
	estimation: SellEstimation | null;
	formRegister: UseFormRegister<SellQuantityForm>;
	formError?: string;
	quantity: string;
	maximum: number;
	presets: number[];
	onQuantityChange: (quantity: number) => void;
	result: SellResult | null;
	errorMessage: string | null;
	confirmationBlockReason: string | null;
	isBusy: boolean;
	canConfirm: boolean;
	onClose: () => void;
	onConfirm: () => void;
	triggerRef: RefObject<HTMLElement | null>;
	initialFocusRef: RefObject<HTMLInputElement | null>;
}

const expiryDateFormatter = new Intl.DateTimeFormat("es-AR", {
	day: "2-digit",
	month: "2-digit",
	year: "numeric",
	hour: "2-digit",
	minute: "2-digit",
});

function formatExpiryDate(expiresAt: string): string {
	const date = new Date(expiresAt);
	return Number.isNaN(date.getTime())
		? expiresAt
		: expiryDateFormatter.format(date);
}

export default function SellTokenModal(props: Props) {
	const { player } = props;
	if (!player) return null;
	const { ref, ...quantityProps } = props.formRegister("quantity");
	const setRef = (element: HTMLInputElement | null) => {
		ref(element);
		props.initialFocusRef.current = element;
	};

	const quote = props.quote;
	const isQuoteAvailable = props.quoteStatus === "available" && quote !== null;

	return (
		<Modal
			isOpen={props.isOpen}
			onClose={props.onClose}
			title={`Publicar venta de ${player.name}`}
			closeOnEsc={!props.isBusy}
			closeOnOverlayClick={!props.isBusy}
			isBusy={props.isBusy}
			triggerRef={props.triggerRef}
			initialFocusRef={props.initialFocusRef}
		>
			<section className={styles.sellModal}>
				<header className={styles.sellModal__header}>
					<div className={styles.sellModal__symbol}>
						<Tag aria-hidden="true" />
					</div>
					<div className={styles.sellModal__heading}>
						<h3>Publicar venta</h3>
						<p>Prepará la oferta para el mercado secundario.</p>
					</div>
					<Button
						unstyled
						type="button"
						className={styles.sellModal__closeButton}
						aria-label="Cerrar modal"
						onClick={props.onClose}
						disabled={props.isBusy}
					>
						<X aria-hidden="true" />
					</Button>
				</header>

				<div className={styles.sellModal__content}>
					<div className={styles.sellModal__player}>
						<MarketPlayerSummary player={player} />
					</div>

					<div className={styles.sellModal__metrics}>
						<div className={styles.sellModal__metricTile}>
							<span className={styles.sellModal__metricLabel}>
								Cotización vigente
							</span>
							<strong className={styles.sellModal__metricValue}>
								{isQuoteAvailable ? formatCredits(quote.price) : "—"}
							</strong>
							<span className={styles.sellModal__metricSubtext}>
								{isQuoteAvailable
									? `${formatCentimes(quote.price)} · ${formatQuoteCalculatedAt(
											quote.calculatedAt,
										)}`
									: props.quoteStatus === "loading"
										? "Consultando cotización…"
										: "Sin cotización"}
							</span>
						</div>

						<div className={styles.sellModal__metricTile}>
							<span className={styles.sellModal__metricLabel}>
								Tokens disponibles
							</span>
							<strong
								className={`${styles.sellModal__metricValue} ${
									props.availableQuantity > 0
										? styles.sellModal__metricValue_available
										: ""
								}`}
							>
								{props.availableQuantity}
							</strong>
							<span className={styles.sellModal__metricSubtext}>
								{props.reservedQuantity} en reserva · Total:{" "}
								{props.availableQuantity + props.reservedQuantity}
							</span>
						</div>
					</div>

					<MarketQuoteFeedback
						status={props.quoteStatus}
						httpStatus={props.quoteHttpStatus}
						canRetry={props.canRetryQuote}
						onRetry={props.onRetryQuote}
					/>

					{props.errorMessage && (
						<div role="alert" className={styles.sellModal__errorAlert}>
							<p>{props.errorMessage}</p>
						</div>
					)}

					{props.result ? (
						<div role="status" className={styles.sellModal__successCard}>
							<div className={styles.sellModal__successHeader}>
								<CheckCircle2 aria-hidden="true" />
								<div>
									<h4>Publicación confirmada</h4>
									<p>Orden registrada #{props.result.listingId}</p>
								</div>
							</div>
							<div className={styles.sellModal__successDetails}>
								<div className={styles.sellModal__successRow}>
									<span>Jugador</span>
									<strong>{player.name}</strong>
								</div>
								<div className={styles.sellModal__successRow}>
									<span>Cantidad publicada</span>
									<strong>
										{props.result.quantity}{" "}
										{props.result.quantity === 1 ? "token" : "tokens"}
									</strong>
								</div>
								<div className={styles.sellModal__successRow}>
									<span>Precio unitario</span>
									<strong>{formatCredits(props.result.unitPrice)}</strong>
								</div>
								<div className={styles.sellModal__successRow}>
									<span>Importe total</span>
									<strong className={styles.sellModal__successTotal}>
										{formatCredits(props.result.totalAmount)}
									</strong>
								</div>
								<div className={styles.sellModal__successRow}>
									<span>Vencimiento</span>
									<span>{formatExpiryDate(props.result.expiresAt)}</span>
								</div>
							</div>
						</div>
					) : (
						<div className={styles.sellModal__form}>
							<TokenQuantityStepper
								id="sell-token-quantity"
								label={`Cantidad a publicar (máx. ${props.maximum})`}
								quantity={Number(props.quantity)}
								min={1}
								max={props.maximum}
								disabled={props.isBusy}
								error={props.formError}
								inputRef={setRef}
								inputProps={quantityProps}
								presets={props.presets.map((val) => ({
									label: val === props.maximum ? "Máx." : String(val),
									value: val,
								}))}
								onQuantityChange={props.onQuantityChange}
							/>

							{props.estimation && (
								<div className={styles.sellModal__estimateCard}>
									<div className={styles.sellModal__estimateRow}>
										<span>Cantidad</span>
										<strong>
											{props.estimation.quantity}{" "}
											{props.estimation.quantity === 1 ? "token" : "tokens"}
										</strong>
									</div>
									<div className={styles.sellModal__estimateRow}>
										<span>Precio unitario estimado</span>
										<span>
											{formatCredits(props.estimation.unitPriceCentimes)}
										</span>
									</div>
									<div className={styles.sellModal__estimateDivider} />
									<div className={styles.sellModal__estimateTotalRow}>
										<div className={styles.sellModal__estimateTotalHeader}>
											<span className={styles.sellModal__estimateTotalLabel}>
												Importe estimado
											</span>
											<span className={styles.sellModal__estimateNotice}>
												El precio definitivo lo determina el servidor al
												publicar.
											</span>
										</div>
										<div className={styles.sellModal__estimateTotalAmount}>
											<strong>
												{formatCredits(props.estimation.totalCentimes)}
											</strong>
											<small>
												({formatCentimes(props.estimation.totalCentimes)})
											</small>
										</div>
									</div>
								</div>
							)}
						</div>
					)}

					{props.confirmationBlockReason && !props.result && (
						<div role="status" className={styles.sellModal__blockReason}>
							<p>{props.confirmationBlockReason}</p>
						</div>
					)}
				</div>

				<footer className={styles.sellModal__footer}>
					{props.result ? (
						<Button
							type="button"
							className={styles.sellModal__closeAction}
							onClick={props.onClose}
						>
							Cerrar
						</Button>
					) : (
						<>
							<Button
								type="button"
								className={styles.sellModal__cancelBtn}
								disabled={props.isBusy}
								onClick={props.onClose}
							>
								Cancelar
							</Button>
							<Button
								type="button"
								className={styles.sellModal__confirmBtn}
								disabled={!props.canConfirm || props.isBusy}
								aria-busy={props.isBusy}
								onClick={props.onConfirm}
							>
								{props.isBusy ? (
									<LoaderCircle className={styles.sellModal__spinner} />
								) : (
									<Tag aria-hidden="true" />
								)}
								<span>
									{props.isBusy ? "Publicando…" : "Confirmar publicación"}
								</span>
							</Button>
						</>
					)}
				</footer>
			</section>
		</Modal>
	);
}
