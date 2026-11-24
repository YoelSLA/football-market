import type { ReactNode } from "react";
import styles from "./QuoteMetricItem.module.scss";

interface Props {
	label: string;
	value: ReactNode;
	tone?: "positive" | "negative" | "neutral";
}

export default function QuoteMetricItem({ label, value, tone }: Props) {
	const toneClass =
		tone === "positive"
			? styles.value_positive
			: tone === "negative"
				? styles.value_negative
				: "";

	return (
		<div className={styles.item}>
			<dt className={styles.label}>{label}</dt>
			<dd className={`${styles.value} ${toneClass}`.trim()}>{value}</dd>
		</div>
	);
}
