import { NavLink } from "react-router-dom";
import { SoonBadge } from "../SoonBadge";
import type { NavTab } from "../types";
import styles from "./TabLink.module.scss";

interface Props {
	tab: NavTab;
	active: boolean;
}

/** Mantiene la misma etiqueta para destinos reales y opciones futuras. */
export function TabLink({ tab, active }: Props) {
	const className = [
		styles["tab-link"],
		active ? styles["tab-link--active"] : "",
		!tab.path ? styles["tab-link--disabled"] : "",
	]
		.filter(Boolean)
		.join(" ");

	if (!tab.path) {
		return (
			<span className={className} aria-disabled="true">
				<span>{tab.label}</span>
				<SoonBadge />
			</span>
		);
	}

	return (
		<NavLink
			to={tab.path}
			className={className}
			aria-current={active ? "page" : undefined}
		>
			<span>{tab.label}</span>
		</NavLink>
	);
}
