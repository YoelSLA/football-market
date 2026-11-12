import { PLAYER_DETAIL_TABS } from "@/features/players/constants";
import type { StatisticsTab } from "@/features/players/types";
import styles from "./PlayerStatisticsTabs.module.scss";

export type { StatisticsTab };

interface Props {
	activeTab: StatisticsTab;
	onChange: (tab: StatisticsTab) => void;
}

export default function PlayerStatisticsTabs({ activeTab, onChange }: Props) {
	return (
		<div
			className={styles.playerStatisticsTabs}
			role="tablist"
			aria-label="Información y estadísticas del jugador"
		>
			{PLAYER_DETAIL_TABS.map(({ key, label, Icon }, index) => (
				<button
					key={key}
					type="button"
					role="tab"
					id={`tab-${key}`}
					aria-selected={activeTab === key}
					aria-controls={`panel-${key}`}
					tabIndex={activeTab === key ? 0 : -1}
					onClick={() => onChange(key)}
					onKeyDown={(event) => {
						if (!["ArrowRight", "ArrowLeft", "Home", "End"].includes(event.key))
							return;
						event.preventDefault();
						let next = index;
						if (event.key === "Home") {
							next = 0;
						} else if (event.key === "End") {
							next = PLAYER_DETAIL_TABS.length - 1;
						} else if (event.key === "ArrowRight") {
							next = (index + 1) % PLAYER_DETAIL_TABS.length;
						} else if (event.key === "ArrowLeft") {
							next =
								(index - 1 + PLAYER_DETAIL_TABS.length) %
								PLAYER_DETAIL_TABS.length;
						}

						onChange(PLAYER_DETAIL_TABS[next].key);
						const buttons =
							event.currentTarget.parentElement?.querySelectorAll<HTMLButtonElement>(
								"button",
							);
						buttons?.[next]?.focus();
					}}
				>
					<Icon size={18} aria-hidden="true" />
					<span>{label}</span>
				</button>
			))}
		</div>
	);
}
