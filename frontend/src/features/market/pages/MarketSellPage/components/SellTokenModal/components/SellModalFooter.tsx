import { Button } from "@/shared/components";
import { LoaderCircle, Tag } from "@/shared/icons";
import styles from "./SellModalFooter.module.scss";

interface Props {
	isSuccess: boolean;
	isBusy: boolean;
	canConfirm: boolean;
	onClose: () => void;
	onConfirm: () => void;
}

export default function SellModalFooter({
	isSuccess,
	isBusy,
	canConfirm,
	onClose,
	onConfirm,
}: Props) {
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
						) : (
							<Tag aria-hidden="true" />
						)}
						<span>{isBusy ? "Publicando…" : "Confirmar publicación"}</span>
					</Button>
				</>
			)}
		</footer>
	);
}
