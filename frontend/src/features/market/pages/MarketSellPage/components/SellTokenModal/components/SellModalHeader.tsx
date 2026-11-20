import { Button } from "@/shared/components";
import { Tag, X } from "@/shared/icons";
import styles from "./SellModalHeader.module.scss";

interface Props {
	onClose: () => void;
	disabled?: boolean;
}

export default function SellModalHeader({ onClose, disabled }: Props) {
	return (
		<header className={styles.header}>
			<div className={styles.header__symbol}>
				<Tag aria-hidden="true" />
			</div>
			<div className={styles.header__heading}>
				<h3>Publicar orden de venta</h3>
				<p>Definí la cantidad de tokens para el mercado secundario</p>
			</div>
			<Button
				unstyled
				type="button"
				className={styles.header__closeButton}
				aria-label="Cerrar modal"
				onClick={onClose}
				disabled={disabled}
			>
				<X aria-hidden="true" />
			</Button>
		</header>
	);
}
