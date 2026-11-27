import styles from "./MarketPlayerCardSkeleton.module.scss";

interface Props {
	variant?: "official" | "community";
}

export default function MarketPlayerCardSkeleton({
	variant = "official",
}: Props) {
	return (
		<article
			className={styles.skeletonCard}
			data-variant={variant}
			aria-hidden="true"
		>
			<div className={styles.skeletonCard__identity}>
				<span className={styles.skeletonCard__portrait} />
				<div className={styles.skeletonCard__identityDetails}>
					<span className={styles.skeletonCard__name} />
					<span className={styles.skeletonCard__club} />
					<span className={styles.skeletonCard__nationality} />
				</div>
			</div>

			<div className={styles.skeletonCard__financial}>
				<div className={styles.skeletonCard__financialRow}>
					<span className={styles.skeletonCard__label} />
					<span className={styles.skeletonCard__label} />
					<span className={styles.skeletonCard__label} />
				</div>
				<div className={styles.skeletonCard__financialRow}>
					<span className={styles.skeletonCard__primaryValue} />
					<span className={styles.skeletonCard__primaryValue} />
					<span className={styles.skeletonCard__primaryValue} />
				</div>
				<div className={styles.skeletonCard__financialRow}>
					<span className={styles.skeletonCard__secondaryValue} />
					<span className={styles.skeletonCard__secondaryValue} />
				</div>
			</div>

			<div className={styles.skeletonCard__action}>
				<span className={styles.skeletonCard__button} />
			</div>
		</article>
	);
}
