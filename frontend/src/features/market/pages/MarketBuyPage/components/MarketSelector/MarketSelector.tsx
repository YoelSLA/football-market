import type { MarketTab } from "@/features/market/types";
import styles from "./MarketSelector.module.scss";

interface Props {
	selectedTab: MarketTab;
	onSelectTab: (tab: MarketTab) => void;
}

const TABS: { id: MarketTab; label: string }[] = [
	{ id: "official", label: "Emisión Oficial" },
	{ id: "community", label: "Oferta de Usuarios" },
];

/**
 * Selector de pestañas de mercado (Emisión Oficial vs Oferta de Usuarios).
 * Basado en las especificaciones del diseño de Penpot.
 */
export default function MarketSelector({ selectedTab, onSelectTab }: Props) {
	const handleKeyDown = (
		event: React.KeyboardEvent<HTMLButtonElement>,
		currentIndex: number,
	) => {
		if (!["ArrowRight", "ArrowLeft", "Home", "End"].includes(event.key)) {
			return;
		}

		event.preventDefault();
		const nextIndex =
			event.key === "Home"
				? 0
				: event.key === "End"
					? TABS.length - 1
					: event.key === "ArrowRight"
						? (currentIndex + 1) % TABS.length
						: (currentIndex - 1 + TABS.length) % TABS.length;

		const nextTab = TABS[nextIndex];
		onSelectTab(nextTab.id);

		const buttons =
			event.currentTarget.parentElement?.querySelectorAll<HTMLButtonElement>(
				"button",
			);
		buttons?.[nextIndex]?.focus();
	};

	return (
		<nav className={styles.selector} aria-label="Segmento de mercado">
			<div
				className={styles.selector__group}
				role="tablist"
				aria-label="Tipo de mercado"
			>
				{TABS.map((tab, index) => {
					const isActive = selectedTab === tab.id;
					return (
						<button
							key={tab.id}
							type="button"
							role="tab"
							id={`tab-${tab.id}`}
							aria-selected={isActive}
							aria-controls={`panel-${tab.id}`}
							tabIndex={isActive ? 0 : -1}
							className={`${styles.selector__tab} ${
								isActive ? styles.selector__tab_active : ""
							}`}
							onClick={() => onSelectTab(tab.id)}
							onKeyDown={(e) => handleKeyDown(e, index)}
						>
							{tab.label}
						</button>
					);
				})}
			</div>
		</nav>
	);
}
