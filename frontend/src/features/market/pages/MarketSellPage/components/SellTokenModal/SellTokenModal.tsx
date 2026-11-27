import type { RefObject } from "react";
import type { UseFormRegister } from "react-hook-form";
import TokenQuantityStepper from "@/features/market/components/TokenQuantityStepper";
import type {
	MarketPlayer,
	MarketQuote,
	MarketQuoteStatus,
	SellEstimation,
	SellMode,
	SellOfficialEstimation,
	SellOfficialResult,
	SellQuantityForm,
	SellResult,
} from "@/features/market/types";
import { Modal } from "@/shared/components";
import { TOKEN_SYMBOL } from "@/shared/constants";
import {
	P2PPriceSelector,
	SellModalFooter,
	SellModalHeader,
	SellModalHeroCard,
	SellModalOfficialEstimation,
	SellModalSuccess,
	SellModeSelector,
} from "./components";
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
	officialEstimation?: SellOfficialEstimation | null;
	sellMode: SellMode;
	onSellModeChange: (mode: SellMode) => void;
	p2pUnitPriceCentimes: number;
	onP2PUnitPriceChange: (price: number) => void;
	formRegister: UseFormRegister<SellQuantityForm>;
	formError?: string;
	quantity: string;
	maximum: number;
	presets: number[];
	onQuantityChange: (quantity: number) => void;
	result: SellResult | SellOfficialResult | null;
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

	const modalTitle = `${
		props.sellMode === "OFFICIAL"
			? "Venta inmediata oficial de"
			: "Publicar venta de"
	} ${player.name}`;

	return (
		<Modal
			className={styles.dialog}
			isOpen={props.isOpen}
			onClose={props.onClose}
			title={modalTitle}
			closeOnEsc={!props.isBusy}
			closeOnOverlayClick={!props.isBusy}
			isBusy={props.isBusy}
			triggerRef={props.triggerRef}
			initialFocusRef={props.initialFocusRef}
		>
			<section
				className={styles.modal}
				aria-label={
					props.sellMode === "OFFICIAL"
						? `Venta inmediata oficial de ${TOKEN_SYMBOL}`
						: `Publicar venta de ${TOKEN_SYMBOL}`
				}
			>
				<SellModalHeader onClose={props.onClose} disabled={props.isBusy} />

				<div className={styles.modal__content}>
					<div className={styles.modal__grid}>
						{/* Columna Izquierda (Opción A): Ficha Hero del Jugador */}
						<div className={styles.modal__heroCol}>
							<SellModalHeroCard
								player={player}
								quote={props.quote}
								quoteStatus={props.quoteStatus}
								quoteHttpStatus={props.quoteHttpStatus}
								availableQuantity={props.availableQuantity}
								reservedQuantity={props.reservedQuantity}
								canRetryQuote={props.canRetryQuote}
								onRetryQuote={props.onRetryQuote}
							/>
						</div>

						{/* Columna Derecha (Opción A): Consola de Venta y Estimación */}
						<div className={styles.modal__formCol}>
							{props.errorMessage && (
								<div role="alert" className={styles.modal__error}>
									<p>{props.errorMessage}</p>
								</div>
							)}

							{props.result ? (
								<SellModalSuccess
									result={props.result}
									playerName={player.name}
								/>
							) : (
								<div className={styles.modal__form}>
									<SellModeSelector
										mode={props.sellMode}
										onChange={props.onSellModeChange}
										disabled={props.isBusy}
									/>

									<TokenQuantityStepper
										id="sell-token-quantity"
										label={
											props.sellMode === "OFFICIAL"
												? `Cantidad a vender de inmediato (máx. ${props.maximum})`
												: `Cantidad a publicar en comunidad (máx. ${props.maximum})`
										}
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

									<div className={styles.modal__estimationSlot}>
										{props.sellMode === "OFFICIAL"
											? props.officialEstimation && (
													<SellModalOfficialEstimation
														estimation={props.officialEstimation}
													/>
												)
											: props.quote && (
													<P2PPriceSelector
														officialPriceCentimes={props.quote.price}
														unitPriceCentimes={props.p2pUnitPriceCentimes}
														quantity={Number(props.quantity) || 1}
														disabled={props.isBusy}
														onChange={props.onP2PUnitPriceChange}
													/>
												)}
									</div>
								</div>
							)}

							{props.confirmationBlockReason && !props.result && (
								<div role="status" className={styles.modal__blockReason}>
									<p>{props.confirmationBlockReason}</p>
								</div>
							)}
						</div>
					</div>
				</div>

				<SellModalFooter
					isSuccess={Boolean(props.result)}
					isBusy={props.isBusy}
					canConfirm={props.canConfirm}
					sellMode={props.sellMode}
					onClose={props.onClose}
					onConfirm={props.onConfirm}
				/>
			</section>
		</Modal>
	);
}
