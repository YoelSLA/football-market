import type { EnrichedUserOrder } from "@/features/market/hooks";
import { Button, Modal, TokenSymbol } from "@/shared/components";
import styles from "./CancelListingModal.module.scss";
import { CancelListingDetails } from "./components";

interface Props {
	isOpen: boolean;
	order: EnrichedUserOrder | null;
	isBusy: boolean;
	onClose: () => void;
	onConfirm: () => void;
}

export default function CancelListingModal({
	isOpen,
	order,
	isBusy,
	onClose,
	onConfirm,
}: Props) {
	if (!order) return null;

	return (
		<Modal
			isOpen={isOpen}
			onClose={onClose}
			title="Cancelar publicación de venta"
			isBusy={isBusy}
		>
			<div className={styles.cancelModal}>
				<div className={styles.cancelModal__header}>
					<h3 className={styles.cancelModal__title}>
						Retirar orden del mercado
					</h3>
				</div>

				<div className={styles.cancelModal__body}>
					<p>¿Estás seguro de que deseás retirar esta orden de venta activa?</p>

					<CancelListingDetails order={order} />

					<div className={styles.cancelModal__warning}>
						Los {order.quantity} <TokenSymbol /> reservados se liberarán
						inmediatamente y volverán a estar disponibles en tu cartera.
					</div>
				</div>

				<div className={styles.cancelModal__footer}>
					<Button
						type="button"
						variant="secondary"
						onClick={onClose}
						disabled={isBusy}
						className={styles.cancelModal__cancelBtn}
					>
						Conservar orden
					</Button>

					<Button
						type="button"
						onClick={onConfirm}
						disabled={isBusy}
						className={styles.cancelModal__confirmBtn}
					>
						{isBusy ? "Retirando..." : "Confirmar retiro"}
					</Button>
				</div>
			</div>
		</Modal>
	);
}
