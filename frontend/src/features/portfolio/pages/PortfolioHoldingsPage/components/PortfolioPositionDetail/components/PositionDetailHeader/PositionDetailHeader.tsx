import type { Ref } from "react";
import { Button } from "@/shared/components";
import { X } from "@/shared/icons";
import styles from "./PositionDetailHeader.module.scss";

interface Props {
	onClose: () => void;
	closeRef?: Ref<HTMLButtonElement>;
}

export default function PositionDetailHeader({ onClose, closeRef }: Props) {
	return (
		<header className={styles.header}>
			<div>
				<h2 id="portfolio-detail-title">Detalle de la posición</h2>
				<p id="portfolio-detail-description">
					Información financiera y actividad del jugador
				</p>
			</div>
			<Button
				unstyled
				ref={closeRef}
				type="button"
				onClick={onClose}
				aria-label="Cerrar detalle de la posición"
				className={styles.closeButton}
			>
				<X size={20} aria-hidden="true" />
			</Button>
		</header>
	);
}
