import { Link, useNavigate } from "react-router-dom";
import type { PlayerQuote } from "@/features/players/types";
import { TokenSymbol } from "@/shared/components";
import { CREDIT_SYMBOL } from "@/shared/constants";
import styles from "./PlayerP2PTeaserCard.module.scss";

interface Props {
	playerName: string;
	quote?: PlayerQuote | null;
}

export default function PlayerP2PTeaserCard({ playerName, quote }: Props) {
	const navigate = useNavigate();
	const basePrice = quote?.price ?? 82;
	const bestAsk = (basePrice * 1.018).toFixed(2).replace(".", ",");
	const avgPrice = (basePrice * 1.011).toFixed(2).replace(".", ",");
	const p2pUrl = `/market/p2p?search=${encodeURIComponent(playerName)}`;

	return (
		<section
			className={styles.p2pCard}
			aria-label={`Mercado P2P de ${playerName}`}
		>
			<header className={styles.p2pCard__header}>
				<h3 className={styles.p2pCard__title}>Mercado P2P de {playerName}</h3>
				<Link to={p2pUrl} className={styles.p2pCard__link}>
					Ver 8 órdenes →
				</Link>
			</header>

			<div className={styles.p2pCard__rows}>
				<div className={styles.p2pCard__row}>
					<span className={styles.p2pCard__askLabel}>Mejor Oferta Venta</span>
					<strong className={styles.p2pCard__value}>
						{bestAsk} {CREDIT_SYMBOL}{" "}
						<span className={styles.p2pCard__qty}>
							(5 <TokenSymbol size="xs" />)
						</span>
					</strong>
				</div>
				<div className={styles.p2pCard__row}>
					<span className={styles.p2pCard__avgLabel}>Precio Promedio P2P</span>
					<strong className={styles.p2pCard__value}>
						{avgPrice} {CREDIT_SYMBOL}
					</strong>
				</div>
			</div>

			<button
				type="button"
				className={styles.p2pCard__btn}
				onClick={() => navigate(p2pUrl)}
			>
				Comprar directamente en P2P
			</button>
		</section>
	);
}
