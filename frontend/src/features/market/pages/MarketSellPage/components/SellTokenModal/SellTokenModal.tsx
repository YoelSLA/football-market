import type { RefObject } from "react";
import type { UseFormRegister } from "react-hook-form";
import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import TokenQuantityStepper from "@/features/market/components/TokenQuantityStepper";
import type {
	MarketPlayer,
	MarketQuote,
	MarketQuoteStatus,
	SellEstimation,
	SellQuantityForm,
	SellResult,
} from "@/features/market/types";
import { Modal } from "@/shared/components";
import {
	SellModalEstimation,
	SellModalFooter,
	SellModalHeader,
	SellModalHoldingInfo,
	SellModalSuccess,
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
			className={styles.dialog}
			isOpen={props.isOpen}
			onClose={props.onClose}
			title={`Publicar venta de ${player.name}`}
			closeOnEsc={!props.isBusy}
			closeOnOverlayClick={!props.isBusy}
			isBusy={props.isBusy}
			triggerRef={props.triggerRef}
			initialFocusRef={props.initialFocusRef}
		>
			<section className={styles.modal} aria-label="Publicar venta de tokens">
				<SellModalHeader onClose={props.onClose} disabled={props.isBusy} />

				<div className={styles.modal__content}>
					<div className={styles.modal__playerCard}>
						<MarketPlayerSummary player={player} variant="officialCard" />
					</div>

					<SellModalHoldingInfo
						quote={props.quote}
						quoteStatus={props.quoteStatus}
						quoteHttpStatus={props.quoteHttpStatus}
						availableQuantity={props.availableQuantity}
						reservedQuantity={props.reservedQuantity}
						canRetryQuote={props.canRetryQuote}
						onRetryQuote={props.onRetryQuote}
					/>

					{props.errorMessage && (
						<div role="alert" className={styles.modal__error}>
							<p>{props.errorMessage}</p>
						</div>
					)}

					{props.result ? (
						<SellModalSuccess result={props.result} playerName={player.name} />
					) : (
						<div className={styles.modal__form}>
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
								<SellModalEstimation estimation={props.estimation} />
							)}
						</div>
					)}

					{props.confirmationBlockReason && !props.result && (
						<div role="status" className={styles.modal__blockReason}>
							<p>{props.confirmationBlockReason}</p>
						</div>
					)}
				</div>

				<SellModalFooter
					isSuccess={Boolean(props.result)}
					isBusy={props.isBusy}
					canConfirm={props.canConfirm}
					onClose={props.onClose}
					onConfirm={props.onConfirm}
				/>
			</section>
		</Modal>
	);
}
