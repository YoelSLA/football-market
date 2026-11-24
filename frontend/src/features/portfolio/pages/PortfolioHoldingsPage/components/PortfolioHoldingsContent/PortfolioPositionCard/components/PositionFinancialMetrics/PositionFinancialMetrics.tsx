import { formatCredits } from "@/features/portfolio/utils";
import { ChartPie, Receipt, TrendingUp } from "@/shared/icons";
import styles from "./PositionFinancialMetrics.module.scss";

interface Props {
	averageUnitPrice: number | null;
	currentUnitPrice: number | null;
	currentValue: number | null;
	valuationStatus: string;
}

export default function PositionFinancialMetrics({
	averageUnitPrice,
	currentUnitPrice,
	currentValue,
	valuationStatus,
}: Props) {
	const isUnvalued = valuationStatus === "UNVALUED";
	const currentPrice = isUnvalued
		? "Sin cotización"
		: formatCredits(currentUnitPrice, true);

	const metrics = [
		{
			key: "average",
			label: "Precio promedio",
			value: formatCredits(averageUnitPrice, true),
			icon: Receipt,
			iconClass: styles.icon_average,
			warning: false,
		},
		{
			key: "current",
			label: "Precio actual",
			value: currentPrice,
			icon: TrendingUp,
			iconClass: styles.icon_current,
			warning: isUnvalued,
		},
		{
			key: "value",
			label: "Valor actual",
			value: formatCredits(currentValue, true),
			icon: ChartPie,
			iconClass: styles.icon_value,
			warning: false,
		},
	] as const;

	return (
		<section
			className={styles.financialMetrics}
			aria-label="Métricas financieras"
		>
			{metrics.map(({ key, label, value, icon: Icon, iconClass, warning }) => (
				<div
					key={key}
					className={`${styles.metric} ${warning ? styles.metric_warning : ""}`}
				>
					<Icon className={`${styles.icon} ${iconClass}`} aria-hidden="true" />
					<div className={styles.info}>
						<dt className={styles.value}>{value}</dt>
						<dd className={styles.label}>{label}</dd>
					</div>
				</div>
			))}
		</section>
	);
}
