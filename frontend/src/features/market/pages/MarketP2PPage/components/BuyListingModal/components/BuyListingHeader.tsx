import { Button } from "@/shared/components";
import { X } from "@/shared/icons";
import styles from "./BuyListingHeader.module.scss";

interface Props {
	listingId: number;
	isBusy: boolean;
	onClose: () => void;
}

export function BuyListingHeader({ listingId, isBusy, onClose }: Props) {
	return (
		<header className={styles.header}>
			<div className={styles.heading}>
				<h3>Confirmar compra P2P</h3>
				<span>Publicación #{listingId}</span>
			</div>
			<Button
				type="button"
				className={styles.close}
				unstyled
				aria-label="Cerrar confirmación de compra"
				onClick={onClose}
				disabled={isBusy}
			>
				<X aria-hidden="true" />
			</Button>
		</header>
	);
}
