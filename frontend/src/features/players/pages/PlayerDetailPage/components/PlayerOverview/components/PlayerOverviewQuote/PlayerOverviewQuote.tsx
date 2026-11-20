import type { PlayerQuote } from "@/features/players/types";
import styles from "./PlayerOverviewQuote.module.scss";

interface Props {
	quote?: PlayerQuote | null;
	loading?: boolean;
	className?: string;
}

export default function PlayerOverviewQuote({
	quote,
	loading,
	className,
}: Props) {
	const change = quote?.percentageChange;
	const changeLabel =
		quote?.origin === "INITIAL"
			? "Cotización inicial"
			: quote?.direction === "up"
				? `▲ +${change?.toFixed(2).replace(".", ",")} %`
				: quote?.direction === "down"
					? `▼ ${change?.toFixed(2).replace(".", ",")} %`
					: quote
						? "Sin variación"
						: "";

	return (
		<section
			className={`${styles.playerOverviewQuote} ${className ?? ""}`.trim()}
			aria-label="Cotización oficial vigente"
			aria-busy={loading}
		>
			<span className={styles.playerOverviewQuote__label}>
				Cotización oficial
			</span>
			<strong className={styles.playerOverviewQuote__price}>
				{loading
					? "Cargando…"
					: quote
						? `${quote.price.toFixed(2).replace(".", ",")} cr.`
						: "Sin cotización"}
			</strong>
			{changeLabel && (
				<span className={styles.playerOverviewQuote__change}>
					{changeLabel}
				</span>
			)}
		</section>
	);
}
