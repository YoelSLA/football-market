import { formatCredits } from "@/features/market/utils";
import { Button } from "@/shared/components";
import { LoaderCircle, ShoppingCart } from "@/shared/icons";
import styles from "./BuyModalFooter.module.scss";

interface Props {
	isSuccess: boolean;
	isBusy: boolean;
	canConfirm: boolean;
	totalCentimes: number;
	onClose: () => void;
	onConfirm: () => void;
}

export function BuyModalFooter({
	isSuccess,
	isBusy,
	canConfirm,
	totalCentimes,
	onClose,
	onConfirm,
}: Props) {
	return (
		<footer className={styles.footer}>
			<Button
				type="button"
				className={styles.btnSecondary}
				onClick={onClose}
				disabled={isBusy}
			>
				{isSuccess ? "Cerrar" : "Cancelar"}
			</Button>
			{!isSuccess && (
				<Button
					type="button"
					className={styles.btnPrimary}
					onClick={onConfirm}
					disabled={!canConfirm || isBusy}
					aria-busy={isBusy}
				>
					{isBusy ? (
						<LoaderCircle className={styles.spinner} aria-hidden="true" />
					) : (
						<ShoppingCart aria-hidden="true" />
					)}
					<span>
						{isBusy
							? "Procesando…"
							: `Confirmar compra (${formatCredits(totalCentimes)})`}
					</span>
				</Button>
			)}
		</footer>
	);
}
