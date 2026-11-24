import type { SellMode } from "@/features/market/types";
import { Button } from "@/shared/components";
import { LoaderCircle, Tag, Zap } from "@/shared/icons";
import styles from "./SellModalFooter.module.scss";

interface Props {
	isSuccess: boolean;
	isBusy: boolean;
	canConfirm: boolean;
	sellMode?: SellMode;
	onClose: () => void;
	onConfirm: () => void;
}

export default function SellModalFooter({
	isSuccess,
	isBusy,
	canConfirm,
	sellMode = "OFFICIAL",
	onClose,
	onConfirm,
}: Props) {
	const isOfficial = sellMode === "OFFICIAL";
	const busyLabel = isOfficial ? "Vendiendo…" : "Publicando…";
	const confirmLabel = isOfficial
		? "Confirmar venta inmediata"
		: "Publicar en P2P";

	return (
		<footer className={styles.footer}>
			{isSuccess ? (
				<Button
					type="button"
					className={styles.footer__closeButton}
					onClick={onClose}
				>
					Cerrar
				</Button>
			) : (
				<>
					<Button
						type="button"
						className={styles.footer__cancelButton}
						disabled={isBusy}
						onClick={onClose}
					>
						Cancelar
					</Button>
					<Button
						type="button"
						className={styles.footer__confirmButton}
						disabled={!canConfirm || isBusy}
						aria-busy={isBusy}
						onClick={onConfirm}
					>
						{isBusy ? (
							<LoaderCircle className={styles.footer__spinner} />
						) : isOfficial ? (
							<Zap aria-hidden="true" />
						) : (
							<Tag aria-hidden="true" />
						)}
						<span>{isBusy ? busyLabel : confirmLabel}</span>
					</Button>
				</>
			)}
		</footer>
	);
}
