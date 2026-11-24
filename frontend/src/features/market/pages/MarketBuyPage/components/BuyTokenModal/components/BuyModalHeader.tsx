import { Button } from "@/shared/components";
import { ShoppingCart, X } from "@/shared/icons";
import styles from "./BuyModalHeader.module.scss";

interface Props {
	playerName: string;
	isBusy: boolean;
	onClose: () => void;
}

export function BuyModalHeader({ playerName, isBusy, onClose }: Props) {
	return (
		<header className={styles.header}>
			<div className={styles.symbol}>
				<ShoppingCart aria-hidden="true" />
			</div>
			<div className={styles.heading}>
				<h3 className={styles.title}>Comprar tokens de {playerName}</h3>
				<p>Emisión oficial directa al valor de cotización vigente</p>
			</div>
			<Button
				unstyled
				type="button"
				className={styles.closeButton}
				aria-label="Cerrar modal"
				onClick={onClose}
				disabled={isBusy}
			>
				<X aria-hidden="true" />
			</Button>
		</header>
	);
}
