import type { TransactionHistoryEntry as TransactionHistoryEntryModel } from "@/features/portfolio/types";
import { TransactionType } from "@/features/portfolio/types";
import {
	formatCount,
	formatCredits,
	formatDateTime,
} from "@/features/portfolio/utils";
import {
	CompetitionLogo,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
import styles from "./TransactionHistoryEntry.module.scss";

interface Props {
	transaction: TransactionHistoryEntryModel;
}

const BADGE_CLASSES: Record<TransactionType, string> = {
	[TransactionType.BUY]: styles.transactionHistoryEntry__badge_buy,
	[TransactionType.SELL]: styles.transactionHistoryEntry__badge_sell,
};

export default function TransactionHistoryEntry({ transaction }: Props) {
	const typeLabel =
		transaction.type === TransactionType.BUY ? "COMPRA" : "VENTA";
	const quantityLabel = formatCount(transaction.quantity, "token", "tokens");
	const badgeClass = `${styles.transactionHistoryEntry__badge} ${BADGE_CLASSES[transaction.type]}`;

	return (
		<li className={styles.transactionHistoryEntry}>
			<div className={styles.transactionHistoryEntry__player}>
				<PlayerPortrait
					src={transaction.player.imageUrl}
					fallbackSrc={transaction.player.fallbackImageUrl}
					name={transaction.player.name}
					size="sm"
				/>
				<div className={styles.transactionHistoryEntry__identity}>
					<strong className={styles.transactionHistoryEntry__playerName}>
						{transaction.player.name}
					</strong>
					<div className={styles.transactionHistoryEntry__affiliations}>
						<span className={styles.transactionHistoryEntry__affiliation}>
							<TeamBadge
								teamId={transaction.player.team.id}
								teamName={transaction.player.team.name}
								leagueId={transaction.player.league.id}
								leagueName={transaction.player.league.name}
								className={styles.transactionHistoryEntry__teamBadge}
							/>
							<span>{transaction.player.team.name}</span>
						</span>
						<span className={styles.transactionHistoryEntry__affiliation}>
							<CompetitionLogo
								competitionId={transaction.player.league.id}
								competitionName={transaction.player.league.name}
								className={styles.transactionHistoryEntry__leagueLogo}
								decorative
							/>
							<span>{transaction.player.league.name}</span>
						</span>
					</div>
					<span className={styles.transactionHistoryEntry__id}>
						Transacción #{transaction.transactionId}
					</span>
				</div>
			</div>
			<div className={styles.transactionHistoryEntry__operation}>
				<span className={badgeClass}>{typeLabel}</span>
			</div>
			<div className={styles.transactionHistoryEntry__quantity}>
				<span className={styles.transactionHistoryEntry__label}>Cantidad</span>
				<span className={styles.transactionHistoryEntry__value}>
					{quantityLabel}
				</span>
			</div>
			<div className={styles.transactionHistoryEntry__unitPrice}>
				<span className={styles.transactionHistoryEntry__label}>
					Precio unitario
				</span>
				<span className={styles.transactionHistoryEntry__value}>
					{formatCredits(transaction.unitPrice)}
				</span>
			</div>
			<div className={styles.transactionHistoryEntry__total}>
				<span className={styles.transactionHistoryEntry__label}>Total</span>
				<strong className={styles.transactionHistoryEntry__totalValue}>
					{formatCredits(transaction.totalAmount)}
				</strong>
			</div>
			<div className={styles.transactionHistoryEntry__date}>
				<span className={styles.transactionHistoryEntry__label}>Fecha</span>
				<time
					dateTime={transaction.executedAt}
					className={styles.transactionHistoryEntry__time}
				>
					{formatDateTime(transaction.executedAt)}
				</time>
			</div>
		</li>
	);
}
