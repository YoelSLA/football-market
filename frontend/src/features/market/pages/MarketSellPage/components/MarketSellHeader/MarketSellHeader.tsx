import { PageHeading } from "@/shared/components";
import { Tag } from "@/shared/icons";
import type { PlayerHolding } from "../../MarketSellPage";
import styles from "./MarketSellHeader.module.scss";

interface Props {
	players: PlayerHolding[];
}

export default function MarketSellHeader({ players }: Props) {
	const totals = players.reduce(
		(accumulator, player) => ({
			total: accumulator.total + player.totalQuantity,
			available: accumulator.available + player.availableQuantity,
			reserved: accumulator.reserved + player.reservedQuantity,
		}),
		{ total: 0, available: 0, reserved: 0 },
	);

	return (
		<header className={styles.sellHeader}>
			<PageHeading
				title="Vender tokens"
				description="Publicá tus tokens en el mercado secundario."
				icon={Tag}
			/>
			<div className={styles.sellHeader__totals}>
				<p>
					Total<strong>{totals.total}</strong>
				</p>
				<p>
					Disponibles<strong>{totals.available}</strong>
				</p>
				<p>
					Reservados<strong>{totals.reserved}</strong>
				</p>
			</div>
		</header>
	);
}
