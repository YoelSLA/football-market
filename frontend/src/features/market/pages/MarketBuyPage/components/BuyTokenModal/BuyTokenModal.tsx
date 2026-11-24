import type { RefObject } from "react";
import type { UseFormRegister } from "react-hook-form";
import TokenQuantityStepper from "@/features/market/components/TokenQuantityStepper";
import type {
	BuyEstimation,
	BuyQuantityForm,
	BuyResult,
	OfficialMarketPlayer,
	OfficialMarketQuote,
} from "@/features/market/types";
import { Modal } from "@/shared/components";
import styles from "./BuyTokenModal.module.scss";
import {
	BuyModalFooter,
	BuyModalHeader,
	BuyModalHeroCard,
	BuyModalOfficialBanner,
	BuyModalOfficialEstimation,
	BuyModalSuccess,
} from "./components";

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
	const totalCentimes = estimation
		? estimation.totalCentimes
		: quote.price * safeQuantity;

	return (
		<Modal
			className={styles.dialog}
			isOpen={isOpen}
			onClose={onClose}
			title={`Comprar tokens de ${player.name}`}
			closeOnEsc={!isBusy}
			closeOnOverlayClick={!isBusy}
			isBusy={isBusy}
			triggerRef={triggerRef}
			initialFocusRef={initialFocusRef as RefObject<HTMLElement | null>}
		>
			<section
				className={styles.modal}
				aria-label={`Comprar tokens de ${player.name}`}
			>
				<BuyModalHeader
					playerName={player.name}
					isBusy={isBusy}
					onClose={onClose}
				/>

				<div className={styles.modal__content}>
					<div className={styles.modal__grid}>
						{/* Columna Izquierda: Ficha Hero del Jugador */}
						<div className={styles.modal__heroCol}>
							<BuyModalHeroCard player={player} quote={quote} />
						</div>

						{/* Columna Derecha: Consola de Compra */}
						<div className={styles.modal__formCol}>
							<BuyModalOfficialBanner />

							{errorMessage && !isSuccess && (
								<p role="alert" className={styles.modal__errorAlert}>
									{errorMessage}
								</p>
							)}

							{isSuccess && purchaseResult ? (
								<BuyModalSuccess
									player={player}
									purchaseResult={purchaseResult}
								/>
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
										label="Cantidad de tokens a comprar (1 a 100)"
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

									<BuyModalOfficialEstimation
										quote={quote}
										quantity={safeQuantity}
										totalCentimes={totalCentimes}
									/>
								</form>
							)}

							{confirmationBlockReason && !isSuccess && (
								<p
									id="market-buy-block-reason"
									role="status"
									className={styles.modal__blockReason}
								>
									{confirmationBlockReason}
								</p>
							)}
						</div>
					</div>
				</div>

				<BuyModalFooter
					isSuccess={isSuccess}
					isBusy={isBusy}
					canConfirm={canConfirm}
					totalCentimes={totalCentimes}
					onClose={onClose}
					onConfirm={onConfirm}
				/>
			</section>
		</Modal>
	);
}
