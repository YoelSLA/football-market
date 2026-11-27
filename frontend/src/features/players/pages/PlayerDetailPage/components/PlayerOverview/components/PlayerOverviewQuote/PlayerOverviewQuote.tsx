import type { PlayerQuote } from "@/features/players/types";
import { TokenSymbol } from "@/shared/components";
import { Plus } from "@/shared/icons";
import styles from "./PlayerOverviewQuote.module.scss";

interface Props {
	quote?: PlayerQuote | null;
	loading?: boolean;
	onBuyClick?: () => void;
	onSellClick?: () => void;
	className?: string;
}

export default function PlayerOverviewQuote({
	quote,
	loading,
	onBuyClick,
	onSellClick,
	className,
}: Props) {
	const change = quote?.percentageChange;
	const changeLabel =
		quote?.origin === "INITIAL"
			? "Cotización inicial"
			: quote?.direction === "up"
				? `▲ +${change?.toFixed(2).replace(".", ",")} % (24h)`
				: quote?.direction === "down"
					? `▼ ${change?.toFixed(2).replace(".", ",")} % (24h)`
					: quote
						? "Sin variación"
						: "";

	const sportMultiplier = quote?.breakdown?.sportMultiplier ?? 1.12;
	const marketMultiplier = quote?.breakdown?.marketMultiplier ?? 1.04;

	return (
		<section
			className={`${styles.playerOverviewQuote} ${className ?? ""}`.trim()}
			aria-label="Cotización oficial vigente"
			aria-busy={loading}
		>
			<div className={styles.playerOverviewQuote__data}>
				<span className={styles.playerOverviewQuote__label}>
					Cotización oficial
				</span>
				<div className={styles.playerOverviewQuote__priceRow}>
					<strong className={styles.playerOverviewQuote__price}>
						{loading
							? "Cargando…"
							: quote
								? `${quote.price.toFixed(2).replace(".", ",")} `
								: "Sin cotización"}
						{quote && (
							<span className={styles.playerOverviewQuote__currency}>₡</span>
						)}
					</strong>
					{changeLabel && (
						<span
							className={`${styles.playerOverviewQuote__change} ${
								quote?.direction === "up"
									? styles["playerOverviewQuote__change--up"]
									: quote?.direction === "down"
										? styles["playerOverviewQuote__change--down"]
										: styles["playerOverviewQuote__change--flat"]
							}`}
						>
							{changeLabel}
						</span>
					)}
				</div>
				<div className={styles.playerOverviewQuote__factors}>
					<span title="Multiplicador deportivo">
						⚽ Deporte: <strong>x{sportMultiplier.toFixed(2)}</strong>
					</span>
					<span
						className={styles.playerOverviewQuote__factorSep}
						aria-hidden="true"
					>
						•
					</span>
					<span title="Demanda de mercado">
						📈 Mercado: <strong>x{marketMultiplier.toFixed(2)}</strong>
					</span>
				</div>
			</div>

			<div className={styles.playerOverviewQuote__actions}>
				{onBuyClick && (
					<button
						type="button"
						className={styles.playerOverviewQuote__buyBtn}
						onClick={onBuyClick}
					>
						<Plus size={15} aria-hidden="true" />
						<span>
							Comprar <TokenSymbol size="sm" />
						</span>
					</button>
				)}
				{onSellClick && (
					<button
						type="button"
						className={styles.playerOverviewQuote__sellBtn}
						onClick={onSellClick}
					>
						<span>Vender / Listar</span>
					</button>
				)}
			</div>
		</section>
	);
}
