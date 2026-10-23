import { NavLink } from "react-router-dom";
import SoonBadge from "@/shared/components/Navbar/SoonBadge";
import type { NavTab } from "@/shared/components/Navbar/types";
import styles from "./TabLink.module.scss";

interface Props {
	tab: NavTab;
	active: boolean;
}

/** Mantiene la misma etiqueta para destinos reales y opciones futuras. */
export default function TabLink({ tab, active }: Props) {
	const Icon = tab.icon;
	const className = [
		styles.tabLink,
		active ? styles.tabLink_active : "",
		!tab.path ? styles.tabLink_disabled : "",
	]
		.filter(Boolean)
		.join(" ");

	if (!tab.path) {
		return (
			<span className={className} aria-disabled="true">
				{Icon && <Icon className={styles.tabLink__icon} aria-hidden="true" />}
				<span>{tab.label}</span>
				<SoonBadge tone={tab.badgeTone} />
			</span>
		);
	}

	return (
		<NavLink
			to={tab.path}
			className={className}
			aria-current={active ? "page" : undefined}
		>
			{Icon && <Icon className={styles.tabLink__icon} aria-hidden="true" />}
			<span>{tab.label}</span>
		</NavLink>
	);
}
