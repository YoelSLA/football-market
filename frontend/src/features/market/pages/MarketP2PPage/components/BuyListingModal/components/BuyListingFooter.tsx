import { Button } from "@/shared/components";
import styles from "./BuyListingFooter.module.scss";

interface Props {
	isBusy: boolean;
	canConfirm: boolean;
	onClose: () => void;
	onConfirm: () => void;
}

export function BuyListingFooter({
	isBusy,
	canConfirm,
	onClose,
	onConfirm,
}: Props) {
	return (
		<footer className={styles.actions}>
			<Button
				type="button"
				variant="secondary"
				onClick={onClose}
				disabled={isBusy}
			>
				Cancelar
			</Button>
			<Button
				type="button"
				onClick={onConfirm}
				disabled={isBusy || !canConfirm}
				aria-busy={isBusy ? "true" : undefined}
			>
				{isBusy ? "Procesando…" : "Confirmar compra"}
			</Button>
		</footer>
	);
}
