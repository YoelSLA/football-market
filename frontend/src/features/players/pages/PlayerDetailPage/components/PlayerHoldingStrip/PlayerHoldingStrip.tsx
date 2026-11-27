import { Link } from "react-router-dom";
import type { PortfolioPosition } from "@/features/portfolio/types";
import { formatCredits } from "@/features/portfolio/utils";
import { TokenSymbol } from "@/shared/components";
import { CREDIT_SYMBOL } from "@/shared/constants";
import styles from "./PlayerHoldingStrip.module.scss";

interface Props {
	holding: PortfolioPosition | null;
	availableCredits?: number;
	playerName: string;
	currentPrice?: number | null;
}

/**
 * Franja informativa que exhibe la tenencia del usuario para este jugador
 * y su saldo de créditos disponible para operar.
 */
export default function PlayerHoldingStrip({
	holding,
	availableCredits,
	playerName,
	currentPrice,
}: Props) {
	const hasTokens = Boolean(holding && holding.quantity > 0);
	const quantity = holding?.quantity ?? 0;
	const currentValue =
		hasTokens && currentPrice ? (quantity * currentPrice).toFixed(2) : null;

	return (
		<section
			className={styles.holdingStrip}
			aria-label="Posición de cartera del usuario"
		>
			<div className={styles.holdingStrip__position}>
				<span className={styles.holdingStrip__badge}>💼 Tu Cartera</span>
				{hasTokens ? (
					<span className={styles.holdingStrip__details}>
						Posees{" "}
						<strong>
							{quantity} <TokenSymbol size="sm" />
						</strong>
						{currentValue && (
							<>
								{" "}
								(valor estimado:{" "}
								<strong>
									{currentValue} {CREDIT_SYMBOL}
								</strong>
								)
							</>
						)}
						{holding?.acquisitionCost != null && (
							<span className={styles.holdingStrip__cost}>
								{" "}
								· Costo medio: {formatCredits(holding.acquisitionCost)}
							</span>
						)}
					</span>
				) : (
					<span className={styles.holdingStrip__empty}>
						No posees <TokenSymbol size="xs" /> de este jugador actualmente.
					</span>
				)}
			</div>

			<div className={styles.holdingStrip__actions}>
				<span className={styles.holdingStrip__credits}>
					Saldo disp.:{" "}
					<strong>
						{availableCredits != null
							? `${availableCredits.toLocaleString("es-AR")} ${CREDIT_SYMBOL}`
							: "—"}
					</strong>
				</span>
				<Link
					to={`/market/p2p?search=${encodeURIComponent(playerName)}`}
					className={styles.holdingStrip__p2pLink}
				>
					Ver órdenes P2P →
				</Link>
			</div>
		</section>
	);
}
