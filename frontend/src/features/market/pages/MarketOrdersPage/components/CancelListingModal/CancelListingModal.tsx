import type { EnrichedUserOrder } from "@/features/market/hooks";
import { formatCreditsAmount } from "@/features/market/utils";
import { Modal } from "@/shared/components";
import styles from "./CancelListingModal.module.scss";

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

					<ul className={styles.cancelModal__details}>
						<li>
							<span>Jugador:</span>
							<strong>
								{order.playerName}
								{order.teamName ? ` (${order.teamName})` : ""}
							</strong>
						</li>
						<li>
							<span>Lote publicado:</span>
							<strong>
								{order.quantity} {order.quantity === 1 ? "token" : "tokens"}
							</strong>
						</li>
						<li>
							<span>Precio unitario:</span>
							<strong>{formatCreditsAmount(order.unitPrice)}</strong>
						</li>
						<li>
							<span>Total del lote:</span>
							<strong>{formatCreditsAmount(order.totalAmount)}</strong>
						</li>
					</ul>

					<div className={styles.cancelModal__warning}>
						Los {order.quantity} tokens reservados se liberarán inmediatamente y
						volverán a estar disponibles en tu cartera.
					</div>
				</div>

				<div className={styles.cancelModal__footer}>
					<button
						type="button"
						onClick={onClose}
						disabled={isBusy}
						className={styles.cancelModal__cancelBtn}
					>
						Conservar orden
					</button>

					<button
						type="button"
						onClick={onConfirm}
						disabled={isBusy}
						className={styles.cancelModal__confirmBtn}
					>
						{isBusy ? "Retirando..." : "Confirmar retiro"}
					</button>
				</div>
			</div>
		</Modal>
	);
}
