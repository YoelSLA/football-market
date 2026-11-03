import type { RefObject } from "react";
import type { UseFormRegister } from "react-hook-form";
import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import MarketQuoteFeedback from "@/features/market/components/MarketQuoteFeedback";
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
import { Button, Field, Modal } from "@/shared/components";
import { LoaderCircle, Minus, Plus, Tag } from "@/shared/icons";
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
						<p>Vence: {formatQuoteCalculatedAt(props.result.expiresAt)}</p>
						<p>Publicación #{props.result.listingId}</p>
					</div>
				) : (
					<>
						<div className={styles.sellModal__quantity}>
							<span>Cantidad (máximo {props.maximum})</span>
							<span>
								<button
									type="button"
									aria-label="Disminuir cantidad"
									disabled={props.isBusy}
									onClick={() =>
										props.onQuantityChange(
											Math.max(1, Number(props.quantity) - 1),
										)
									}
								>
									<Minus />
								</button>
								<Field
									id="sell-token-quantity"
									label="Cantidad entre 1 y 100"
									type="text"
									inputMode="numeric"
									autoComplete="off"
									disabled={props.isBusy}
									error={props.formError}
									ref={setRef}
									{...quantityProps}
								/>
								<button
									type="button"
									aria-label="Aumentar cantidad"
									disabled={
										props.isBusy || Number(props.quantity) >= props.maximum
									}
									onClick={() =>
										props.onQuantityChange(
											Math.min(props.maximum, Number(props.quantity) + 1),
										)
									}
								>
									<Plus />
								</button>
							</span>
						</div>
						<div className={styles.sellModal__presets}>
							{props.presets.map((value) => (
								<button
									type="button"
									key={value}
									disabled={props.isBusy}
									onClick={() => props.onQuantityChange(value)}
								>
									{value === props.maximum ? "Máx." : value}
								</button>
							))}
						</div>
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
									{formatCentimes(props.estimation.totalCentimes)} centésimas)
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
