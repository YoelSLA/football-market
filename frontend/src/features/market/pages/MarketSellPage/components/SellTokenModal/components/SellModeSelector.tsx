import type { SellMode } from "@/features/market/types";
import styles from "./SellModeSelector.module.scss";

interface Props {
	mode: SellMode;
	onChange: (mode: SellMode) => void;
	disabled?: boolean;
}

export default function SellModeSelector({ mode, onChange, disabled }: Props) {
	return (
		<div
			className={styles.selector}
			role="tablist"
			aria-label="Modalidad de venta"
		>
			<button
				type="button"
				role="tab"
				id="tab-official"
				aria-selected={mode === "OFFICIAL"}
				aria-controls="panel-official"
				className={mode === "OFFICIAL" ? styles.tab_active : styles.tab}
				disabled={disabled}
				onClick={() => onChange("OFFICIAL")}
			>
				<span className={styles.tab__title}>⚡ Venta Inmediata Oficial</span>
				<span className={styles.tab__badgeOfficial}>(superusuario)</span>
			</button>
			<button
				type="button"
				role="tab"
				id="tab-p2p"
				aria-selected={mode === "P2P"}
				aria-controls="panel-p2p"
				className={mode === "P2P" ? styles.tab_active : styles.tab}
				disabled={disabled}
				onClick={() => onChange("P2P")}
			>
				<span className={styles.tab__title}>
					👥 Publicación en Comunidad P2P
				</span>
				<span className={styles.tab__badgeP2P}>(mercado secundario)</span>
			</button>
		</div>
	);
}
