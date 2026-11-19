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
import { LoaderCircle, Tag } from "@/shared/icons";
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
					<Tag aria-hidden="true" />
					<div>
						<h2>Vender tokens</h2>
						<p>Prepará la publicación para el mercado.</p>
					</div>
				</header>
				<MarketPlayerSummary player={player} />
				<p className={styles.sellModal__holding}>
					Disponibles: {props.availableQuantity} · Reservados:{" "}
					{props.reservedQuantity}
				</p>
				<MarketQuoteFeedback
					status={props.quoteStatus}
					httpStatus={props.quoteHttpStatus}
					canRetry={props.canRetryQuote}
					onRetry={props.onRetryQuote}
				/>
				{props.quote && (
					<p>
						Precio vigente: {formatCredits(props.quote.price)} (
						{formatCentimes(props.quote.price)}) ·{" "}
						{formatQuoteCalculatedAt(props.quote.calculatedAt)}
					</p>
				)}
				{props.errorMessage && (
					<p role="alert" className={styles.sellModal__error}>
						{props.errorMessage}
					</p>
				)}
				{props.result ? (
					<div role="status" className={styles.sellModal__success}>
						<h3>Publicación confirmada</h3>
						<p>
							{props.result.quantity} tokens de {player.name}
						</p>
						<p>Precio unitario: {formatCredits(props.result.unitPrice)}</p>
						<p>Importe total: {formatCredits(props.result.totalAmount)}</p>
						<p>Vence: {formatExpiryDate(props.result.expiresAt)}</p>
						<p>Publicación #{props.result.listingId}</p>
					</div>
				) : (
					<>
						<TokenQuantityStepper
							id="sell-token-quantity"
							label={`Cantidad (máximo ${props.maximum})`}
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
							<section className={styles.sellModal__estimate}>
								<p>Jugador: {player.name}</p>
								<p>Cantidad: {props.estimation.quantity}</p>
								<p>
									Precio unitario estimado:{" "}
									{formatCredits(props.estimation.unitPriceCentimes)}
								</p>
								<strong>
									Importe estimado:{" "}
									{formatCredits(props.estimation.totalCentimes)} (
									{formatCentimes(props.estimation.totalCentimes)})
								</strong>
								<small>
									El precio definitivo lo determina el servidor al publicar.
								</small>
							</section>
						)}
					</>
				)}
				{props.confirmationBlockReason && !props.result && (
					<p role="status">{props.confirmationBlockReason}</p>
				)}
				<footer className={styles.sellModal__footer}>
					{props.result ? (
						<Button type="button" onClick={props.onClose}>
							Cerrar
						</Button>
					) : (
						<>
							<Button
								type="button"
								disabled={props.isBusy}
								onClick={props.onClose}
							>
								Cancelar
							</Button>
							<Button
								type="button"
								disabled={!props.canConfirm || props.isBusy}
								aria-busy={props.isBusy}
								onClick={props.onConfirm}
							>
								{props.isBusy ? (
									<LoaderCircle className={styles.sellModal__spinner} />
								) : (
									<Tag />
								)}{" "}
								{props.isBusy ? "Publicando…" : "Confirmar publicación"}
							</Button>
						</>
					)}
				</footer>
			</section>
		</Modal>
	);
}
