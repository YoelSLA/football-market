import type { TransactionHistoryEntry as TransactionHistoryEntryModel } from "@/features/portfolio/types";
import { TransactionType } from "@/features/portfolio/types";
import { formatCredits, formatDateTime } from "@/features/portfolio/utils";
import { PlayerTableCell, TokenSymbol } from "@/shared/components";
import styles from "./TransactionHistoryEntry.module.scss";

interface Props {
	transaction: TransactionHistoryEntryModel;
}

const BADGE_CLASSES: Record<TransactionType, string> = {
	[TransactionType.BUY]: styles.badge_buy,
	[TransactionType.SELL]: styles.badge_sell,
};

export default function TransactionHistoryEntry({ transaction }: Props) {
	const isBuy = transaction.type === TransactionType.BUY;
	const typeLabel = isBuy ? "COMPRA" : "VENTA";
	const badgeClass = `${styles.badge} ${BADGE_CLASSES[transaction.type]}`;

	return (
		<tr className={styles.row}>
			<td className={styles.td_id}>
				<span className={styles.ticketBadge}>#{transaction.transactionId}</span>
			</td>
			<td className={styles.td_player}>
				<PlayerTableCell
					player={{
						id: transaction.player.id,
						name: transaction.player.name,
						imageUrl: transaction.player.imageUrl,
						fallbackImageUrl: transaction.player.fallbackImageUrl,
						position: transaction.player.position,
						teamId: transaction.player.team.id,
						teamName: transaction.player.team.name,
						leagueId: transaction.player.league.id,
						leagueName: transaction.player.league.name,
					}}
					href={`/players/${transaction.player.id}`}
				/>
			</td>
			<td className={styles.td_operation}>
				<span className={badgeClass}>
					<span className={styles.badgeDot} aria-hidden="true" />
					{typeLabel}
				</span>
			</td>
			<td className={styles.td_quantity}>
				<span className={styles.quantityValue}>
					{transaction.quantity} <TokenSymbol />
				</span>
			</td>
			<td className={styles.td_unitPrice}>
				<span className={styles.unitPriceValue}>
					{formatCredits(transaction.unitPrice)} / <TokenSymbol size="sm" />
				</span>
			</td>
			<td className={styles.td_total}>
				<strong
					className={`${styles.totalValue} ${
						isBuy ? styles.totalValue_buy : styles.totalValue_sell
					}`}
				>
					{isBuy ? "- " : "+ "}
					{formatCredits(transaction.totalAmount)}
				</strong>
			</td>
			<td className={styles.td_date}>
				<time dateTime={transaction.executedAt} className={styles.timeValue}>
					{formatDateTime(transaction.executedAt)}
				</time>
			</td>
		</tr>
	);
}
