import styles from "./SoonBadge.module.scss";

interface Props {
	tone?: "gold" | "violet";
}

/** Comunica el estado futuro sin depender exclusivamente del color. */
export default function SoonBadge({ tone }: Props) {
	const toneClass = tone
		? { gold: styles.soonBadge_gold, violet: styles.soonBadge_violet }[tone]
		: "";
	return (
		<span className={`${styles.soonBadge} ${toneClass}`.trim()}>
			Próximamente
		</span>
	);
}
