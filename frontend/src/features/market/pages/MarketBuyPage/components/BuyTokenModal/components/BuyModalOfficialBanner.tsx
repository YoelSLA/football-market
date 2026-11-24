import styles from "./BuyModalOfficialBanner.module.scss";

export function BuyModalOfficialBanner() {
	return (
		<div className={styles.banner}>
			<div className={styles.header}>
				<span>⚡ Emisión Oficial Primaria</span>
				<span className={styles.badge}>Garantizado</span>
			</div>
			<p className={styles.text}>
				Compra directa a la emisión oficial con acreditación instantánea de
				tokens en tu cartera.
			</p>
		</div>
	);
}
