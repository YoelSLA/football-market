import type { LucideIcon } from "@/shared/icons";
import styles from "./MarketPageHeading.module.scss";

interface Props {
	title: string;
	description: string;
	icon: LucideIcon;
}

export default function MarketPageHeading({
	title,
	description,
	icon: Icon,
}: Props) {
	return (
		<div className={styles.marketPageHeading}>
			<span className={styles.marketPageHeading__symbol}>
				<Icon aria-hidden="true" />
			</span>
			<div>
				<h1>{title}</h1>
				<p>{description}</p>
			</div>
		</div>
	);
}
