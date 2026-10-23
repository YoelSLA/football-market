import {
	House,
	type LucideIcon,
	Store,
	Trophy,
	UsersRound,
	Wallet,
} from "@/shared/icons";
import type { NavIconKey } from "../types";
import styles from "./NavIcon.module.scss";

interface Props {
	name: NavIconKey;
	className?: string;
}

const NAV_ICONS: Record<NavIconKey, LucideIcon> = {
	home: House,
	players: UsersRound,
	market: Store,
	portfolio: Wallet,
	brand: Trophy,
};

/** Centraliza los iconos decorativos, que heredan el color de su etiqueta. */
export default function NavIcon({ name, className }: Props) {
	const Icon = NAV_ICONS[name];
	return (
		<Icon
			className={className ? `${styles.icon} ${className}` : styles.icon}
			strokeWidth={1.6}
			aria-hidden="true"
			focusable="false"
		/>
	);
}
