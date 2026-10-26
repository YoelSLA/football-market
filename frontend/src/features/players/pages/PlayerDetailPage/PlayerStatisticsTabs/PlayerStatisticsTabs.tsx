import { BarChart3, List } from "@/shared/icons";
import styles from "./PlayerStatisticsTabs.module.scss";

interface Props { activeTab: "summary" | "catalog"; onChange: (tab: "summary" | "catalog") => void }
export default function PlayerStatisticsTabs({ activeTab, onChange }: Props) {
	const tabs = [{ key: "summary" as const, label: "Resumen", Icon: BarChart3 }, { key: "catalog" as const, label: "Todas las estadísticas", Icon: List }];
	return <div className={styles.playerStatisticsTabs} role="tablist" aria-label="Estadísticas del jugador">{tabs.map(({ key, label, Icon }, index) => <button key={key} type="button" role="tab" id={`tab-${key}`} aria-selected={activeTab === key} aria-controls={`panel-${key}`} tabIndex={activeTab === key ? 0 : -1} onClick={() => onChange(key)} onKeyDown={(event) => {
		if (!["ArrowRight", "ArrowLeft", "Home", "End"].includes(event.key)) return;
		event.preventDefault();
		const next = event.key === "Home" ? 0 : event.key === "End" ? 1 : 1 - index;
		onChange(tabs[next].key);
		const buttons = event.currentTarget.parentElement?.querySelectorAll<HTMLButtonElement>("button");
		buttons?.[next]?.focus();
	}}><Icon size={18} aria-hidden="true" />{label}</button>)}</div>;
}
